package co.kaush.msusf.movies

import co.kaush.msusf.di.AppScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
  fun initialMovie(): StateFlow<MSMovie>  = MutableStateFlow(blade).asStateFlow()

  private val blade by lazy {
    val ratingImdb =
        MSRating(
            source = "Internet Movie Database",
            rating = "7.1/10",
        )

    val ratingRottenTomatoes =
        MSRating(
            source = "Rotten Tomatoes",
            rating = "54%",
        )

    MSMovie(
        result = true,
        errorMessage = null,
        title = "Blade",
        ratings = listOf(ratingImdb, ratingRottenTomatoes),
        posterUrl =
        "https://m.media-amazon.com/images/M/MV5BMTQ4MzkzNjcxNV5BMl5BanBnXkFtZTcwNzk4NTU0Mg@@._V1_SX300.jpg",
    )
  }
}
