package co.kaush.msusf.processors

import co.kaush.msusf.processor.UsfViewModelProcessor
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * this class is responsible for instantiating the [UsfViewModelProcessor] processor and introducing it
 * to the KSP
 */
class UsfViewModelProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return UsfViewModelProcessor(
        environment.codeGenerator,
        environment.logger,
        environment.options,
    )
  }
}
