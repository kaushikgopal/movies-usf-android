package co.kaush.msusf.movies

data class MSMovieVs(
    val searchBoxText: String = "",
    val searchedMovieTitle: String = "",
    val searchedMovieRating: String = "",
    val searchedMoviePoster: String = ""
)

sealed class MSMovieEvent {

    object ScreenLoadEvent: MSMovieEvent()

}

sealed class MSMovieResult {
    object ScreenLoadResult: MSMovieResult()
}