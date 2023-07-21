package co.kaush.msusf.movies

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
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
    coroutineScope: CoroutineScope? = null,
    handle: SavedStateHandle? = null,
) : ViewModel() {

  private val usfViewModelImpl:
      UsfVm<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect> =
      MSMovieViewModelImpl(
          movieRepo,
          coroutineScope ?: viewModelScope,
      )

  override fun processInput(event: MSMovieEvent) = usfViewModelImpl.processInput(event)

  override val viewState = usfViewModelImpl.viewState
  override val viewEffect = usfViewModelImpl.viewEffect

  // Define ViewModel factory in a companion object
  class MSMovieViewModelFactory(
      private val movieRepo: MSMovieRepository,
      owner: SavedStateRegistryOwner,
      defaultArgs: Bundle? = null,
  ) : AbstractSavedStateViewModelFactory(owner = owner, defaultArgs = defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T =
        MSMovieViewModel(
            movieRepo = movieRepo,
            handle = handle,
        )
            as T
  }
}
