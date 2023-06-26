package co.kaush.msusf.movies

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.MSMovieViewEffect.AddedToHistoryToastEffect
import co.kaush.msusf.movies.di.TestAppComponent
import co.kaush.msusf.movies.di.blade
import co.kaush.msusf.movies.di.bladeRunner2049
import co.kaush.msusf.movies.di.create
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class MSMovieVmTest {

  private lateinit var viewModel: MSMovieVm

  private val testAppComponent = TestAppComponent::class.create()

  private var fakeMovieAppRepository: MSMovieRepository = testAppComponent.movieRepository

  @Before
  fun setUp() {
    viewModel = MSMovieVm(mockApp, fakeMovieAppRepository)
  }

  @Test
  fun onSubscribing_shouldReceiveStartingviewState() = runTest {
    assertThat(viewModel.viewState.value).isEqualTo(MSMovieViewState())
  }

  @Test
  fun onScreenLoad_searchBoxText_shouldBeCleared() = runTest {
    viewModel.viewState.test {
      assertThat(awaitItem()).isEqualTo(MSMovieViewState())
      viewModel.processInput(ScreenLoadEvent)
      assertThat(awaitItem().searchBoxText).isEqualTo("load test")
    }
  }

  @Test
  fun onSearchingMovie_shouldSeeSearchResults() = runTest {
    viewModel.viewState.test {
      assertThat(awaitItem()).isEqualTo(MSMovieViewState())
      viewModel.processInput(SearchMovieEvent("blade runner 2049"))
      assertThat(awaitItem().searchedMovieTitle).isEqualTo("Searching Movie...")
      with(awaitItem()) {
        assertThat(searchedMovieTitle).isEqualTo("Blade Runner 2049")
        assertThat(searchedMoviePoster)
            .isEqualTo(
                "https://m.media-amazon.com/images/M/MV5BNzA1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_SX300.jpg",
            )
        assertThat(searchedMovieRating).isEqualTo("\n8.1/10 (IMDB)\n87% (RT)")
      }
    }
  }

  @Test
  fun onClickingMovieSearchResult_shouldPopulateHistoryList() = runTest {
    turbineScope {
      val vsTester = viewModel.viewState.testIn(backgroundScope)
      val veTester = viewModel.viewEffect.testIn(backgroundScope)

      viewModel.processInput(SearchMovieEvent("blade runner 2049"))
      viewModel.processInput(AddToHistoryEvent(bladeRunner2049))

      assertThat(vsTester.awaitItem()).isEqualTo(MSMovieViewState())
      assertThat(vsTester.awaitItem().searchedMovieTitle).isEqualTo("Searching Movie...")
      assertThat(vsTester.awaitItem().searchedMovieTitle).isEqualTo("Blade Runner 2049")
      with(vsTester.awaitItem()) {
        assertThat(adapterList).hasSize(1)
        assertThat(adapterList[0]).isEqualTo(bladeRunner2049)
      }

      assertThat(veTester.awaitItem()).isEqualTo(AddedToHistoryToastEffect)
    }
  }

  @Test
  fun onClickingMovieSearchResultTwice_shouldShowToastEachTime() = runTest {
    turbineScope {
      val veTester = viewModel.viewEffect.testIn(backgroundScope)

      viewModel.processInput(SearchMovieEvent("blade runner 2049"))
      viewModel.processInput(AddToHistoryEvent(bladeRunner2049))
      viewModel.processInput(AddToHistoryEvent(bladeRunner2049))

      assertThat(veTester.awaitItem()).isEqualTo(AddedToHistoryToastEffect)
      assertThat(veTester.awaitItem()).isEqualTo(AddedToHistoryToastEffect)
    }
  }

  @Test
  fun onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo() = runTest {
    viewModel.viewState.test {
      awaitItem() // starting state

      // populate history
      viewModel.processInput(SearchMovieEvent("blade runner 2049"))
      viewModel.processInput(SearchMovieEvent("blade"))
      repeat(4) { awaitItem() }

      // click blade runner 2049 from history
      viewModel.processInput(RestoreFromHistoryEvent(bladeRunner2049))
      with(awaitItem()) {
        assertThat(searchedMovieTitle).isEqualTo("Blade Runner 2049")
        assertThat(searchedMovieRating).isEqualTo(bladeRunner2049.ratingSummary)
      }

      // click blade again
      viewModel.processInput(RestoreFromHistoryEvent(blade))
      with(awaitItem()) {
        assertThat(searchedMovieTitle).isEqualTo("Blade")
        assertThat(searchedMovieRating).isEqualTo(blade.ratingSummary)
      }
    }
  }

  private val mockApp: MSApp by lazy { mock(MSApp::class.java) }
}
