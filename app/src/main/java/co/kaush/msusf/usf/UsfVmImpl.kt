package co.kaush.msusf.usf

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
abstract class UsfVmImpl<E : Any, R : Any, VS : Any, VE : Any>(
  initialState: VS,
  private val viewModelScope: CoroutineScope,
  private val processingDispatcher: CoroutineDispatcher = Dispatchers.IO,
  logger: UsfVmLogger = object : UsfVmLogger {
    override fun debug(message: String) = Timber.d(message)
    override fun warning(message: String) = Timber.w(message)
    override fun error(error: Throwable, message: String) = Timber.e(error, message)
  }
) : UsfVm<E, R, VS, VE> {

  /**
   * @return [Flow]<[R]> a single [E]vent can result in multiple [R]esults
   *                     for e.g. emit a R for loading and another for the actual result
   *
   *  @param event every input is processed into an [E]vent
   */
  abstract suspend fun eventToResultFlow(event: E): Flow<R>

  /**
   * @param currentViewState  the current [VS]tate of the view
   *                          (.copy it for the returned [VS]tate)
   *
   * @return [VS]tate Curiously, we don't return a [Flow]<[VS]> here
   *                  every [R]esult will only ever be transformed into a single [VS]tate
   *                  if you want multiple [VS]tates emit multiple [R]esults
   *                   transforming each [R]esult to the respective [VS]tate
   */
  abstract suspend fun resultToViewState(currentViewState: VS, result: R): VS

  /**
   * @param result  a single [R]esult can result in multiple [VE]s
   *                for e.g. emit a VE for navigation and another for an analytics call
   *                hence a return type of [Flow]<[VE]>
   *
   * @return [Flow] of [VE]s where null emissions will be ignored automatically
   */
  abstract suspend fun resultToViewEffectFlow(result: R): Flow<VE?>

  private val _events = MutableSharedFlow<E>()
  private val _viewState = MutableStateFlow(initialState)
  private val _viewEffects = MutableSharedFlow<VE>()

  override val viewState = _viewState.asStateFlow()
  override val viewEffect = _viewEffects.asSharedFlow()

  init {
    logger.debug("------ [init] ${Thread.currentThread().name}")

    viewModelScope.launch(processingDispatcher) {
        _events
            .flatMapConcat { event ->
              logger.debug("----- [event] ${Thread.currentThread().name} $event")
              eventToResultFlow(event)
            }
            .collect { result ->
              logger.debug("----- [result] ${Thread.currentThread().name} $result")

              // StateFlow already behaves as if distinctUntilChanged operator is applied to it
              resultToViewState(_viewState.value, result).let { vs ->
                logger.debug("----- [view-state] ${Thread.currentThread().name} $result")
                _viewState.emit(vs)
              }

              // effects are emitted after a view state by virtue of this collect call
              // (rarely) would we want VS & VE to be emitted at the exact same instant
              _viewEffects.emitAll(
                  resultToViewEffectFlow(result)
                      .filterNotNull()
                      .onEach { logger.debug("----- [view-effect] ${Thread.currentThread().name} $it") },
              )
            }
//      }
    }
  }

  override fun processInput(event: E) {
    viewModelScope.launch(processingDispatcher) {
      _events.emit(event)
    }
  }

  interface UsfVmLogger {
    fun debug(message: String)
    fun warning(message: String)
    fun error(error: Throwable, message: String)
  }
}
