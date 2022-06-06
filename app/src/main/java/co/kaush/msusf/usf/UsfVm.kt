package co.kaush.msusf.usf

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

/**
 *
 * [Any]    type used because of a Kotlin Compiler bug
 *          that won't take in @NotNull event: E
 *          https://youtrack.jetbrains.com/issue/KT-36770
 *
 * @param Event
 */
interface UsfVm<Event : Any, Result, ViewState : Any, ViewEffect : Any> {

    val eventSink: PublishSubject<Event>
    val disposables: CompositeDisposable

    fun processInput(event: Event) {
        eventSink.onNext(event)
    }

    /**
     * It is unnecessary to do `subscribeOn(Schedulers.io())` as
     * the chain executes on the thread that pushed an "event" into [processInput].
     *
     * **Example:**
     * ```
     * vm.viewState
     *  .observeOn(AndroidSchedulers.mainThread())
     *  .subscribe(::render, Timber::w)
     * ```
     */
    fun viewState(): Observable<ViewState>

    fun viewEffect(): Observable<ViewEffect>

    fun clear() = disposables.clear()
}