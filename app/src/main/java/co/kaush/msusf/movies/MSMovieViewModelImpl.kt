package co.kaush.msusf.movies

import co.kaush.msusf.movies.MSMovieEvent.LongRunningEvent
import co.kaush.usf.UsfViewModelImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// @UsfViewModel
class MSMovieViewModelImpl(
    private val movieRepo: MSMovieRepository,
    coroutineScope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) :
    UsfViewModelImpl<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieEffect>(
        MSMovieViewState(),
        coroutineScope,
        dispatcher,
    ) {

  // -----------------------------------------------------------------------------------
  // Event -> Results
  override fun eventToResultFlow(event: MSMovieEvent): Flow<MSMovieResult> {
    return when (event) {
      is MSMovieEvent.ScreenLoadEvent -> onScreenLoad()
      is MSMovieEvent.SearchMovieEvent -> onSearchMovie(event)
      is MSMovieEvent.AddToHistoryEvent -> onAddToHistory(event)
      is MSMovieEvent.RestoreFromHistoryEvent -> onRestoreFromHistory(event)
      is LongRunningEvent -> onLongRunningTask(event)
    }
  }

  private fun onScreenLoad(): Flow<MSMovieResult> = flow { emit(MSMovieResult.ScreenLoadResult()) }

  private fun onLongRunningTask(event: LongRunningEvent): Flow<MSMovieResult> {
    return flow {
      delay(2000)
      emit(
          MSMovieResult.SearchMovieResult(
              movie =
                  MSMovie(
                      true,
                      title = "Batman (1989)",
                      posterUrl =
                          "data:image/jpeg;base64,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",
                  ),
          ),
      )
    }
  }

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

  override fun resultToViewState(
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
  // Results -> Effect

  override fun resultToEffects(result: MSMovieResult): Flow<MSMovieEffect?> = result.toEffects()
}
