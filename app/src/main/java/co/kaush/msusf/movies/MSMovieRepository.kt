package co.kaush.msusf.movies

import com.google.gson.Gson
import io.reactivex.Observable
import javax.inject.Inject

class MSMovieRepository @Inject constructor(
        val movieApi: MSMovieApi
) {
    fun searchMovie(movieName: String): Observable<MSMovie> {
        return movieApi.searchMovie(movieName)
                .map {
                    it.body()?.let { return@map it }

                    it.errorBody()?.let { body ->
                        val errorResponse: MSMovie = Gson().fromJson(
                                body.string(),
                                MSMovie::class.java
                        )

                        errorResponse
                    }
                }
    }
}
