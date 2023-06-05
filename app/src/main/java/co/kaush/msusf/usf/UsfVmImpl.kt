package co.kaush.msusf.usf

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
abstract class UsfVmImpl<E : Any, R : Any, VS : Any, VE : Any>(
  private val viewModelScope: CoroutineScope,
  private val processingDispatcher: CoroutineDispatcher = Dispatchers.IO,
  logger: UsfVmLogger = object : UsfVmLogger {
    override fun debug(message: String) = Timber.d(message)
    override fun warning(message: String) = Timber.w(message)
    override fun error(error: Throwable, message: String) = Timber.e(error, message)
  }
) : UsfVm<E, R, VS, VE> {

  // remember that a single E(vent) can yield multiple R(esult)s
  //  so if you wish to show an initial loading state before the final result
  //  emit multiple R(esult)s for the single E(vent)
  abstract fun eventToResultFlow(): (E) -> Flow<R>

  // curiously, we don't return a Flow<VS> here
  //  as every (R)esult will only ever be transformed into a single (V)iew(S)tate
  //  if you want multiple (V)iew(S)tates, then emit multiple (R)esults
  //  and transform each (R)esult accordingly to the respective (V)iew(S)tate
  abstract fun resultToViewState(): (currentViewState: VS, result: R) -> VS

  // a single (R)esult can result in multiple (V)iew(E)ffects
  //  for e.g. emit one VE for navigation and another for making an analytics call
  //  hence a return type of Flow<VE>
  abstract fun resultToViewEffectFlow(): (R) -> Flow<VE>

  abstract val initialViewState: VS
  private val eventSink = MutableSharedFlow<E>()
  private lateinit var viewStateSink: Flow<VS>
  private lateinit var viewEffectSink: Flow<VE>


  init {
    logger.debug("------ init ${Thread.currentThread().name}")

    viewModelScope.launch {
      val resultFlow: Flow<R> = eventSink.flatMapLatest(transform = eventToResultFlow())
          .onEach { logger.debug("----- result ${Thread.currentThread().name} $it") }

      // share the result stream otherwise it will get subscribed to multiple times
      // in the following also block
      //  TODO: i'm not yet sure if we need this given we're using a mutable shared flow

      withContext(processingDispatcher) {
        viewStateSink = resultFlow
            .scan(initialViewState, resultToViewState())

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
                replay = 1,
            )

        viewEffectSink = resultFlow.flatMapLatest(transform = resultToViewEffectFlow())
            .onEach { logger.debug("----- ve $it") }
      }
    }

  }

  override fun processInput(event: E) {
    viewModelScope.launch(processingDispatcher) {
      eventSink.emit(event)
    }
  }

  override fun viewState(): Flow<VS> = viewStateSink

  override fun viewEffect(): Flow<VE> = viewEffectSink

  interface UsfVmLogger {
    fun debug(message: String)
    fun warning(message: String)
    fun error(error: Throwable, message: String)
  }
}
