package co.kaush.msusf.movies

import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.MSMovieViewEffect.*
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.TimeUnit

class MSMainVmTest {

    private lateinit var viewModel: MSMainVm

    @Test
    fun onSubscribing_shouldReceiveStartingviewState() {
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState().test()
        viewStateTester.assertValueCount(1)
    }

    @Test
    fun onScreenLoad_searchBoxText_shouldBeCleared() {
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState().test()

        viewModel.processInput(ScreenLoadEvent)

        viewStateTester.assertValueAt(1) {
            assertThat(it.searchBoxText).isEqualTo("load test")
            true
        }
    }

    @Test
    fun onSearchingMovie_shouldSeeSearchResults() {
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState().test()

        viewModel.processInput(SearchMovieEvent("blade runner 2049"))

        viewStateTester.assertValueAt(1) {
            assertThat(it.searchedMovieTitle).isEqualTo("Searching Movie...")
            true
        }

        viewStateTester.assertValueAt(2) {
            assertThat(it.searchedMovieTitle).isEqualTo("Blade Runner 2049")
            assertThat(it.searchedMoviePoster)
                .isEqualTo("https://m.media-amazon.com/images/M/MV5BNzA1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_SX300.jpg")
            assertThat(it.searchedMovieRating).isEqualTo("\n8.1/10 (IMDB)\n87% (RT)")

            true
        }
    }

    @Test
    fun onClickingMovieSearchResult_shouldPopulateHistoryList() {
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState().test()
        val viewEffectTester = viewModel.viewEffect().test()

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
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val viewEffectTester = viewModel.viewEffect().test()

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
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val viewStateTester = viewModel.viewState().test()

        // populate history
        viewModel.processInput(SearchMovieEvent("blade runner 2049"))
        viewStateTester.awaitTerminalEvent(20L, TimeUnit.MILLISECONDS)
//        viewModel.processInput(AddToHistoryEvent)
        viewModel.processInput(SearchMovieEvent("blade"))
        viewStateTester.awaitTerminalEvent(20L, TimeUnit.MILLISECONDS)
//        viewModel.processInput(AddToHistoryEvent)

        // check that the result is showing Blade
        viewStateTester.assertValueAt(viewStateTester.valueCount() - 1) {
            assertThat(it.searchedMovieTitle).isEqualTo("Blade")
            true
        }

        // click blade runner 2049 from history
        viewModel.processInput(RestoreFromHistoryEvent(bladeRunner2049))
        viewStateTester.assertValueAt(viewStateTester.valueCount() - 1) {
            assertThat(it.searchedMovieTitle).isEqualTo("Blade Runner 2049")
            assertThat(it.searchedMovieRating).isEqualTo(bladeRunner2049.ratingSummary)
            true
        }

        // click blade again
        viewModel.processInput(RestoreFromHistoryEvent(blade))
        viewStateTester.assertValueAt(viewStateTester.valueCount() - 1) {
            assertThat(it.searchedMovieTitle).isEqualTo("Blade")
            assertThat(it.searchedMovieRating).isEqualTo(blade.ratingSummary)
            true
        }
    }

    private val mockApp: MSApp by lazy { mock(MSApp::class.java) }

    private val mockMovieRepo: MSMovieRepository by lazy {
        mock(MSMovieRepository::class.java).apply {
            whenever(searchMovie("blade runner 2049"))
                .thenReturn(Observable.just(bladeRunner2049))
            whenever(searchMovie("blade"))
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

        MSMovie(
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

        MSMovie(
            result = true,
            errorMessage = null,
            title = "Blade",
            ratings = listOf(ratingImdb, ratingRottenTomatoes),
            posterUrl = "https://m.media-amazon.com/images/M/MV5BMTQ4MzkzNjcxNV5BMl5BanBnXkFtZTcwNzk4NTU0Mg@@._V1_SX300.jpg"
        )
    }
}
