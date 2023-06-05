package co.kaush.msusf.movies

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.MSMovieResult.AddToHistoryResult
import co.kaush.msusf.movies.MSMovieResult.ScreenLoadResult
import co.kaush.msusf.movies.MSMovieResult.SearchMovieResult
import co.kaush.msusf.movies.MSMovieViewEffect.AddedToHistoryToastEffect
import co.kaush.msusf.usf.UsfVm
import co.kaush.msusf.usf.UsfVmImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

  private val usfVm: UsfVm<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect> =
      MSMovieVmImpl(
          movieRepo,
          viewModelScope,
      )

  override fun processInput(event: MSMovieEvent) = usfVm.processInput(event)
  override fun viewState(): Flow<MSMovieViewState> = usfVm.viewState()
  override fun viewEffect(): Flow<MSMovieViewEffect> = usfVm.viewEffect()

  class MSMovieVmFactory(
    private val app: MSApp,
    private val movieRepo: MSMovieRepository,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MSMovieVm(app, movieRepo) as T
    }
  }
}

class MSMovieVmImpl(
  private val movieRepo: MSMovieRepository,
  viewModelScope: CoroutineScope,
) : UsfVmImpl<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect>(viewModelScope) {

  // -----------------------------------------------------------------------------------
  // Event -> Results

  override fun eventToResultFlow(): (MSMovieEvent) -> Flow<MSMovieResult> {
    return { event ->
      when (event) {
        is ScreenLoadEvent -> onScreenLoad(event)
        is SearchMovieEvent -> onSearchMovie(event)
        is AddToHistoryEvent -> onAddToHistory(event)
        is RestoreFromHistoryEvent -> onRestoreFromHistory(event)
      }
    }
  }

  private fun onScreenLoad(event: ScreenLoadEvent): Flow<MSMovieResult> =
      flow { emit(ScreenLoadResult()) }

  private fun onSearchMovie(event: SearchMovieEvent): Flow<MSMovieResult> {
    return flow {
      emit(SearchMovieResult(loading = true))
      try {
        val movie = movieRepo.searchMovie(event.searchedMovieTitle)
        emit(SearchMovieResult(movie = movie, errorMessage = movie?.errorMessage ?: ""))
      } catch (e: Exception) {
        emit(
            SearchMovieResult(
                movie = MSMovie(result = false, errorMessage = e.localizedMessage),
                errorMessage = e.localizedMessage ?: "",
            ),
        )
      }
    }
  }

  private fun onAddToHistory(event: AddToHistoryEvent): Flow<MSMovieResult> {
    return flow {
      emit(AddToHistoryResult(movie = event.searchedMovie))
    }
  }

  private fun onRestoreFromHistory(event: RestoreFromHistoryEvent): Flow<MSMovieResult> {
    return flow {
      emit(SearchMovieResult(movie = event.movieFromHistory))
    }
  }

  // -----------------------------------------------------------------------------------
  // Results -> ViewState
  override val initialViewState: MSMovieViewState = MSMovieViewState()

  override fun resultToViewState():
      (currentViewState: MSMovieViewState, result: MSMovieResult) -> MSMovieViewState {
    return { vs, result ->
      when {
        result.loading -> {
          vs.copy(
              searchBoxText = null,
              searchedMovieTitle = "Searching Movie...",
              searchedMovieRating = "",
              searchedMoviePoster = "",
              searchedMovieReference = null,
          )
        }

        result.errorMessage.isNotBlank() -> {
          vs.copy(searchedMovieTitle = result.errorMessage)
        }

        result is ScreenLoadResult -> {
          vs.copy(searchBoxText = "load test")
        }

        result is SearchMovieResult -> {
          val movie: MSMovie = result.movie!!

          vs.copy(
              searchBoxText = movie.title,
              searchedMovieTitle = movie.title,
              searchedMovieRating = movie.ratingSummary,
              searchedMoviePoster = movie.posterUrl,
              searchedMovieReference = movie,
          )
        }

        result is AddToHistoryResult -> {
          val movie: MSMovie = result.movie!!

          if (!vs.adapterList.contains(movie)) {
            vs.copy(adapterList = vs.adapterList.plus(movie))
          } else vs.copy()
        }

        else -> throw RuntimeException("Unexpected result")
      }
    }
  }

  // -----------------------------------------------------------------------------------
  // Results -> ViewEffect

  override fun resultToViewEffectFlow(): (MSMovieResult) -> Flow<MSMovieViewEffect> {
    return { result ->
      when (result) {
        is AddToHistoryResult -> flow { emit(AddedToHistoryToastEffect) }
        else -> throw RuntimeException("Unexpected result")
      }
    }
  }
}
