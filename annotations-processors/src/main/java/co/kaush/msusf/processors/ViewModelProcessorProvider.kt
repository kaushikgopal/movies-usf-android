package co.kaush.msusf.processors

import co.kaush.msusf.processor.ViewModelProcessor
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * this class is responsible for instantiating the [ViewModelProcessor] processor and introducing it
 * to the KSP
 */
class ViewModelProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return ViewModelProcessor(
        environment.codeGenerator,
        environment.logger,
        environment.options,
    )
  }
}
