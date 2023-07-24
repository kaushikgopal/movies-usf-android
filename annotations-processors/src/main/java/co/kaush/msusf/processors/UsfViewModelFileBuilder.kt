package co.kaush.msusf.processors

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

/** This is the primary class that builds out the structure of the ViewModel class */
object UsfViewModelFileBuilder {

  private const val androidxPackage = "androidx.lifecycle"
  private val androidViewModelTypeClassName = ClassName(androidxPackage, "ViewModel")

  private val viewModelProviderTypeClassName =
      ClassName(androidxPackage, "ViewModelProvider.Factory")

  private const val kotlinxPackage = "kotlinx.coroutines"
  private val coroutineScopeClassName = ClassName(kotlinxPackage, "CoroutineScope")

  private const val viewModelScopeReservedWord = "viewModelScope"

  /** File level structure */
  fun buildFileSpec(
      viewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition,
      packageName: String,
  ): FileSpec {
    return FileSpec.builder(packageName, viewModelClassBuilderDefinition.simplifiedClassName)
        .addType(buildAndroidViewModelClassSpec(viewModelClassBuilderDefinition))
        .addImport(androidxPackage, "viewModelScope", "ViewModelProvider")
        .build()
  }

  /** Class level structure */
  private fun buildAndroidViewModelClassSpec(
      viewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition,
  ): TypeSpec {

    val classBuilder =
        TypeSpec.classBuilder(viewModelClassBuilderDefinition.simplifiedClassName)
            .addModifiers(KModifier.PUBLIC)
            .superclass(androidViewModelTypeClassName)

    val constructorBuilder = FunSpec.constructorBuilder()

    val paramsForConstructor =
        buildParamForwardingConstructorBody(
            viewModelClassBuilderDefinition.parameters, setOf(), setOf(coroutineScopeClassName)) {
                sb,
                param ->
              when (param.paramType) {
                coroutineScopeClassName -> {
                  sb.append("\t${param.paramName} = $viewModelScopeReservedWord,\n")
                }
                else -> {
                  // no-op
                }
              }
            }

    addParamsToClassConstructor(
        viewModelClassBuilderDefinition, constructorBuilder, classBuilder, false)

    classBuilder.addProperty(
        PropertySpec.builder(
                viewModelClassBuilderDefinition.viewModel.paramName,
                viewModelClassBuilderDefinition.viewModel.paramType,
                KModifier.PRIVATE)
            .initializer(
                "${(viewModelClassBuilderDefinition.viewModel.paramType as? ClassName)?.simpleName}$paramsForConstructor")
            .build())
    classBuilder.primaryConstructor(constructorBuilder.build())

    viewModelClassBuilderDefinition.functions.forEach { functionMetaData ->
      val functionBuilder = FunSpec.builder(functionMetaData.name).addModifiers(KModifier.PUBLIC)

      val params = functionMetaData.parameters.map { ParameterSpec(it.paramName, it.paramType) }
      functionBuilder.addParameters(params)

      val funcCallString =
          "${viewModelClassBuilderDefinition.viewModel.paramName}.${functionMetaData.name}${buildParamForwardingConstructorBody(functionMetaData.parameters)}"

      if (functionMetaData.returnType != null) {
        functionBuilder.returns(functionMetaData.returnType)
      }

      val codeBody =
          if (functionMetaData.returnType != null) {
            "return $funcCallString"
          } else {
            funcCallString
          }

      functionBuilder.addCode(codeBody)
      classBuilder.addFunction(functionBuilder.build())
    }

    viewModelClassBuilderDefinition.properties.forEach { propertyMetaData ->
      val property =
          PropertySpec.builder(propertyMetaData.paramName, propertyMetaData.paramType)
              .getter(
                  FunSpec.getterBuilder()
                      .addStatement(
                          "return ${viewModelClassBuilderDefinition.viewModel.paramName}.${propertyMetaData.paramName}")
                      .build())

      classBuilder.addProperty(property.build())
    }

    classBuilder.addType(buildFactory(viewModelClassBuilderDefinition))

    return classBuilder.build()
  }

  private fun buildFactory(
      ViewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition
  ): TypeSpec {
    val classBuilder =
        TypeSpec.classBuilder("${ViewModelClassBuilderDefinition.simplifiedClassName}Factory")
            .addSuperinterface(viewModelProviderTypeClassName)

    val constructorBuilder = FunSpec.constructorBuilder()

    val paramsForConstructor =
        buildParamForwardingConstructorBody(
            ViewModelClassBuilderDefinition.parameters, setOf(coroutineScopeClassName), emptySet())

    addParamsToClassConstructor(
        ViewModelClassBuilderDefinition, constructorBuilder, classBuilder, true)

    val genericViewModel =
        com.squareup.kotlinpoet.TypeVariableName("T", androidViewModelTypeClassName)

    val suppressAnnotation =
        AnnotationSpec.Companion.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build()

    val factoryCreateFunctionSpec =
        FunSpec.builder("create")
            .addTypeVariable(genericViewModel)
            .addAnnotation(suppressAnnotation)
            .addParameter(
                "modelClass", Class::class.asClassName().parameterizedBy(genericViewModel))
            .addCode(
                "return ${ViewModelClassBuilderDefinition.simplifiedClassName}$paramsForConstructor as T")
            .returns(genericViewModel)
            .addModifiers(KModifier.OVERRIDE)

    classBuilder.addFunction(factoryCreateFunctionSpec.build())
    classBuilder.primaryConstructor(constructorBuilder.build())

    return classBuilder.build()
  }

  private fun addParamsToClassConstructor(
      ViewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition,
      constructorBuilder: FunSpec.Builder,
      classBuilder: TypeSpec.Builder,
      addAsProperty: Boolean
  ) {
    for (paramMetaData in ViewModelClassBuilderDefinition.parameters) {

      // TODO: better way to do this
      if (paramMetaData.paramType == coroutineScopeClassName) {
        continue
      }

      constructorBuilder.addParameter(paramMetaData.paramName, paramMetaData.paramType)

      if (addAsProperty) {
        classBuilder.addProperty(
            PropertySpec.builder(
                    paramMetaData.paramName, paramMetaData.paramType, KModifier.PRIVATE)
                .initializer(paramMetaData.paramName)
                .build())
      }
    }
  }

  private fun buildParamForwardingConstructorBody(
      params: List<ParametersDefinition>,
      ignoreList: Set<ClassName> = emptySet(),
      overrides: Set<ClassName> = emptySet(),
      overrideAction: ((StringBuilder, ParametersDefinition) -> Unit)? = null
  ): String {

    return buildString {
      append("(\n")
      for (param in params) {
        if (ignoreList.contains(param.paramType)) {
          continue
        }

        if (overrides.contains(param.paramType)) {
          overrideAction?.let { it(this, param) }
          continue
        }

        append("\t${param.paramName} = ${param.paramName},\n")
      }

      if (params.isNotEmpty()) {
        delete(length - 2, length)
      }
      append("\n)")
    }
  }
}
