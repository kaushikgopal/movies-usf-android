package co.kaush.msusf.processors

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

data class UsfViewModelClassBuilderDefinition(
    private val classType: ClassName,
    val simpleViewModelType: String,
    val viewModelType: ClassName,
    val viewModelSubTypes: List<ClassName> = emptyList(),
    val params: List<VariableDefinition> = emptyList(),
    val outputDirectory: String,
    val containingFile: KSFile
) {

  val packageName
    get() = classType.packageName

  val containerClassName
    get() = classType.simpleName

  private var _baseClassName: String? = null
  private val baseClassName: String
    get() {
      _baseClassName =
          if (_baseClassName == null) {
            classType.simpleName.split("ViewModel")[0]
          } else {
            _baseClassName
          }
      return _baseClassName!!
    }

  val androidViewModelClassName
    get() = "${baseClassName}ViewModel"

  val bindModuleClassName
    get() = "${baseClassName}Module"

  val assistedInjectParamDataClassName
    get() = "${baseClassName}Params"

  val vmFactoryClassName
    get() = "${androidViewModelClassName}Factory"

  val assistedFactoryClassName
    get() = "${vmFactoryClassName}AssistedFactory"

  val bindingsClassName
    get() = "${baseClassName}Bindings"

  private var _viewModelClassNameWithSubTypes: ParameterizedTypeName? = null
  val viewModelClassNameWithSubTypes: ParameterizedTypeName
    get() {
      if (_viewModelClassNameWithSubTypes != null) _viewModelClassNameWithSubTypes
      var delegateClassType = viewModelType.parameterizedBy(viewModelSubTypes.first())
      for (i in 1 until viewModelSubTypes.size) {
        delegateClassType = delegateClassType.plusParameter(viewModelSubTypes[i])
      }
      _viewModelClassNameWithSubTypes = delegateClassType
      return _viewModelClassNameWithSubTypes!!
    }

  companion object {
    val RESERVED_WORDS
      get() =
          listOf(
              "ViewModel",
              "Module",
              "Params",
              "ViewModelFactory",
              "ViewModelFactoryAssistedFactory",
              "Bindings",
          )
  }
}

data class VariableDefinition(
    val isVariableAnnotated: Boolean,
    val variableName: String,
    var variableClass: ClassName,
    val isSaveStateHandle: Boolean
)
