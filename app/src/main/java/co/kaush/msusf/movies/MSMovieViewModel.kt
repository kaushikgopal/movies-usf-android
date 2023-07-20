package co.kaush.msusf.movies

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.kaush.msusf.MSApp
import co.kaush.msusf.usf.UsfVm
import kotlinx.coroutines.CoroutineScope

/**
 * For this example, a simple ViewModel would have sufficed, but in most real world examples we
 * would use an AndroidViewModel
 *
 * Our Unit tests should still be able to run given this
 */
class MSMovieViewModel(
    movieRepo: MSMovieRepository,
    app: MSApp,
    coroutineScope: CoroutineScope? = null,
) : AndroidViewModel(app), UsfVm<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect> {

  private val usfViewModelImpl:
      UsfVm<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect> =
      MSMovieViewModelImpl(
          movieRepo,
          coroutineScope ?: viewModelScope,
      )

  override fun processInput(event: MSMovieEvent) = usfViewModelImpl.processInput(event)

  class MSMovieVmFactory(
      private val app: MSApp,
      private val movieRepo: MSMovieRepository,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MSMovieViewModel(movieRepo, app) as T
    }
  }

  override val viewState = usfViewModelImpl.viewState
  override val viewEffect = usfViewModelImpl.viewEffect
}
