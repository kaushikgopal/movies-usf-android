package co.kaush.msusf.movies

import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import co.kaush.msusf.MSApp
import io.reactivex.Observable

class MSMainVm(
        app: MSApp,
        movieRepo: MSMovieRepository
) : AndroidViewModel(app) {

    fun send(vararg event: Observable<out MSMovieEvent>): Observable<MSMovieVs> {
        // event -> (action) result
        // result -> viewState

        return Observable.just(MSMovieVs())
    }


}

// -----------------------------------------------------------------------------------
// LCE

sealed class Lce {
    object Loading : Lce()
    data class Content<T>(val packet: T) : Lce()
    data class Error<T>(val packet: T? = null) : Lce()
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
