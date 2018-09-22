package co.kaush.msusf.movies

import com.google.gson.Gson
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject

class MSMovieRepository @Inject constructor(
    val movieApi: MSMovieApi
) {
    fun searchMovie(movieName: String): Observable<MSMovie> {
        return movieApi.searchMovie(movieName)
            .doOnError { Timber.w("search Movie fail", it) }
            .map { response ->
                response.body()?.let { return@map it }

                response.errorBody()?.let { body ->
                    val errorResponse: MSMovie = Gson().fromJson(
                        body.string(),
                        MSMovie::class.java
                    )

                    return@map errorResponse
                }
            }
    }
}
