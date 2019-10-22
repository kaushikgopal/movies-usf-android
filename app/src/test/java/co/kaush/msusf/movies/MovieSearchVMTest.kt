package co.kaush.msusf.movies

import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.search.MSMovieEvent.*
import co.kaush.msusf.movies.search.MSMovieViewEffect.AddedToHistoryToastEffect
import co.kaush.msusf.movies.search.MovieSearchVM
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.TimeUnit

class MovieSearchVMTest {

    private lateinit var viewModel: MovieSearchVM

    @Test
    fun onSubscribing_shouldReceiveStartingviewState() {
        viewModel = MovieSearchVM(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState.test()
        viewStateTester.assertValueCount(1)
    }

    @Test
    fun onScreenLoad_searchBoxText_shouldBeCleared() {
        viewModel = MovieSearchVM(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState.test()

        viewModel.processInput(ViewResumeEvent)

        viewStateTester.assertValueAt(1) {
            assertThat(it.searchBoxText).isEqualTo("")
            true
        }
    }

    @Test
    fun onSearchingMovie_shouldSeeSearchResults() {
        viewModel = MovieSearchVM(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState.test()

        viewModel.processInput(SearchMovieEvent("blade runner 2049"))

        viewStateTester.assertValueAt(1) {
            assertThat(it.movieTitle).isEqualTo("Searching Movie...")
            true
        }

        viewStateTester.assertValueAt(2) {
            assertThat(it.movieTitle).isEqualTo("Blade Runner 2049")
            assertThat(it.moviePosterUrl)
                .isEqualTo("https://m.media-amazon.com/images/M/MV5BNzA1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_SX300.jpg")
            assertThat(it.rating1).isEqualTo("\n8.1/10 (IMDB)\n87% (RT)")

            true
        }
    }

    @Test
    fun onClickingMovieSearchResult_shouldPopulateHistoryList() {
        viewModel = MovieSearchVM(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.processInput(SearchMovieEvent("blade runner 2049"))
        viewModel.processInput(AddToHistoryEvent(bladeRunner2049))

        viewStateTester.assertValueAt(3) {
            assertThat(it.searchBoxText).isEqualTo(null) // prevents search box from reset
            assertThat(it.adapterList).hasSize(1)
            assertThat(it.adapterList[0]).isEqualTo(bladeRunner2049)
            true
        }

        viewEffectTester.assertValueAt(0) { it is AddedToHistoryToastEffect }
    }

    @Test
    fun onClickingMovieSearchResultTwice_shouldShowToastEachTime() {
        viewModel = MovieSearchVM(mockApp, mockMovieRepo)

        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.processInput(SearchMovieEvent("blade runner 2049"))
        viewEffectTester.awaitTerminalEvent(20L, TimeUnit.MILLISECONDS)

        viewModel.processInput(AddToHistoryEvent(bladeRunner2049))
        viewModel.processInput(AddToHistoryEvent(bladeRunner2049))

        viewEffectTester.assertValueCount(2)
        viewEffectTester.assertValueAt(0) { it is AddedToHistoryToastEffect }
        viewEffectTester.assertValueAt(1) { it is AddedToHistoryToastEffect }
    }

    @Test
    fun onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo() {
        viewModel = MovieSearchVM(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState.test()

        // populate history
        viewModel.processInput(SearchMovieEvent("blade runner 2049"))
        viewStateTester.awaitTerminalEvent(20L, TimeUnit.MILLISECONDS)
//        viewModel.processInput(AddToHistoryEvent)
        viewModel.processInput(SearchMovieEvent("blade"))
        viewStateTester.awaitTerminalEvent(20L, TimeUnit.MILLISECONDS)
//        viewModel.processInput(AddToHistoryEvent)

        // check that the result is showing Blade
        viewStateTester.assertValueAt(viewStateTester.valueCount() - 1) {
            assertThat(it.movieTitle).isEqualTo("Blade")
            true
        }

        // click blade runner 2049 from history
        viewModel.processInput(RestoreFromHistoryEvent(bladeRunner2049))
        viewStateTester.assertValueAt(viewStateTester.valueCount() - 1) {
            assertThat(it.movieTitle).isEqualTo("Blade Runner 2049")
            assertThat(it.rating1).isEqualTo(bladeRunner2049.ratingSummary)
            true
        }

        // click blade again
        viewModel.processInput(RestoreFromHistoryEvent(blade))
        viewStateTester.assertValueAt(viewStateTester.valueCount() - 1) {
            assertThat(it.movieTitle).isEqualTo("Blade")
            assertThat(it.rating1).isEqualTo(blade.ratingSummary)
            true
        }
    }

    private val mockApp: MSApp by lazy { mock(MSApp::class.java) }

    private val mockMovieRepo: MovieRepository by lazy {
        mock(MovieRepository::class.java).apply {
            whenever(movieOnce("blade runner 2049"))
                .thenReturn(Observable.just(bladeRunner2049))
            whenever(movieOnce("blade"))
                .thenReturn(Observable.just(blade))
        }
    }

    private val bladeRunner2049 by lazy {
        val ratingImdb = MSRating(
            source = "Internet Movie Database",
            rating = "8.1/10"
        )

        val ratingRottenTomatoes = MSRating(
            source = "Rotten Tomatoes",
            rating = "87%"
        )

        MovieSearchResult(
            result = true,
            errorMessage = null,
            title = "Blade Runner 2049",
            ratings = listOf(ratingImdb, ratingRottenTomatoes),
            posterUrl = "https://m.media-amazon.com/images/M/MV5BNzA1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_SX300.jpg"
        )
    }

    private val blade by lazy {
        val ratingImdb = MSRating(
            source = "Internet Movie Database",
            rating = "7.1/10"
        )

        val ratingRottenTomatoes = MSRating(
            source = "Rotten Tomatoes",
            rating = "54%"
        )

        MovieSearchResult(
            result = true,
            errorMessage = null,
            title = "Blade",
            ratings = listOf(ratingImdb, ratingRottenTomatoes),
            posterUrl = "https://m.media-amazon.com/images/M/MV5BMTQ4MzkzNjcxNV5BMl5BanBnXkFtZTcwNzk4NTU0Mg@@._V1_SX300.jpg"
        )
    }
}