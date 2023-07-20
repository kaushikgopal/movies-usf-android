package co.kaush.msusf.processors

import com.google.devtools.ksp.innerArguments
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName

class UsfViewModelVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : KSVisitorVoid() {

  private val reservedFunctionsNames =
      setOf("equals", "hashCode", "toString", "getApplication", "<init>")

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.CLASS) {
      logger.error("Only a class can be annotated with this annotation", classDeclaration)
      throw IllegalArgumentException()
    }

    if (classDeclaration.containingFile == null) {
      logger.error(
          "Attempted to add annotation to file that does not exist",
          classDeclaration.containingFile,
      )
      throw IllegalArgumentException()
    }

    val classBuilderDefinition = generateGenericClassBuilderDefinition(classDeclaration)

    val packageName = classDeclaration.toClassName().packageName

    val fileSpecForClass =
        UsfViewModelFileBuilder.buildFileSpec(classBuilderDefinition, packageName)

    codeGenerator
        .createNewFile(
            dependencies = Dependencies(false, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = fileSpecForClass.name,
        )
        .use { it.write(fileSpecForClass.toString().toByteArray()) }
  }

  private fun generateGenericClassBuilderDefinition(
      classDeclaration: KSClassDeclaration
  ): UsfViewModelClassBuilderDefinition {
    val constructorParams = mutableListOf<ParametersDefinition>()

    classDeclaration.primaryConstructor?.parameters?.forEach { ksValueParameter ->
      val paramType = ksValueParameter.type.resolve().toClassName()
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
            funParams.add(ParametersDefinition(varName, it.type.resolve().toClassName()))
          }
        }

        val returnType =
            try {
              ksFunctionDeclaration.returnType?.resolve()?.toClassName()
            } catch (ex: Exception) {
              throw ex
            }

        // might need to include nullability data....
        functions.add(
            FunctionsDefinition(
                functionName,
                funParams,
                returnType,
            ),
        )
      }
    }

    val properties = mutableListOf<ParametersDefinition>()
    classDeclaration.getAllProperties().forEach { ksPropertyDecleration ->
      if (ksPropertyDecleration.isPublic()) {

        val returnType = ksPropertyDecleration.type.resolve()

        val genericParams =
            returnType.innerArguments.mapNotNull { it.type?.resolve()?.toClassName() }

        // TODO: Figure this out
        val returnTypeClassName =
            if (genericParams.isNotEmpty()) {
              returnType.toClassName().parameterizedBy(genericParams).rawType
            } else {
              returnType.toClassName()
            }

        properties.add(
            ParametersDefinition(
                ksPropertyDecleration.simpleName.getShortName(),
                returnTypeClassName,
            ),
        )
      }
    }

    return UsfViewModelClassBuilderDefinition(
        constructorParams,
        functions,
        properties,
        ParametersDefinition(
            classDeclaration.simpleName.getShortName(),
            classDeclaration.toClassName(),
        ),
    )
  }

  override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
    function.returnType!!.accept(this, Unit)
  }

  override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
    super.visitTypeReference(typeReference, data)
  }
}
