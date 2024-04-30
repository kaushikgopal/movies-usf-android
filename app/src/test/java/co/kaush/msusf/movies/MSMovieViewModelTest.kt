package co.kaush.msusf.movies

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import co.kaush.msusf.movies.MSMovieEffect.AddedToHistoryToastEffect
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.di.TestAppComponent
import co.kaush.msusf.movies.di.blade
import co.kaush.msusf.movies.di.bladeRunner2049
import co.kaush.msusf.movies.di.create
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MSMovieViewModelTest {

  // Set the main coroutines dispatcher for unit testing.
  @OptIn(ExperimentalCoroutinesApi::class)
  @JvmField
  @RegisterExtension
  val testRule = CoroutineTestRule()

  // Subject under test
  private lateinit var viewModel: MSMovieViewModelImpl

  // Use a fake repository to be injected into the viewModel
  private val testAppComponent = TestAppComponent::class.create()
  private var fakeMovieAppRepository: MSMovieRepository = testAppComponent.movieRepository

  @Test
  @DisplayName("on init, initial view state should have Blade pre-populated")
  fun onSubscription_InitialStateIsEmitted() = runTest {
    viewModel = createTestViewModel()
    val vs = viewModel.viewState.first()
    assertThat(vs.searchBoxText).isEqualTo("Blade")
  }

  @Test
  @DisplayName("on screen load, search box test should be cleared")
  fun onScreenLoad_searchBoxText_shouldBeCleared() = runTest {
    val vm = createTestViewModel()
    assertThat(vm.viewState.first().searchBoxText).isEqualTo("Blade") // starts off with blade
    vm.processInput(ScreenLoadEvent)
    runCurrent()
    assertThat(vm.viewState.first().searchBoxText).isEmpty()
  }

  @Test
  @DisplayName("on screen load, search box test should be cleared - using turbine")
  fun onScreenLoad_searchBoxText_shouldBeCleared_2() =
      // Functionally the exact same test as the previous one
      // we use turbine here as a demonstration
      runTest {
        viewModel = createTestViewModel()
        viewModel.viewState.test() {
          assertThat(awaitItem().searchBoxText).isEqualTo("Blade") // starts off with blade
          viewModel.processInput(ScreenLoadEvent)
          // notice that we don't need to runCurrent()
          // runCurrent()
          // this is because Turbine uses an UnconfinedTestDispatcher internally
          // https://github.com/cashapp/turbine/blob/trunk/src/commonMain/kotlin/app/cash/turbine/flow.kt#L199-L201
          assertThat(awaitItem().searchBoxText).isEmpty()
        }
      }

  @Disabled
  @Test
  fun onSearchingMovie_showLoadingIndicator_ThenResult() = runTest {
    viewModel = createTestViewModel()
    val vs = viewModel.viewState.first()
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

  @Disabled
  @Test
  fun onClickingMovieSearchResult_shouldPopulateHistoryList() = runTest {
    // TODO: yield?
    viewModel = createTestViewModel()
    val vs = viewModel.viewState.first()
    turbineScope {
      val vsTester = viewModel.viewState.testIn(backgroundScope)
      val veTester = viewModel.effects.testIn(backgroundScope)

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

  @Disabled
  @Test
  fun onClickingMovieSearchResultTwice_shouldShowToastEachTime() = runTest {
    // TODO: yield?
    viewModel = createTestViewModel()
    val vs = viewModel.viewState.first()
    viewModel.effects.test {
      viewModel.processInput(SearchMovieEvent("blade runner 2049"))
      viewModel.processInput(AddToHistoryEvent(bladeRunner2049))
      viewModel.processInput(AddToHistoryEvent(bladeRunner2049))

      assertThat(awaitItem()).isEqualTo(AddedToHistoryToastEffect)
      assertThat(awaitItem()).isEqualTo(AddedToHistoryToastEffect)
      expectNoEvents()
    }
  }

  @Disabled
  @Test
  fun onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo() = runTest {
    viewModel = createTestViewModel()
    val vs = viewModel.viewState.first()
    viewModel.viewState.test {
      skipItems(1) // starting state

      // populate history
      viewModel.processInput(SearchMovieEvent("blade runner 2049"))
      skipItems(2)

      viewModel.processInput(SearchMovieEvent("blade"))
      skipItems(2)

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

  private fun TestScope.createTestViewModel() =
      MSMovieViewModelImpl(fakeMovieAppRepository, backgroundScope, testRule.testDispatcher)
}
