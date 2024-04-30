package co.kaush.msusf.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewModelScope
import java.lang.Class
import kotlin.Suppress
import kotlin.Unit
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

public class MSMovieViewModel(
    movieRepo: MSMovieRepository,
) : ViewModel() {
  private val MSMovieViewModelImpl: MSMovieViewModelImpl =
      MSMovieViewModelImpl(movieRepo = movieRepo, coroutineScope = viewModelScope)

  public val effects: SharedFlow<MSMovieEffect>
    get() = MSMovieViewModelImpl.effects

  public val viewState: StateFlow<MSMovieViewState>
    get() = MSMovieViewModelImpl.viewState

  public fun processInput(event: MSMovieEvent): Unit =
      MSMovieViewModelImpl.processInput(event = event)

  public class MSMovieViewModelFactory(
      private val movieRepo: MSMovieRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    public override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MSMovieViewModel(movieRepo = movieRepo) as T
  }
}
