package co.kaush.msusf.movies

import co.kaush.msusf.di.AppScope
import com.google.gson.Gson
import me.tatarka.inject.annotations.Inject

class MSMovieRepository @AppScope @Inject constructor(val movieApi: MSMovieApi) {
  suspend fun searchMovie(movieName: String): MSMovie? {
    val response = movieApi.searchMovie(movieName)

    response.body()?.let {
      return it
    }

    return response.errorBody()?.let { body ->
      Gson()
          .fromJson(
              body.string(),
              MSMovie::class.java,
          )
    }
  }
}
