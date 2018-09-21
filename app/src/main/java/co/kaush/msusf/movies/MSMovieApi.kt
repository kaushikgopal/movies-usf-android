package co.kaush.msusf.movies

import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MSMovieApi {

    @GET("/")
    fun searchMovie(
            @Query("t") movieName: String,
            @Query("apiKey") apiKey: String = ""
    ): Observable<Response<MSMovieResult>>
}

data class MSMovieResult(
        @SerializedName("Result") val result: Boolean,
        @SerializedName("Error") val errorMessage: String = "",
        @SerializedName("Title") val title: String = "",
        @SerializedName("Poster") val posterUrl: String = "",
        @SerializedName("imdbRating") val rating: Float? = null,
        @SerializedName("Ratings") val ratings: List<MSRating> = emptyList()
)

data class MSRating(
        @SerializedName("Source") val source: String,
        @SerializedName("Value") val rating: String
)