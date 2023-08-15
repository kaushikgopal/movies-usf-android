package co.kaush.msusf.movies

import co.kaush.msusf.annotations.UsfViewModel
import co.kaush.msusf.usf.UsfViewModelImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@UsfViewModel
class MSMovieViewModelImpl(
    private val movieRepo: MSMovieRepository,
    coroutineScope: CoroutineScope,
) :
    UsfViewModelImpl<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect>(
        MSMovieViewState(),
        coroutineScope,
    ) {

  // -----------------------------------------------------------------------------------
  // Event -> Results
  override suspend fun eventToResultFlow(event: MSMovieEvent): Flow<MSMovieResult> {
    return when (event) {
      is MSMovieEvent.ScreenLoadEvent -> onScreenLoad()
      is MSMovieEvent.SearchMovieEvent -> onSearchMovie(event)
      is MSMovieEvent.AddToHistoryEvent -> onAddToHistory(event)
      is MSMovieEvent.RestoreFromHistoryEvent -> onRestoreFromHistory(event)
    }
  }

  private fun onScreenLoad(): Flow<MSMovieResult> = flow { emit(MSMovieResult.ScreenLoadResult()) }

  private fun onSearchMovie(event: MSMovieEvent.SearchMovieEvent): Flow<MSMovieResult> {
    return flow {
      emit(MSMovieResult.SearchMovieResult(loading = true))
      try {
        val movie = movieRepo.searchMovie(event.searchedMovieTitle)
        emit(
            MSMovieResult.SearchMovieResult(
                movie = movie,
                errorMessage = movie?.errorMessage ?: "",
            ),
        )
      } catch (e: Exception) {
        emit(
            MSMovieResult.SearchMovieResult(
                movie = MSMovie(result = false, errorMessage = e.localizedMessage),
                errorMessage = e.localizedMessage ?: "",
            ),
        )
      }
    }
  }

  private fun onAddToHistory(event: MSMovieEvent.AddToHistoryEvent): Flow<MSMovieResult> {
    return flow { emit(MSMovieResult.AddToHistoryResult(movie = event.searchedMovie)) }
  }

  private fun onRestoreFromHistory(
      event: MSMovieEvent.RestoreFromHistoryEvent
  ): Flow<MSMovieResult> {
    return flow { emit(MSMovieResult.SearchMovieResult(movie = event.movieFromHistory)) }
  }

  // -----------------------------------------------------------------------------------
  // Results -> ViewState

  override suspend fun resultToViewState(
      currentViewState: MSMovieViewState,
      result: MSMovieResult
  ): MSMovieViewState {
    return when {
      result.loading -> {
        currentViewState.copy(
            searchBoxText = "",
            searchedMovieTitle = "Searching Movie...",
            searchedMovieRating = "",
            searchedMoviePoster = "",
            searchedMovieReference = null,
        )
      }
      result.errorMessage.isNotBlank() -> {
        currentViewState.copy(searchedMovieTitle = result.errorMessage)
      }
      else -> result.toViewState(currentViewState)
    }
  }

  // -----------------------------------------------------------------------------------
  // Results -> ViewEffect

  override suspend fun resultToViewEffectFlow(result: MSMovieResult): Flow<MSMovieViewEffect?> =
      result.toViewEffect()
}
