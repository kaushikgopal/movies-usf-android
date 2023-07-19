package co.kaush.msusf.processors

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

object UsfViewModelFileBuilder {

  private const val androidxPackage = "androidx.lifecycle"
  private val saveStateHandleClassName = ClassName(androidxPackage, "SavedStateHandle")
  private val viewModelClassName = ClassName(androidxPackage, "ViewModel")
  private val viewModelProviderClassName =
      ClassName(androidxPackage, "AbstractSavedStateViewModelFactory")

  private const val kotlinxPackage = "kotlinx.coroutines"
  private val coroutineScopeClassName = ClassName(kotlinxPackage, "CoroutineScope")

  private const val viewModelScopeReservedWord = "viewModelScope"
  private const val handleReservedWord = "handle"

  fun buildFileSpecForAndroidViewModel(
      ViewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition
  ): FileSpec {
    return FileSpec.builder("", "${ViewModelClassBuilderDefinition.viewModel.paramName}PostFix")
        .addType(buildAndroidViewModel(ViewModelClassBuilderDefinition))
        .addImport(androidxPackage, "viewModelScope")
        .build()
  }

  private fun buildAndroidViewModel(
      ViewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition
  ): TypeSpec {

    val classBuilder =
        TypeSpec.classBuilder("${ViewModelClassBuilderDefinition.viewModel.paramName}PostFix")
    val constructorBuilder = FunSpec.constructorBuilder()

    classBuilder.superclass(viewModelClassName)

    val paramsForConstructor =
        buildParamForwardingConstructorBody(
            ViewModelClassBuilderDefinition.parameters,
            setOf(),
            setOf(coroutineScopeClassName),
        ) { sb, param ->
          when (param.paramType) {
            coroutineScopeClassName -> {
              sb.append("\t${param.paramName} = $viewModelScopeReservedWord,\n")
            }
          }
        }

    addParamsToClassConstructor(
        ViewModelClassBuilderDefinition,
        constructorBuilder,
        classBuilder,
        false,
    )

    classBuilder.addProperty(
        PropertySpec.builder(
                ViewModelClassBuilderDefinition.viewModel.paramName,
                ViewModelClassBuilderDefinition.viewModel.paramType,
                KModifier.PRIVATE,
            )
            .initializer(
                "${ViewModelClassBuilderDefinition.viewModel.paramType.simpleName}$paramsForConstructor")
            .build(),
    )
    classBuilder.primaryConstructor(constructorBuilder.build())

    ViewModelClassBuilderDefinition.functions.forEach { functionMetaData ->
      val functionBuilder = FunSpec.builder(functionMetaData.name).addModifiers(KModifier.PUBLIC)

      val params =
          functionMetaData.parameters.map {
            ParameterSpec(
                it.paramName,
                it.paramType,
            )
          }
      functionBuilder.addParameters(params)

      val funcCallString =
          "${ViewModelClassBuilderDefinition.viewModel.paramName}.${functionMetaData.name}${
            buildParamForwardingConstructorBody(functionMetaData.parameters)
          }"

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

    ViewModelClassBuilderDefinition.properties.forEach { propertyMetaData ->
      val property =
          PropertySpec.builder(propertyMetaData.paramName, propertyMetaData.paramType)
              .getter(
                  FunSpec.getterBuilder()
                      .addStatement(
                          "return ${ViewModelClassBuilderDefinition.viewModel.paramName}.${propertyMetaData.paramName}")
                      .build(),
              )

      classBuilder.addProperty(property.build())
    }

    classBuilder.addType(buildFactory(ViewModelClassBuilderDefinition))

    return classBuilder.build()
  }

  private fun buildFactory(
      ViewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition
  ): TypeSpec {
    val classBuilder =
        TypeSpec.classBuilder(
            "${ViewModelClassBuilderDefinition.viewModel.paramName}PostFixFactory")
    val constructorBuilder = FunSpec.constructorBuilder()

    classBuilder.superclass(viewModelProviderClassName)

    val paramsForConstructor =
        buildParamForwardingConstructorBody(
            ViewModelClassBuilderDefinition.parameters,
            setOf(coroutineScopeClassName),
            setOf(saveStateHandleClassName),
        ) { sb, param ->
          when (param.paramType) {
            saveStateHandleClassName -> {
              sb.append("\t${param.paramName} = $handleReservedWord,\n")
            }
          }
        }

    addParamsToClassConstructor(
        ViewModelClassBuilderDefinition,
        constructorBuilder,
        classBuilder,
        true,
    )

    val genericViewModel = com.squareup.kotlinpoet.TypeVariableName("T", viewModelClassName)
    val factoryCreateFunctionSpec =
        FunSpec.builder("create")
            .addTypeVariable(genericViewModel)
            .addParameter("key", String::class)
            .addParameter(
                "modelClass", Class::class.asClassName().parameterizedBy(genericViewModel))
            .addParameter(handleReservedWord, saveStateHandleClassName)
            .addCode(
                "return ${ViewModelClassBuilderDefinition.viewModel.paramName}PostFix$paramsForConstructor as T")
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
                    paramMetaData.paramName,
                    paramMetaData.paramType,
                    KModifier.PRIVATE,
                )
                .initializer(paramMetaData.paramName)
                .build(),
        )
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
