package co.kaush.msusf.processors

import co.kaush.msusf.processors.UsfViewModelClassBuilderDefinition.Companion.RESERVED_WORDS
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid

class UsfViewModelVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.CLASS) {
      logger.error("Only class can be annotated with @CLASS", classDeclaration)
      throw IllegalArgumentException()
    }

    // ensure no reserved words are used
    RESERVED_WORDS.forEach { reservedWord ->
      if (classDeclaration.simpleName.getShortName().endsWith(reservedWord)) {
        logger.error("Class can not end in $reservedWord", classDeclaration)
        throw UnsupportedOperationException()
      }
    }





  }

  override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
    function.returnType!!.accept(this, Unit)
  }

  override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
    super.visitTypeReference(typeReference, data)
  }
}
