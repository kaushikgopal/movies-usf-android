package co.kaush.msusf.movies

data class MovieSearchViewState(
    val searchBoxText: String? = null,
    val searchedMovieTitle: String = "",
    val searchedMovieRating: String = "",
    val searchedMoviePoster: String = "",
    val searchedMovieReference: MSMovie? = null,
    val adapterList: List<MSMovie> = emptyList()
)

sealed class MovieSearchViewEffect {
    object AddedToHistoryToastEffect: MovieSearchViewEffect()
}

sealed class MovieSearchEvent {
    object ScreenLoadEvent : MovieSearchEvent()
    data class SearchMovieEvent(val searchedMovieTitle: String = "") : MovieSearchEvent()
    data class AddToHistoryEvent(val searchedMovie: MSMovie) : MovieSearchEvent()
    data class RestoreFromHistoryEvent(val movieFromHistory: MSMovie) : MovieSearchEvent()
}

sealed class MovieSearchResult {
    object ScreenLoadResult : MovieSearchResult()
    data class SearchMovieResult(val movie: MSMovie) : MovieSearchResult()
    data class AddToHistoryResult(val movie: MSMovie) : MovieSearchResult()
}