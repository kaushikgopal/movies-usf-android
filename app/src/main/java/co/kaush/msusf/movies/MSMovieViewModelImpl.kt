package co.kaush.msusf.movies

import co.kaush.msusf.annotations.UsfViewModel
import co.kaush.msusf.usf.UsfViewModelImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

@UsfViewModel
class MSMovieViewModelImpl(
        private val movieRepo: MSMovieRepository,
        private val longRunningFlow: MSUselessRepository,
        coroutineScope: CoroutineScope,
) :
        UsfViewModelImpl<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect>(
                MSMovieViewState(),
                coroutineScope,
        ) {

    init {
        setup(emptyList())
    }
    // -----------------------------------------------------------------------------------
    // Event -> Results

    override fun Flow<MSMovieEvent>.toResultFlow(): Flow<MSMovieResult> =
            merge(
                    filterIsInstance<MSMovieEvent.ScreenLoadEvent>().onScreenLoad(),
                    filterIsInstance<MSMovieEvent.ScreenLoadEvent2>().onScreenLoad2(),
                    filterIsInstance<MSMovieEvent.SearchMovieEvent>().onSearchMovie(),
                    filterIsInstance<MSMovieEvent.AddToHistoryEvent>().onAddToHistory(),
                    filterIsInstance<MSMovieEvent.RestoreFromHistoryEvent>().onRestoreFromHistory(),
            )

    private fun Flow<MSMovieEvent.ScreenLoadEvent>.onScreenLoad() = map {
        MSMovieResult.ScreenLoadResult()
    }

    private fun Flow<MSMovieEvent.ScreenLoadEvent2>.onScreenLoad2() = longRunningFlow.start().onEach {
        println("${System.currentTimeMillis()} post longRunningFlow")
    }


    private fun Flow<MSMovieEvent.SearchMovieEvent>.onSearchMovie(): Flow<MSMovieResult> =
            flatMapLatest {
                flowOf(it.searchedMovieTitle)
                        .map {
                            try {
                                val movie = movieRepo.searchMovie(it)
                                MSMovieResult.SearchMovieResult(
                                        movie = movie,
                                        errorMessage = movie?.errorMessage ?: "",
                                )
                            } catch (e: Exception) {
                                MSMovieResult.SearchMovieResult(
                                        movie = MSMovie(result = false, errorMessage = e.localizedMessage),
                                        errorMessage = e.localizedMessage ?: "",
                                )
                            }
                        }
                        .onStart { emit(MSMovieResult.SearchMovieResult(loading = true)) }
            }

    private fun Flow<MSMovieEvent.AddToHistoryEvent>.onAddToHistory() = map {
        MSMovieResult.AddToHistoryResult(movie = it.searchedMovie)
    }

    private fun Flow<MSMovieEvent.RestoreFromHistoryEvent>.onRestoreFromHistory() = map {
        MSMovieResult.SearchMovieResult(movie = it.movieFromHistory)
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
