package co.kaush.msusf.movies

data class MSMovieViewState(
    val searchBoxText: String = "Blade",
    val searchedMovieTitle: String = "",
    val searchedMovieRating: String = "",
    val searchedMoviePoster: String = "",
    val searchedMovieReference: MSMovie? = null,
    val adapterList: List<MSMovie> = emptyList()
)

sealed class MSMovieViewEffect {
  object AddedToHistoryToastEffect : MSMovieViewEffect()
}

sealed class MSMovieEvent {
  object ScreenLoadEvent : MSMovieEvent()
  object ScreenLoadEvent2 : MSMovieEvent()
  data class SearchMovieEvent(val searchedMovieTitle: String = "") : MSMovieEvent()
  data class AddToHistoryEvent(val searchedMovie: MSMovie) : MSMovieEvent()
  data class RestoreFromHistoryEvent(val movieFromHistory: MSMovie) : MSMovieEvent()
}
