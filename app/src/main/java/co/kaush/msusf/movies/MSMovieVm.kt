package co.kaush.msusf.movies

import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieEvent.ClickMovieEvent
import co.kaush.msusf.movies.MSMovieEvent.ClickMovieFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.MSMovieResult.ClickMovieResult
import co.kaush.msusf.movies.MSMovieResult.ScreenLoadResult
import co.kaush.msusf.movies.MSMovieResult.SearchMovieResult
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MSMainVm(
    app: MSApp,
    private val movieRepo: MSMovieRepository
) : AndroidViewModel(app) {

    private var viewState: MSMovieVs = MSMovieVs()

    fun send(vararg es: Observable<out MSMovieEvent>): Observable<MSMovieVs> {

        val events: Observable<out MSMovieEvent> = Observable.mergeArray(*es)
            .doOnNext { Timber.d("----- event ${it.javaClass.simpleName}") }

        val results: Observable<Lce<out MSMovieResult>> = results(events)
            .doOnNext { Timber.d("----- result $it") }

        return render(results)
            .doOnNext { Timber.d("----- viewState $it") }
    }

    // -----------------------------------------------------------------------------------
    // Internal helpers

    private fun render(results: Observable<Lce<out MSMovieResult>>): Observable<MSMovieVs> {
        return results.scan(viewState) { state, result ->
            when (result) {

                is Lce.Content -> {
                    when (result.packet) {
                        is ScreenLoadResult -> state.copy(searchBoxText = "")

                        is SearchMovieResult -> {
                            val movie: MSMovie = result.packet.movie
                            state.copy(
                                searchedMovieTitle = movie.title,
                                searchedMovieRating = movie.ratingSummary,
                                searchedMoviePoster = movie.posterUrl,
                                searchedMovieReference = movie
                            )
                        }

                        is ClickMovieResult -> {
                            (result.packet.clickedMovie)
                                ?.let {
                                    val adapterList: MutableList<MSMovie> =
                                        mutableListOf(*state.adapterList.toTypedArray())
                                    adapterList.add(it)
                                    state.copy(adapterList = adapterList)
                                } ?: state.copy()
                        }
                    }
                }

                is Lce.Loading -> {
                    state.copy(
                        searchBoxText = null,
                        searchedMovieTitle = "Searching Movie",
                        searchedMovieRating = ""
                    )
                }

                else -> throw RuntimeException("Unexpected result LCE state")
            }
        }
            .doOnNext { viewState = it }
    }

    private fun results(events: Observable<out MSMovieEvent>): Observable<Lce<out MSMovieResult>> {
        return events.publish { o ->
            Observable.merge(
                o.ofType(ScreenLoadEvent::class.java).compose(onScreenLoad()),
                o.ofType(SearchMovieEvent::class.java).compose(onMovieSearch()),
                o.ofType(ClickMovieEvent::class.java).compose(onMovieSelect()),
                o.ofType(ClickMovieFromHistoryEvent::class.java).compose(onMovieFromHistorySelect())
            )
        }
    }

    // -----------------------------------------------------------------------------------
    // use cases

    private fun onScreenLoad(): ObservableTransformer<ScreenLoadEvent, Lce<ScreenLoadResult>> {
        return ObservableTransformer { upstream ->
            upstream.map { Lce.Content(ScreenLoadResult) }
        }
    }

    private fun onMovieSearch(): ObservableTransformer<SearchMovieEvent, Lce<SearchMovieResult>> {
        return ObservableTransformer { upstream ->
            upstream.switchMap { searchMovieEvent ->
                movieRepo.searchMovie(searchMovieEvent.searchedMovieTitle)
                    .subscribeOn(Schedulers.io())
                    .map { Lce.Content(SearchMovieResult(it)) as Lce<SearchMovieResult> }
                    .startWith(Lce.Loading())
            }
        }
    }

    private fun onMovieSelect(): ObservableTransformer<ClickMovieEvent, Lce<ClickMovieResult>> {
        return ObservableTransformer { upstream ->
            upstream.map {
                val movieResult: MSMovie = viewState.searchedMovieReference!!

                if (!viewState.adapterList.contains(movieResult)) {
                    Lce.Content(ClickMovieResult(movieResult))
                } else {
                    Lce.Content(ClickMovieResult(null))
                }
            }
        }
    }

    private fun onMovieFromHistorySelect(): ObservableTransformer<ClickMovieFromHistoryEvent,
        Lce<SearchMovieResult>> {
        return ObservableTransformer { upstream ->
            upstream.map { Lce.Content(SearchMovieResult(it.movieFromHistory)) }
        }
    }
}

// -----------------------------------------------------------------------------------
// LCE

sealed class Lce<T> {
    class Loading<T> : Lce<T>()
    data class Content<T>(val packet: T) : Lce<T>()
    data class Error<T>(val packet: T? = null) : Lce<T>()
}

// -----------------------------------------------------------------------------------

class MSMainVmFactory(
    private val app: MSApp,
    private val movieRepo: MSMovieRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MSMainVm(app, movieRepo) as T
    }
}
