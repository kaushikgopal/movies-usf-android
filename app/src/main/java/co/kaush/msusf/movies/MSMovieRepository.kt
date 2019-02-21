package co.kaush.msusf.movies

import com.google.gson.Gson
import io.reactivex.Observable
import javax.inject.Inject

@OpenClassOnDebug
class MSMovieRepository @Inject constructor(
    val movieApi: MSMovieApi
) {
    fun searchMovie(movieName: String): Observable<MSMovie> {
        return movieApi.searchMovie(movieName)
            .map { response ->
                response.body()?.let { return@map it }
                response.errorBody()?.let { body ->
                    return@map Gson().fromJson(
                        body.string(),
                        MSMovie::class.java
                    )
                }
            }
    }
}
