package co.kaush.msusf.processor

import co.kaush.msusf.annotations.ViewModel
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
   * @param resolver [Resolver] helps you grab a bunch of info on class annotated with [ViewModel]
   *   including classes, properties, functions (each having their own [KDeclaration])
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

    return emptyList()
  }
}