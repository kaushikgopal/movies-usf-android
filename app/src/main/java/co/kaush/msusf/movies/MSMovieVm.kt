package co.kaush.msusf.movies

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers

typealias MSUsfVM = UsfVm<MSMovieEvent, MSMovieResult, MSMovieViewState, MSMovieViewEffect>

/**
 * For this example, a simple ViewModel would have sufficed,
 * but in most real world examples we would use an AndroidViewModel
 *
 * Our Unit tests should still be able to run given this
 */
class MSMainVm(
    app: MSApp,
    private val movieRepo: MSMovieRepository,
    private val vmImpl: MSUsfVM = MSMainVmImpl(movieRepo).usfVmImpl
) : AndroidViewModel(app), MSUsfVM by vmImpl  {

    override fun onCleared() {
        super.onCleared()
        clear()
    }

    class MSMainVmFactory(
        private val app: MSApp,
        private val movieRepo: MSMovieRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MSMainVm(app, movieRepo) as T
        }
    }
}

class MSMainVmImpl(
    private val movieRepo: MSMovieRepository,
) {
    val usfVmImpl: MSUsfVM = UsfVmImpl(
        eventToResultTransformer = e2R,
        resultToViewStateTransformer = r2VS,
        resultToViewEffectTransformer = r2VE
    )


    // -----------------------------------------------------------------------------------
    // Event -> Results

    private val e2R: ObservableTransformer<MSMovieEvent, MSMovieResult>
        get() = ObservableTransformer { upstream: Observable<MSMovieEvent> ->
            upstream.publish { downstream: Observable<MSMovieEvent> ->
                Observable.merge(
                    downstream.ofType(ScreenLoadEvent::class.java).onScreenLoad(),
                    downstream.ofType(SearchMovieEvent::class.java).onSearchMovie(),
                    downstream.ofType(AddToHistoryEvent::class.java).onAddToHistory(),
                    downstream.ofType(RestoreFromHistoryEvent::class.java).onRestoreFromHistory()
                )
            }
        }

    private fun Observable<ScreenLoadEvent>.onScreenLoad(): Observable<ScreenLoadResult> {
        return map { ScreenLoadResult() }
    }

    private fun Observable<SearchMovieEvent>.onSearchMovie(): Observable<SearchMovieResult> {
        return switchMap { searchMovieEvent ->

            movieRepo.searchMovie(searchMovieEvent.searchedMovieTitle)
                .subscribeOn(Schedulers.io())
                .map {
                    SearchMovieResult(
                        movie = it,
                        errorMessage = it.errorMessage ?: "",
                    )
                }
                .onErrorReturn {
                    SearchMovieResult(
                        movie = MSMovie(result = false, errorMessage = it.localizedMessage),
                        errorMessage = it.localizedMessage ?: "",
                    )
                }
                .startWith(SearchMovieResult(loading = true))
        }
    }

    private fun Observable<AddToHistoryEvent>.onAddToHistory(): Observable<AddToHistoryResult> {
        return map { AddToHistoryResult(movie = it.searchedMovie) }
    }

    private fun Observable<RestoreFromHistoryEvent>.onRestoreFromHistory(): Observable<SearchMovieResult> {
        return map { SearchMovieResult(movie = it.movieFromHistory) }
    }

    // -----------------------------------------------------------------------------------
    // Results -> ViewState

    private val r2VS: ObservableTransformer<MSMovieResult,MSMovieViewState>
        get() = ObservableTransformer { upstream: Observable<MSMovieResult> ->
            upstream.scan(MSMovieViewState()) { vs, result ->
           when {
                result.loading -> {
                    vs.copy(
                        searchBoxText = null,
                        searchedMovieTitle = "Searching Movie...",
                        searchedMovieRating = "",
                        searchedMoviePoster = "",
                        searchedMovieReference = null
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
                        searchedMovieTitle = movie.title,
                        searchedMovieRating = movie.ratingSummary,
                        searchedMoviePoster = movie.posterUrl,
                        searchedMovieReference = movie
                    )
                }

                result is AddToHistoryResult -> {
                    val movie: MSMovie = result.movie!!

                    if (!vs.adapterList.contains(movie)) {
                        vs.copy(adapterList = vs.adapterList.plus(movie))
                    } else vs.copy()
                }

                else -> throw RuntimeException("Unexpected state")
            }
        }
        }


    // -----------------------------------------------------------------------------------
    // Results -> ViewEffect

    private val r2VE: ObservableTransformer<MSMovieResult,MSMovieViewEffect>
        get() = ObservableTransformer { upstream: Observable<MSMovieResult> ->
            upstream.filter { it is AddToHistoryResult }
            .map { AddedToHistoryToastEffect }
        }

}
