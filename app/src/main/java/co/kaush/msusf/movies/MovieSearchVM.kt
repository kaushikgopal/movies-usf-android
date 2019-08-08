package co.kaush.msusf.movies

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.kaush.msusf.Lce
import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MovieSearchEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MovieSearchEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MovieSearchEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MovieSearchEvent.SearchMovieEvent
import co.kaush.msusf.movies.MovieSearchResult.AddToHistoryResult
import co.kaush.msusf.movies.MovieSearchResult.ScreenLoadResult
import co.kaush.msusf.movies.MovieSearchResult.SearchMovieResult
import co.kaush.msusf.movies.MovieSearchViewEffect.AddedToHistoryToastEffect
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * For this example, a simple ViewModel would have sufficed,
 * but in most real world examples we would use an AndroidViewModel
 *
 * Our Unit tests should still be able to run given this
 */
class MovieSearchVM(
    app: MSApp,
    private val movieRepo: MovieRepository
) : AndroidViewModel(app) {

    private val eventEmitter: PublishSubject<MovieSearchEvent> = PublishSubject.create()

    private lateinit var disposable: Disposable

    val viewState: Observable<MovieSearchViewState>
    val viewEffects: Observable<MovieSearchViewEffect>

    init {
        eventEmitter
            .doOnNext { Timber.d("----- event $it") }
            .eventToResult()
            .doOnNext { Timber.d("----- result $it") }
            .share()
            .also { result ->
                viewState = result
                    .resultToViewState()
                    .doOnNext { Timber.d("----- vs $it") }
                    .replay(1)
                    .autoConnect(1) { disposable = it }

                viewEffects = result
                    .resultToViewEffect()
                    .doOnNext { Timber.d("----- ve $it") }
            }
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }

    fun processInput(event: MovieSearchEvent) = eventEmitter.onNext(event)

    // -----------------------------------------------------------------------------------
    // Internal helpers

    private fun Observable<MovieSearchEvent>.eventToResult(): Observable<Lce<out MovieSearchResult>> {
        return publish { o ->
            Observable.merge(
                o.ofType(ScreenLoadEvent::class.java).onScreenLoad(),
                o.ofType(SearchMovieEvent::class.java).onSearchMovie(),
                o.ofType(AddToHistoryEvent::class.java).onAddToHistory(),
                o.ofType(RestoreFromHistoryEvent::class.java).onRestoreFromHistory()
            )
        }
    }

    private fun Observable<Lce<out MovieSearchResult>>.resultToViewState(): Observable<MovieSearchViewState> {
        return scan(MovieSearchViewState()) { vs, result ->
            when (result) {
                is Lce.Content -> {
                    when (result.packet) {
                        is ScreenLoadResult -> {
                            vs.copy(searchBoxText = "")
                        }
                        is SearchMovieResult -> {
                            val movie: MSMovie = result.packet.movie
                            vs.copy(
                                searchedMovieTitle = movie.title,
                                searchedMovieRating = movie.ratingSummary,
                                searchedMoviePoster = movie.posterUrl,
                                searchedMovieReference = movie
                            )
                        }

                        is AddToHistoryResult -> {
                            val movieToBeAdded: MSMovie = result.packet.movie

                            if (!vs.adapterList.contains(movieToBeAdded)) {
                                vs.copy(adapterList = vs.adapterList.plus(movieToBeAdded))
                            } else vs.copy()
                        }
                    }
                }

                is Lce.Loading -> {
                    vs.copy(
                        searchBoxText = null,
                        searchedMovieTitle = "Searching Movie...",
                        searchedMovieRating = "",
                        searchedMoviePoster = "",
                        searchedMovieReference = null
                    )
                }

                is Lce.Error -> {
                    when (result.packet) {
                        is SearchMovieResult -> {
                            val movie: MSMovie = result.packet.movie
                            vs.copy(searchedMovieTitle = movie.errorMessage!!)
                        }
                        else -> throw RuntimeException("Unexpected result LCE state")
                    }
                }
            }
        }
            .distinctUntilChanged()
    }

    private fun Observable<Lce<out MovieSearchResult>>.resultToViewEffect(): Observable<MovieSearchViewEffect> {
        return filter { it is Lce.Content && it.packet is AddToHistoryResult }
            .map<MovieSearchViewEffect> { AddedToHistoryToastEffect }
    }

    // -----------------------------------------------------------------------------------
    // use cases
    private fun Observable<ScreenLoadEvent>.onScreenLoad(): Observable<Lce<ScreenLoadResult>> {
        return map { Lce.Content(ScreenLoadResult) }
    }

    private fun Observable<SearchMovieEvent>.onSearchMovie(): Observable<Lce<SearchMovieResult>> {
        return switchMap { searchMovieEvent ->
            movieRepo.searchMovie(searchMovieEvent.searchedMovieTitle)
                .subscribeOn(Schedulers.io())
                .map {
                    if (it.errorMessage?.isNullOrBlank() == false) {
                        Lce.Error(SearchMovieResult(it))
                    } else {
                        Lce.Content(SearchMovieResult(it))
                    }
                }
                .onErrorReturn {
                    Lce.Error(SearchMovieResult(MSMovie(result = false, errorMessage = it.localizedMessage)))
                }
                .startWith(Lce.Loading())
        }
    }

    private fun Observable<AddToHistoryEvent>.onAddToHistory(): Observable<Lce<AddToHistoryResult>> {
        return map { Lce.Content(AddToHistoryResult(it.searchedMovie)) }
    }

    private fun Observable<RestoreFromHistoryEvent>.onRestoreFromHistory(): Observable<Lce<SearchMovieResult>> {
        return map { Lce.Content(SearchMovieResult(it.movieFromHistory)) }
    }

// -----------------------------------------------------------------------------------

    class MSMainVmFactory(
        private val app: MSApp,
        private val movieRepo: MovieRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MovieSearchVM(app, movieRepo) as T
        }
    }
}
