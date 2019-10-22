package co.kaush.msusf.movies

import com.google.gson.Gson
import io.reactivex.Observable
import javax.inject.Inject

@OpenClassOnDebug
class MovieRepository @Inject constructor(
    val searchService: MovieSearchService
) {
    fun movieOnce(title: String): Observable<MovieSearchResult?> {
        return searchService.searchMovie(title)
            .map { response ->
                response.body()?.let { return@map it }
                response.errorBody()?.let { body ->
                    return@map Gson().fromJson(
                        body.string(),
                        MovieSearchResult::class.java
                    )
                }
            }
    }
}
