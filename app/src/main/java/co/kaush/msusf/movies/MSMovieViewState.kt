package co.kaush.msusf.movies

data class MSMovieViewState(
    val searchBoxText: String? = null,
    val searchedMovieTitle: String = "",
    val searchedMovieRating: String = "",
    val searchedMoviePoster: String = "",
    val searchedMovieReference: MSMovie? = null,
    val adapterList: List<MSMovie> = emptyList()
)

sealed class MSMovieViewEffect { object AddedToHistoryToastEffect : MSMovieViewEffect()
}

sealed class MSMovieEvent { object ScreenLoadEvent : MSMovieEvent()
    data class SearchMovieEvent(val searchedMovieTitle: String = "") : MSMovieEvent()
    data class AddToHistoryEvent(val searchedMovie: MSMovie) : MSMovieEvent()
    data class RestoreFromHistoryEvent(val movieFromHistory: MSMovie) : MSMovieEvent()
}

sealed class MSMovieResult {
    abstract val loading: Boolean
    abstract val errorMessage: String

    data class ScreenLoadResult(
        override val loading: Boolean = false,
        override val errorMessage: String = "",
    ) : MSMovieResult()

    data class SearchMovieResult(
        override val loading: Boolean = false,
        override val errorMessage: String = "",
        val movie: MSMovie? = null,
    ) : MSMovieResult()

    data class AddToHistoryResult(
        override val loading: Boolean = false,
        override val errorMessage: String = "",
        val movie: MSMovie? = null,
    ) : MSMovieResult()
}