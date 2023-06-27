package co.kaush.msusf.processors

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid

class ViewModelVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    // visit the functions of the class
    classDeclaration.getDeclaredFunctions().forEach { it.accept(this, Unit) }
  }

  override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
    function.returnType!!.accept(this, Unit)
  }

  override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
    super.visitTypeReference(typeReference, data)
  }
}
