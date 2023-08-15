package co.kaush.msusf.movies

import app.cash.turbine.test
import app.cash.turbine.turbineScope
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MSMovieViewModelTest {

  // Set the main coroutines dispatcher for unit testing.
  @ExperimentalCoroutinesApi @get:Rule val mainCoroutineRule = MainCoroutineRule()

  // Subject under test
  private lateinit var viewModel: MSMovieViewModel

  // Use a fake repository to be injected into the viewModel
  private val testAppComponent = TestAppComponent::class.create()
  private var fakeMovieAppRepository: MSMovieRepository = testAppComponent.movieRepository

  @Before
  fun setupViewModel() {
    viewModel = MSMovieViewModel(fakeMovieAppRepository)
  }

  @Test
  fun onSubscription_InitialStateIsEmitted() = runTest {
    val vs = viewModel.viewState.first()
    assertThat(vs.searchBoxText).isEqualTo("Blade")
  }

  @Test
  fun onScreenLoad_searchBoxText_shouldBeCleared() = runTest {
    viewModel.viewState.test {
      assertThat(awaitItem().searchBoxText).isEqualTo("Blade")
      viewModel.processInput(ScreenLoadEvent)
      assertThat(awaitItem().searchBoxText).isEmpty()
      expectNoEvents()
    }
  }

  @Test
  fun onSearchingMovie_showLoadingIndicator_ThenResult() = runTest {
    viewModel.viewState.test {
      skipItems(1) // starting state

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
      expectNoEvents()
    }
  }

  @Test
  fun onClickingMovieSearchResult_shouldPopulateHistoryList() = runTest {
    turbineScope {
      val vsTester = viewModel.viewState.testIn(backgroundScope)
      val veTester = viewModel.viewEffect.testIn(backgroundScope)

      viewModel.processInput(SearchMovieEvent("blade runner 2049"))
      viewModel.processInput(AddToHistoryEvent(bladeRunner2049))

      vsTester.skipItems(3)
      with(vsTester.awaitItem()) {
        assertThat(adapterList).hasSize(1)
        assertThat(adapterList[0]).isEqualTo(bladeRunner2049)
      }

      assertThat(veTester.awaitItem()).isEqualTo(AddedToHistoryToastEffect)

      vsTester.expectNoEvents()
      veTester.expectNoEvents()
    }
  }

  @Test
  fun onClickingMovieSearchResultTwice_shouldShowToastEachTime() = runTest {
    viewModel.viewEffect.test {
      viewModel.processInput(SearchMovieEvent("blade runner 2049"))
      viewModel.processInput(AddToHistoryEvent(bladeRunner2049))
      viewModel.processInput(AddToHistoryEvent(bladeRunner2049))

      assertThat(awaitItem()).isEqualTo(AddedToHistoryToastEffect)
      assertThat(awaitItem()).isEqualTo(AddedToHistoryToastEffect)
      expectNoEvents()
    }
  }

  @Test
  fun onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo() = runTest {
    viewModel.viewState.test {

      // skipItems(1) // skip tends to be flaky
      awaitItem() // starting state

      // populate history
      viewModel.processInput(SearchMovieEvent("blade runner 2049"))
      awaitItem()
      awaitItem()

      viewModel.processInput(SearchMovieEvent("blade"))
      awaitItem()
      awaitItem()

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
}
