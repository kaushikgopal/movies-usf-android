package co.kaush.msusf.movies

data class MSMovieViewState(
    val searchBoxText: String? = null,
    val searchedMovieTitle: String = "",
    val searchedMovieRating: String = "",
    val searchedMoviePoster: String = "",
    val searchedMovieReference: MSMovie? = null,
    val adapterList: List<MSMovie> = emptyList()
)

sealed class MSMovieViewEffect {
    object AddedToHistoryToastEffect: MSMovieViewEffect()
}

data class MSMovieViewChange(val vs: MSMovieViewState, var effects: List<MSMovieViewEffect> = emptyList())

sealed class MSMovieEvent {
    object ScreenLoadEvent : MSMovieEvent()
    data class SearchMovieEvent(val searchedMovieTitle: String = "") : MSMovieEvent()
    object AddToHistoryEvent : MSMovieEvent()
    data class RestoreFromHistoryEvent(val movieFromHistory: MSMovie) : MSMovieEvent()
}

sealed class MSMovieResult {
    object ScreenLoadResult : MSMovieResult()
    data class SearchMovieResult(val movie: MSMovie) : MSMovieResult()
    data class SearchHistoryResult(val movieHistory: MSMovie?) : MSMovieResult()
}