package co.kaush.usf

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * We intentionally name this interface [UsfVm] instead of `UsfViewModel` because we want to avoid
 * naming conflict with the annotation @[UsfViewModel]
 *
 * This interface provides the contract for the ViewModel to follow USF/UDF architecture. Having
 * this as an interface allows us to possibly avoid using of [AndroidViewModel] or [ViewModel] if we
 * don't need it.
 *
 * [Any] type used because of a Kotlin Compiler bug that won't take in @NotNull event: E
 * https://youtrack.jetbrains.com/issue/KT-36770
 *
 * @param Event
 */
interface UsfVm<Event : Any, ViewState : Any, Effect : Any> {

  fun processInput(event: Event)

  val viewState: StateFlow<ViewState>

  /**
   * we use a "shared" flow vs state flow here to avoid conflation of state flows.
   *
   * every effect must be sent out and cannot be ignored even if there are multiple side effects
   * emitted quickly/simultaneously as that could have implications to the Screen logic
   *
   * there are times where we _want_ to ignore certain effects (like multiple loading spinner calls)
   * these can be handled in the Results emission layer.
   */
  val effects: SharedFlow<Effect>
}
