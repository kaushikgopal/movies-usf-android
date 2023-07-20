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

/** This is the primary class that builds out the structure of the ViewModel class */
object UsfViewModelFileBuilder {

  private const val androidxPackage = "androidx.lifecycle"
  private val saveStateHandleTypeClassName = ClassName(androidxPackage, "SavedStateHandle")
  private val androidViewModelTypeClassName = ClassName(androidxPackage, "AndroidViewModel")
  private val vmFactoryTypeClassName =
      ClassName(androidxPackage, "AbstractSavedStateViewModelFactory")

  private const val kotlinxPackage = "kotlinx.coroutines"
  private val coroutineScopeClassName = ClassName(kotlinxPackage, "CoroutineScope")

  private const val viewModelScopeReservedWord = "viewModelScope"
  private const val handleReservedWord = "handle"

  /** File level structure */
  fun buildFileSpec(
      viewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition,
      packageName: String,
  ): FileSpec {

    val vmInterfaceTypeName = "UsfVm"
    val vmInterfacePackage = "co.kaush.msusf.usf"

    val vmInterfaceTypeInterfaceName = ClassName(vmInterfacePackage, vmInterfaceTypeName)

    return FileSpec.builder(
            packageName,
            "${viewModelClassBuilderDefinition.simplifiedClassName()}1",
        )
        .addType(
            buildAndroidViewModelClassSpec(
                vmInterfaceTypeInterfaceName,
                packageName,
                viewModelClassBuilderDefinition,
            ),
        )
        .addImport(androidxPackage, "viewModelScope")
        .addImport(vmInterfacePackage, vmInterfaceTypeName)
        .build()
  }

  /** Class level structure */
  private fun buildAndroidViewModelClassSpec(
      vmInterfaceTypeInterfaceName: ClassName,
      packageName: String,
      viewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition
  ): TypeSpec {

    val classBuilder =
        TypeSpec.classBuilder("${viewModelClassBuilderDefinition.simplifiedClassName()}1")
            .addModifiers(KModifier.PUBLIC)
            .superclass(androidViewModelTypeClassName)
            .addSuperinterface(vmInterfaceTypeInterfaceName)

    val constructorBuilder = FunSpec.constructorBuilder()

    addParamsToClassConstructor(
        viewModelClassBuilderDefinition,
        constructorBuilder,
        classBuilder,
        false,
    )

    val paramsForConstructor =
        buildParamForwardingConstructorBody(
            viewModelClassBuilderDefinition.parameters,
            setOf(),
            setOf(coroutineScopeClassName),
        ) { sb, param ->
          when (param.paramType) {
            coroutineScopeClassName -> {
              sb.append("\t${param.paramName} = $viewModelScopeReservedWord,\n")
            }
          }
        }

    classBuilder.addProperty(
        PropertySpec.builder(
                viewModelClassBuilderDefinition.simplifiedClassName(),
                viewModelClassBuilderDefinition.viewModel.paramType,
                KModifier.PRIVATE,
            )
            .initializer(
                "${viewModelClassBuilderDefinition.viewModel.paramType.simpleName}$paramsForConstructor",
            )
            .build(),
    )
    classBuilder.primaryConstructor(constructorBuilder.build())

    viewModelClassBuilderDefinition.functions.forEach { functionMetaData ->
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
          "${viewModelClassBuilderDefinition.simplifiedClassName()}.${functionMetaData.name}${
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

    viewModelClassBuilderDefinition.properties.forEach { propertyMetaData ->
      val property =
          PropertySpec.builder(propertyMetaData.paramName, propertyMetaData.paramType)
              .getter(
                  FunSpec.getterBuilder()
                      .addStatement(
                          "return ${viewModelClassBuilderDefinition.simplifiedClassName()}.${propertyMetaData.paramName}",
                      )
                      .build(),
              )

      classBuilder.addProperty(property.build())
    }

    classBuilder.addType(buildFactory(viewModelClassBuilderDefinition))

    return classBuilder.build()
  }

  private fun buildFactory(
      ViewModelClassBuilderDefinition: UsfViewModelClassBuilderDefinition
  ): TypeSpec {
    val classBuilder =
        TypeSpec.classBuilder(
            "${ViewModelClassBuilderDefinition.simplifiedClassName()}Factory",
        )
    val constructorBuilder = FunSpec.constructorBuilder()

    classBuilder.superclass(vmFactoryTypeClassName)

    val paramsForConstructor =
        buildParamForwardingConstructorBody(
            ViewModelClassBuilderDefinition.parameters,
            setOf(coroutineScopeClassName),
            setOf(saveStateHandleTypeClassName),
        ) { sb, param ->
          when (param.paramType) {
            saveStateHandleTypeClassName -> {
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

    val genericViewModel =
        com.squareup.kotlinpoet.TypeVariableName("T", androidViewModelTypeClassName)
    val factoryCreateFunctionSpec =
        FunSpec.builder("create")
            .addTypeVariable(genericViewModel)
            .addParameter("key", String::class)
            .addParameter(
                "modelClass",
                Class::class.asClassName().parameterizedBy(genericViewModel),
            )
            .addParameter(handleReservedWord, saveStateHandleTypeClassName)
            .addCode(
                "return ${ViewModelClassBuilderDefinition.viewModel.paramName}$paramsForConstructor as T",
            )
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
