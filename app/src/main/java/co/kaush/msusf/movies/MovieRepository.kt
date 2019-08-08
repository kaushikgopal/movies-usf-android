package co.kaush.msusf.movies

import com.google.gson.Gson
import io.reactivex.Observable
import javax.inject.Inject

@OpenClassOnDebug
class MovieRepository @Inject constructor(
    val movieSearchApi: MovieSearchApi
) {
    fun searchMovie(movieName: String): Observable<MSMovie?> {
        return movieSearchApi.searchMovie(movieName)
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
