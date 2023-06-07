package co.kaush.msusf.movies

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.kaush.msusf.MSApp
import co.kaush.msusf.usf.UsfVm

/**
 * For this example, a simple ViewModel would have sufficed,
 * but in most real world examples we would use an AndroidViewModel
 *
 * Our Unit tests should still be able to run given this
 */
class MSMovieVm(
    app: MSApp,
    movieRepo: MSMovieRepository,
) : AndroidViewModel(app),
  UsfVm<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect> {

  private val usfVmImpl: UsfVm<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect> =
      MSMovieVmImpl(
          movieRepo,
          viewModelScope,
      )

  override fun processInput(event: MSMovieEvent) = usfVmImpl.processInput(event)

  class MSMovieVmFactory(
    private val app: MSApp,
    private val movieRepo: MSMovieRepository,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MSMovieVm(app, movieRepo) as T
    }
  }

  override val viewState = usfVmImpl.viewState
  override val viewEffect = usfVmImpl.viewEffect
}
