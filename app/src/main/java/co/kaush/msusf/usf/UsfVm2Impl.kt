package co.kaush.msusf.usf

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class UsfVm2Impl<E: Any, R: Any, VS: Any, VE: Any>(
    eventToResultTransformer: (E) -> Flow<R>,
    resultToViewStateTransformer: (R) -> Flow<VS>,
    resultToViewEffectTransformer: (R) -> Flow<VE>,
    viewModelScope: CoroutineScope,
    processingDispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: UsfVmLogger = object : UsfVmLogger {
        override fun debug(message: String) = Timber.d(message)
        override fun warning(message: String) = Timber.w(message)
        override fun error(error: Throwable, message: String)  = Timber.e(error, message)
    }
) : UsfVm2<E, R, VS, VE> {

    private val eventSink = MutableSharedFlow<E>()
    private lateinit var viewStateSink: Flow<VS>
    private lateinit var viewEffectSink: Flow<VE>

    init {
        logger.debug("------ init ${Thread.currentThread().name}")

        viewModelScope.launch {
            val result: Flow<R> = eventSink
                .flatMapLatest(transform = eventToResultTransformer)
                .onEach { logger.debug("----- result ${Thread.currentThread().name} $it") }

                // share the result stream otherwise it will get subscribed to multiple times
                // in the following also block
                //  TODO: i'm not yet sure if we need this given we're using a mutable shared flow

            withContext(processingDispatcher) {
                viewStateSink = result
                                    .flatMapLatest(transform = resultToViewStateTransformer)

                                    // if the viewState is identical
                                    // there's little reason to re-emit the same view state
                                    .distinctUntilChanged()

                                    .onEach { logger.debug("----- vs $it") }

                                    .shareIn(
                                        // sharing is started within view model scope
                                        viewModelScope,

                                        // stream starts when first subscriber appears
                                        // stops immediately when last subscriber disappears
                                        // but keeps replay  forever
                                        SharingStarted.WhileSubscribed(),

                                        // keep the last view state in cache
                                        // for new UI subscribers
                                        replay = 1
                                    )

                viewEffectSink = result
                                    .flatMapLatest(transform = resultToViewEffectTransformer)
                                    .onEach { logger.debug("----- ve $it") }
            }
        }

    }

    override suspend fun processInput(event: E) = eventSink.emit(event)

    override suspend fun viewState(): Flow<VS> = viewStateSink

    override suspend fun viewEffect(): Flow<VE> = viewEffectSink

    interface UsfVmLogger {
        fun debug(message: String)
        fun warning(message: String)
        fun error(error: Throwable, message: String)
    }
}
