package co.kaush.msusf.processors

import com.squareup.kotlinpoet.ClassName

data class UsfViewModelClassBuilderDefinition(
    val parameters: List<ParametersDefinition>,
    val functions: List<FunctionsDefinition>,
    val properties: List<ParametersDefinition>,
    val viewModel: ParametersDefinition
) {

  /** Trim all characters after ViewModel e.g. MyFancyViewModelImpl -> MyFancyViewModel */
  fun simplifiedClassName(): String {
    val className = this.viewModel.paramName
    val indexOfViewModel = className.indexOf("ViewModel")
    return if (indexOfViewModel != -1) {
      className.substring(0, indexOfViewModel + "ViewModel".length)
    } else className
  }
}

data class FunctionsDefinition(
    val name: String,
    val parameters: List<ParametersDefinition>,
    val returnType: ClassName?
)

data class ParametersDefinition(
    val paramName: String,
    val paramType: ClassName,
)
