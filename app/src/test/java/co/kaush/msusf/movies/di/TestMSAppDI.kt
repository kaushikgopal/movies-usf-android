package co.kaush.msusf.movies.di

import co.kaush.msusf.di.AppScope
import co.kaush.msusf.di.CommonAppComponent
import co.kaush.msusf.movies.MSMovie
import co.kaush.msusf.movies.MSMovieRepository
import co.kaush.msusf.movies.MSRating
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal val bladeRunner2049 by lazy {
  val ratingImdb =
      MSRating(
          source = "Internet Movie Database",
          rating = "8.1/10",
      )

  val ratingRottenTomatoes =
      MSRating(
          source = "Rotten Tomatoes",
          rating = "87%",
      )

  MSMovie(
      result = true,
      errorMessage = null,
      title = "Blade Runner 2049",
      ratings = listOf(ratingImdb, ratingRottenTomatoes),
      posterUrl =
          "https://m.media-amazon.com/images/M/MV5BNzA1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_SX300.jpg",
  )
}

internal val blade by lazy {
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

internal val movieNotFound by lazy {
  val ratingImdb =
      MSRating(
          source = "Internet Movie Database",
          rating = "8.1/10",
      )

  val ratingRottenTomatoes =
      MSRating(
          source = "Rotten Tomatoes",
          rating = "87%",
      )

  MSMovie(
      result = true,
      errorMessage = null,
      title = "Blade Runner 2049",
      ratings = listOf(ratingImdb, ratingRottenTomatoes),
      posterUrl =
          "https://m.media-amazon.com/images/M/MV5BNzA1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_SX300.jpg",
  )
}

class TestFakes {

  @Provides
  fun provideFakeMovieRepository(): MSMovieRepository {
    return mock<MSMovieRepository> {
      on { runBlocking { searchMovie("blade runner 2049") } } doReturn bladeRunner2049

      on { runBlocking { searchMovie("blade") } } doReturn blade
    }
  }
}

@AppScope
@Component
abstract class TestAppComponent(@Component val fakes: TestFakes = TestFakes()) :
    CommonAppComponent()
