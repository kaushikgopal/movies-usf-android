package co.kaush.msusf.usf

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 *
 * [Any]    type used because of a Kotlin Compiler bug
 *          that won't take in @NotNull event: E
 *          https://youtrack.jetbrains.com/issue/KT-36770
 *
 * @param Event
 */
interface UsfVm2<Event : Any, Result, ViewState : Any, ViewEffect : Any> {

    suspend fun processInput(event: Event)

    suspend fun viewState(): Flow<ViewState>

    suspend fun viewEffect(): Flow<ViewEffect>
}