package co.kaush.msusf.movies

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized

data class MSMovieViewState(
    val searchBoxText: String? = null,
    val searchMovieRequest: Async<MSMovie> = Uninitialized,
    val adapterList: List<MSMovie> = emptyList(),
    val addedToHistory: Boolean = false
) : MvRxState
