package co.kaush.msusf.movies

import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.MSMovieResult.ScreenLoadResult
import co.kaush.msusf.movies.MSMovieResult.SearchHistoryResult
import co.kaush.msusf.movies.MSMovieResult.SearchMovieResult
import co.kaush.msusf.movies.MSMovieViewEffect.*
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.IllegalStateException

/**
 * For this example, a simple ViewModel would have sufficed,
 * but in most real world examples we would use an AndroidViewModel
 *
 * Our Unit tests should still be able to run given this
 */
class MSMainVm(
    app: MSApp,
    private val movieRepo: MSMovieRepository
) : AndroidViewModel(app) {

    private var viewModelDisposable: Disposable? = null
    private val eventEmitter: PublishSubject<MSMovieEvent> = PublishSubject.create()
    private val viewState: BehaviorSubject<MSMovieViewState> = BehaviorSubject.create()
    private val viewEffects: PublishSubject<MSMovieViewEffect> = PublishSubject.create()

    init {
        val viewChangeSource = eventEmitter
            .doOnNext { Timber.d("----- event ${it.javaClass.simpleName}") }
            .compose(eventToResult())
            .doOnNext { Timber.d("----- result $it") }
            .publish()

        viewChangeSource.compose(resultToViewState()).subscribe(viewState)
        viewChangeSource.compose(resultToViewEffect()).subscribe(viewEffects)

        viewChangeSource.autoConnect(0) { viewModelDisposable = it }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelDisposable?.dispose()
    }

    fun processInputs(vararg es: Observable<out MSMovieEvent>) {
        return Observable.mergeArray(*es).subscribe(eventEmitter)
    }

    fun viewState(): Observable<MSMovieViewState> = viewState

    fun viewEffects(): Observable<MSMovieViewEffect> = viewEffects

    // -----------------------------------------------------------------------------------
    // Internal helpers

    private fun eventToResult(
    ): ObservableTransformer<MSMovieEvent, Lce<out MSMovieResult>> {
        return ObservableTransformer { upstream ->
            upstream.publish { o ->
                Observable.merge(
                    o.ofType(ScreenLoadEvent::class.java).compose(onScreenLoad()),
                    o.ofType(SearchMovieEvent::class.java).compose(onSearchMovie()),
                    o.ofType(AddToHistoryEvent::class.java).compose(onAddToHistory()),
                    o.ofType(RestoreFromHistoryEvent::class.java).compose(onRestoreFromHistory())
                )
            }
        }
    }

    private fun resultToViewState(): ObservableTransformer<Lce<out MSMovieResult>, out MSMovieViewState> {
        return ObservableTransformer { upstream ->
            upstream.scan(viewState.value ?: MSMovieViewState()) { vs, result ->
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

                            is SearchHistoryResult -> {
                                result.packet.movieHistory
                                    ?.let {
                                        val adapterList: MutableList<MSMovie> =
                                            mutableListOf(*vs.adapterList.toTypedArray())
                                        adapterList.add(it)
                                        vs.copy(adapterList = adapterList)
                                    } ?: vs.copy()
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
    }

    private fun resultToViewEffect(): ObservableTransformer<Lce<out MSMovieResult>, MSMovieViewEffect> {
        return ObservableTransformer { upstream ->
            upstream
                .filter { it is Lce.Content && it.packet is SearchHistoryResult }
                .map<MSMovieViewEffect> { AddedToHistoryToastEffect }
        }
    }

    // -----------------------------------------------------------------------------------
    // use cases

    private fun onScreenLoad(): ObservableTransformer<ScreenLoadEvent, Lce<ScreenLoadResult>> {
        return ObservableTransformer { upstream ->
            upstream.map { Lce.Content(ScreenLoadResult) }
        }
    }

    private fun onSearchMovie(): ObservableTransformer<SearchMovieEvent, Lce<SearchMovieResult>> {
        return ObservableTransformer { upstream ->
            upstream.switchMap { searchMovieEvent ->
                movieRepo.searchMovie(searchMovieEvent.searchedMovieTitle)
                    .subscribeOn(Schedulers.io())
                    .map {
                        if (it.errorMessage?.isNullOrBlank() == false) {
                            Lce.Error(SearchMovieResult(it))
                        } else {
                            Lce.Content(SearchMovieResult(it))
                        }
                    }
                    .startWith(Lce.Loading())
            }
        }
    }

    private fun onAddToHistory(): ObservableTransformer<AddToHistoryEvent, Lce<SearchHistoryResult>> {
        return ObservableTransformer { upstream ->
            upstream.map {
                val movieResult: MSMovie = viewState.value?.searchedMovieReference
                    ?: throw IllegalStateException("couldn't find searched movie reference")

                val adapterList: List<MSMovie> = viewState.value?.adapterList
                    ?: throw IllegalStateException("couldn't find movie history")

                if (!adapterList.contains(movieResult)) {
                    Lce.Content(SearchHistoryResult(movieResult))
                } else {
                    Lce.Content(SearchHistoryResult(null))
                }
            }
        }
    }

    private fun onRestoreFromHistory(): ObservableTransformer<RestoreFromHistoryEvent,
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
    data class Error<T>(val packet: T) : Lce<T>()
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
