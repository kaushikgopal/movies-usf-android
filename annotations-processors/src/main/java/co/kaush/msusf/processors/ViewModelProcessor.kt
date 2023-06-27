package co.kaush.msusf.processor

import co.kaush.msusf.annotations.ViewModel
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.asClassName

/**
 * This processor handles all classes annotated with @[ViewModel]. You typically annotate a
 * ViewModel implementation class (*VMImpl) with @[ViewModel]. The processor then goes on to
 * generate the corresponding *VM boilerplate code necessary to access the ViewModel from an
 * Activity/Fragment.
 *
 * @param codeGenerator [CodeGenerator] responsible for creating the files that will be generated
 *   using the code from the processor
 * @param logger [KSPLogger] responsible for logging messages to the console (errors/warnings)
 */
class ViewModelProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

  /**
   * This function contains the main logic to process the [ViewModel] annotation.
   *
   * @param resolver [Resolver] helps you grab a bunch of info on class annotated with
   *   [UsfViewModel] including classes, properties, functions (each having their own
   *   [KDeclaration])
   */
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val viewModelClassName = ViewModel::class.asClassName()

    // find all classes that have been annotated with @ViewModel
    val symbols =
        resolver
            .getSymbolsWithAnnotation(viewModelClassName.toString())
            .filterIsInstance<KSClassDeclaration>()

    // symbols is a [Sequence] not a [List], so we don't know its size in advance
    // hence the use of an iterator to check if it's empty
    if (!symbols.iterator().hasNext()) return emptyList()

    val sourceFiles = symbols.mapNotNull { it.containingFile }
    val fileText = buildString {
      sourceFiles.forEach {
        append("// ")
        append(it.fileName)
        append("\n")
      }
    }
    val file =
        codeGenerator.createNewFile(
            Dependencies(
                false,
                *sourceFiles.toList().toTypedArray(),
            ),
            "your.generated.file.package",
            "GeneratedLists",
        )

    file.write(fileText.toByteArray())

    /*
          symbols.forEach { classDeclaration ->
            // The ViewModelContainer provide information on what Dagger Scope the generated classes
            // should be added to all of these values are hard coded strings not the best, but I couldn't
            // figure out a better way
            val scopeArg: KSValueArgument? =
                classDeclaration.annotations
                    .first { it.shortName.asString() == viewModelClassName.simpleName }
                    .arguments
                    .firstOrNull { arg -> arg.name?.asString() == "scope" }

            if (scopeArg == null) {
              classDeclaration.accept(ViewModelVisitor(codeGenerator, logger), Unit)
            } else {
              val scopeClassType = (scopeArg.value as KSType).toClassName()

              if (scopeClassType == Nothing::class.asClassName() ||
                scopeClassType == Void::class.asClassName()) {
                classDeclaration.accept(ViewModelVisitor(codeGenerator, logger), Unit)
              } else {
      //          classDeclaration.accept(Visitor(scopeClassType), Unit)
              }
            }

          }
    */

    return (symbols).filterNot { it.validate() }.toList()
  }
}
