package co.kaush.msusf.movies

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

sealed class MSMovieResult {
  abstract val loading: Boolean
  abstract val errorMessage: String
  abstract fun toViewState(currentViewState: MSMovieViewState): MSMovieViewState
  abstract fun toViewEffect(): Flow<MSMovieViewEffect>

  data class ScreenLoadResult(
      override val loading: Boolean = false,
      override val errorMessage: String = "",
  ) : MSMovieResult() {
    override fun toViewState(currentViewState: MSMovieViewState): MSMovieViewState =
        currentViewState.copy(searchBoxText = "load test")

    override fun toViewEffect(): Flow<MSMovieViewEffect> = emptyFlow()
  }

  data class SearchMovieResult(
      override val loading: Boolean = false,
      override val errorMessage: String = "",
      val movie: MSMovie? = null,
  ) : MSMovieResult() {
    override fun toViewState(currentViewState: MSMovieViewState): MSMovieViewState {
      val movie: MSMovie = movie!!

      return currentViewState.copy(
          searchBoxText = movie.title,
          searchedMovieTitle = movie.title,
          searchedMovieRating = movie.ratingSummary,
          searchedMoviePoster = movie.posterUrl,
          searchedMovieReference = movie,
      )
    }

    override fun toViewEffect(): Flow<MSMovieViewEffect> = emptyFlow()
  }

  data class AddToHistoryResult(
      override val loading: Boolean = false,
      override val errorMessage: String = "",
      val movie: MSMovie? = null,
  ) : MSMovieResult() {
    override fun toViewState(currentViewState: MSMovieViewState): MSMovieViewState {
      val movie: MSMovie = movie!!

      return if (!currentViewState.adapterList.contains(movie)) {
        currentViewState.copy(adapterList = currentViewState.adapterList.plus(movie))
      } else currentViewState.copy()
    }

    override fun toViewEffect(): Flow<MSMovieViewEffect> {
      return flow { emit(MSMovieViewEffect.AddedToHistoryToastEffect) }
    }
  }
}
