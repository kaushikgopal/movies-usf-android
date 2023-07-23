package co.kaush.msusf.processors

import com.squareup.kotlinpoet.TypeName

data class UsfViewModelClassBuilderDefinition(
    val parameters: List<ParametersDefinition>,
    val functions: List<FunctionsDefinition>,
    val properties: List<ParametersDefinition>,
    val viewModel: ParametersDefinition
) {
  val simplifiedClassName: String
    get() {
      val vmConstant = "ViewModel"
      val className = this.viewModel.paramName
      val indexOfViewModel = className.indexOf(vmConstant)
      return if (indexOfViewModel != -1) {
        className.substring(0, indexOfViewModel + vmConstant.length)
      } else {
        className + "GenViewModel"
      }
    }
}

data class FunctionsDefinition(
    val name: String,
    val parameters: List<ParametersDefinition>,
    val returnType: TypeName?
)

data class ParametersDefinition(
    val paramName: String,
    val paramType: TypeName,
)
