package co.kaush.msusf.movies

import co.kaush.msusf.BuildConfig
import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MovieSearchApi {

    @GET("/")
    fun searchMovie(
            @Query("t") movieName: String,
            @Query("apiKey") apiKey: String = BuildConfig.OMDB_API_KEY
    ): Observable<Response<MSMovie>>
}

data class MSMovie(
    @SerializedName("Result") val result: Boolean,
    @SerializedName("Error") val errorMessage: String? = null,
    @SerializedName("Title") val title: String = "",
    @SerializedName("Poster") val posterUrl: String = "",
    @SerializedName("Ratings") val ratings: List<MSRating> = emptyList()
) {
    val ratingSummary: String
        get() {
            return ratings.fold("") { summary, msRating -> "$summary\n${msRating.summary}" }
        }
}

data class MSRating(
    @SerializedName("Source") val source: String,
    @SerializedName("Value") val rating: String
) {

    val summary: String get() = "$rating (${sourceShortName(source)})"

    private fun sourceShortName(ratingSource: String): String {
        return when {
            ratingSource.contains("Internet Movie Database") -> "IMDB"
            ratingSource.contains("Rotten Tomatoes") -> "RT"
            ratingSource.contains("Metacritic") -> "Metac"
            else -> ratingSource
        }
    }
}