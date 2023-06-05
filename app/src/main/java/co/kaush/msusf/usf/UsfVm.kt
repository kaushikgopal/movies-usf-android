package co.kaush.msusf.usf

import kotlinx.coroutines.flow.Flow

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

  fun viewState(): Flow<ViewState>

  fun viewEffect(): Flow<ViewEffect>
}
