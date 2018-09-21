package co.kaush.msusf.movies

import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieResult.ScreenLoadResult
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import timber.log.Timber

class MSMainVm(
    app: MSApp,
    movieRepo: MSMovieRepository
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
                    }
                }

                else -> throw RuntimeException("Unexpected result LCE state")
            }
        }
    }

    private fun results(events: Observable<out MSMovieEvent>): Observable<Lce<out MSMovieResult>> {
        return events.publish { o ->
            Observable.merge(
                o.ofType(ScreenLoadEvent::class.java).compose(onScreenLoad()),
                Observable.never()
            )
        }
    }

    private fun onScreenLoad(): ObservableTransformer<ScreenLoadEvent, Lce<out MSMovieResult>> {
        return ObservableTransformer { upstream ->
            upstream.map { Lce.Content(ScreenLoadResult) }
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
