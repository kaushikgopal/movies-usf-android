package co.kaush.msusf.processors

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class UsfViewModelVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : KSVisitorVoid() {

  private val reservedFunctionsNames =
      setOf("equals", "hashCode", "toString", "getApplication", "<init>", "addCloseable")

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.CLASS) {
      logger.error("Only a class can be annotated with this annotation", classDeclaration)
      throw IllegalArgumentException()
    }

    if (classDeclaration.containingFile == null) {
      logger.error(
          "Attempted to add annotation to file that does not exist",
          classDeclaration.containingFile)
      throw IllegalArgumentException()
    }

    val packageName = classDeclaration.toClassName().packageName
    val definition = generateGenericClassBuilderDefinition(classDeclaration)
    val fileSpec = UsfViewModelFileBuilder.buildFileSpec(definition, packageName)

    codeGenerator
        .createNewFile(
            dependencies = Dependencies(false, classDeclaration.containingFile!!),
            packageName = classDeclaration.toClassName().packageName,
            fileName = fileSpec.name)
        .use { it.write(fileSpec.toString().toByteArray()) }
  }

  private fun generateGenericClassBuilderDefinition(
      classDeclaration: KSClassDeclaration
  ): UsfViewModelClassBuilderDefinition {
    val constructorParams = mutableListOf<ParametersDefinition>()

    classDeclaration.primaryConstructor?.parameters?.forEach { ksValueParameter ->
      val paramType = ksValueParameter.type.resolve().toTypeName()
      val name = ksValueParameter.name?.getShortName()

      if (name != null) {
        constructorParams.add(ParametersDefinition(name, paramType))
      }
    }

    val functions = mutableListOf<FunctionsDefinition>()
    classDeclaration.getAllFunctions().forEach { ksFunctionDeclaration ->
      val functionName = ksFunctionDeclaration.simpleName.getShortName()

      if (ksFunctionDeclaration.isPublic() && !reservedFunctionsNames.contains(functionName)) {
        val funParams = mutableListOf<ParametersDefinition>()
        ksFunctionDeclaration.parameters.map {
          val varName = it.name?.getShortName()
          if (varName != null) {
            funParams.add(ParametersDefinition(varName, it.type.resolve().toTypeName()))
          }
        }

        val returnType =
            try {
              ksFunctionDeclaration.returnType?.resolve()?.toTypeName()
            } catch (ex: Exception) {
              throw ex
            }

        functions.add(FunctionsDefinition(functionName, funParams, returnType))
      }
    }

    val properties = mutableListOf<ParametersDefinition>()
    classDeclaration.getAllProperties().forEach { ksPropertyDecleration ->
      if (ksPropertyDecleration.isPublic()) {

        val returnType = ksPropertyDecleration.type.resolve().toTypeName()

        properties.add(
            ParametersDefinition(ksPropertyDecleration.simpleName.getShortName(), returnType))
      }
    }

    return UsfViewModelClassBuilderDefinition(
        constructorParams,
        functions,
        properties,
        ParametersDefinition(
            classDeclaration.simpleName.getShortName(), classDeclaration.toClassName()))
  }

  override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
    function.returnType!!.accept(this, Unit)
  }
}
