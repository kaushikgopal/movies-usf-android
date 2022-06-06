package co.kaush.msusf.usf

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class UsfVmImpl<E: Any, R: Any, VS: Any, VE: Any>(
    eventToResultTransformer: ObservableTransformer<E, R>,
    resultToViewStateTransformer: ObservableTransformer<R, VS>,
    resultToViewEffectTransformer: ObservableTransformer<R, VE>,
    logger: UsfVmLogger = object : UsfVmLogger {
        override fun debug(message: String) = Timber.d(message)
        override fun warning(message: String) = Timber.w(message)
        override fun error(error: Throwable, message: String)  = Timber.e(error, message)
    }
) : UsfVm<E, R, VS, VE> {

    override val eventSink: PublishSubject<E> = PublishSubject.create()
    override val disposables: CompositeDisposable = CompositeDisposable()

    private val viewStateSink: Observable<VS>
    private val viewEffectSink: Observable<VE>

    init {
        logger.debug("------ init ${Thread.currentThread().name}")

        eventSink
            .compose(eventToResultTransformer)
            .doOnNext { logger.debug("----- result ${Thread.currentThread().name} $it") }

            // share the result stream otherwise it will get subscribed to multiple times
            // in the following also block
            .share()

            .also { result ->
                 logger.debug("------ also ${Thread.currentThread().name}")
                viewStateSink = result
                    .compose(resultToViewStateTransformer)


                    // if the viewState is identical
                    // there's little reason to re-emit the same view state
                    .distinctUntilChanged()

                    .doOnNext { logger.debug("----- vs $it") }

                    // when a screen rebinds to the ViewModel after rotation/config change
                    // emit the last known viewState to new screen subscriber
                    .replay(1)


                    // autoConnect makes sure the streams stays alive even when the UI disconnects
                    // autoConnect(0) kicks off the stream without waiting for anyone to subscribe
                    .autoConnect(0) { disposables.addAll(it)}


                viewEffectSink = result
                    .compose(resultToViewEffectTransformer)
                    .doOnNext { logger.debug("----- ve $it") }
            }
    }

    override fun viewState(): Observable<VS> = viewStateSink

    override fun viewEffect(): Observable<VE> = viewEffectSink

    interface UsfVmLogger {
        fun debug(message: String)
        fun warning(message: String)
        fun error(error: Throwable, message: String)
    }
}
