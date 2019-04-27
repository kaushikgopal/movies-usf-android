package co.kaush.msusf.movies

import co.kaush.msusf.BuildConfig
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import io.reactivex.schedulers.Schedulers

/**
 * For this example, a simple ViewModel would have sufficed,
 * but in most real world examples we would use an AndroidViewModel
 *
 * Our Unit tests should still be able to run given this
 */
class MSMainVm(
    initialState: MSMovieViewState,
    private val movieRepo: MSMovieRepository,
    debugMode: Boolean = BuildConfig.DEBUG
) : BaseMvRxViewModel<MSMovieViewState>(initialState, debugMode) {

    init {
        screenLoad()
    }

    private fun screenLoad() {
        setState { copy(searchBoxText = "") }
    }

    fun searchMovie(searchedMovieTitle: String) {
        movieRepo.searchMovie(searchedMovieTitle)
            .map {
                if (!it.errorMessage.isNullOrBlank()) {
                    throw Exception(it.errorMessage)
                } else {
                    it
                }
            }
            .subscribeOn(Schedulers.io())
            .execute { copy(searchBoxText = searchedMovieTitle, searchMovieRequest = it) }
    }

    fun addToHistory() {
        setState {
            val movieResult = searchMovieRequest.invoke()!!

            return@setState if (adapterList.contains(movieResult)) {
                copy(addedToHistory = true)
            } else {
                copy(adapterList = adapterList.plus(movieResult), addedToHistory = true)
            }
        }
        setState { copy(addedToHistory = false) }
    }

    fun restoreFromHistory(movieFromHistory: MSMovie) {
        setState {
            copy(
                searchBoxText = movieFromHistory.title,
                searchMovieRequest = Success(movieFromHistory)
            )
        }
    }

    companion object : MvRxViewModelFactory<MSMainVm, MSMovieViewState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: MSMovieViewState
        ): MSMainVm? {
            return MSMainVm(state, (viewModelContext.activity as MSMovieActivity).movieRepo)
        }
    }
}
