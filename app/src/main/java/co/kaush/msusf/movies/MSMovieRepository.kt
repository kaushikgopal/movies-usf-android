package co.kaush.msusf.movies

import com.google.gson.Gson
import javax.inject.Inject

@OpenClassOnDebug
class MSMovieRepository @Inject constructor(
    val movieApi: MSMovieApi
) {
  suspend fun searchMovie(movieName: String): MSMovie? {
    val response = movieApi.searchMovie(movieName)

    response.body()?.let { return it }

    return response.errorBody()?.let { body ->
      Gson().fromJson(
          body.string(),
          MSMovie::class.java,
      )
    }
  }
}
