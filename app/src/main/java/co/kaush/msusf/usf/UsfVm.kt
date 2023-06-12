package co.kaush.msusf.usf

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 *
 * [Any]    type used because of a Kotlin Compiler bug
 *          that won't take in @NotNull event: E
 *          https://youtrack.jetbrains.com/issue/KT-36770
 *
 * @param Event
 */
interface UsfVm<Event : Any, Result, ViewState : Any, ViewEffect : Any> {

  fun processInput(event: Event)

  val viewState: StateFlow<ViewState>

  val viewEffect: SharedFlow<ViewEffect>
}
