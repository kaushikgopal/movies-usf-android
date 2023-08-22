package co.kaush.msusf.usf

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

  val effects: SharedFlow<Effect>
}
