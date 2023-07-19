package co.kaush.msusf.processors

import com.squareup.kotlinpoet.ClassName

data class UsfViewModelClassBuilderDefinition(
    val parameters: List<ParametersDefinition>,
    val functions: List<FunctionsDefinition>,
    val properties: List<ParametersDefinition>,
    val viewModel: ParametersDefinition
)

data class FunctionsDefinition(
    val name: String,
    val parameters: List<ParametersDefinition>,
    val returnType: ClassName?
)

data class ParametersDefinition(
    val paramName: String,
    val paramType: ClassName,
)
