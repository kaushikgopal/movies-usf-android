package co.kaush.msusf.movies

import app.cash.turbine.test
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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.times

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

  // todo: write a test to emulate the memory leak
  //  1. call OnViewCreated multiple times
  //  2. if the connection is always live, it _will_ leak
  //    but with the new sharingWhileSubscribed, does it dispose off the previous one?

  @Test
  fun memLeakTest() = runTest {
    val vm = createTestViewModel()

    val vs = mutableListOf<MSMovieViewState>()

    val job = launch { vm.viewState.toList(vs) }

    repeat(10) {
      vm.processInput(ScreenLoadEvent)
      runCurrent()
    }

    job.ca

  }

  @Test
  @DisplayName("on screen load, search box test should be cleared")
  fun onScreenLoad_searchBoxText_shouldBeCleared() = runTest {
    val vm = createTestViewModel()

    val vs = mutableListOf<MSMovieViewState>()
    backgroundScope.launch { vm.viewState.toList(vs) }

    runCurrent()
    assertThat(vs[0].searchBoxText).isEqualTo("Blade") // starts off with blade

    vm.processInput(ScreenLoadEvent)
    runCurrent()
    assertThat(vs[1].searchBoxText).isEmpty()

//    vm.viewState.test {
//      assertThat(awaitItem().searchBoxText).isEqualTo("Blade") // starts off with blade
//      vm.processInput(ScreenLoadEvent)
//      assertThat(awaitItem().searchBoxText).isEmpty()
//    }
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

  @Test
  @DisplayName("on search movie, show loading indicator")
  fun onSearchingMovie_showLoadingIndicator_ThenResult() = runTest {
    viewModel = createTestViewModel()
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
  @DisplayName("click search result, should show it in history")
  fun onClickingMovieSearchResult_shouldPopulateHistoryList() = runTest {
    viewModel = createTestViewModel()

    val vs = mutableListOf<MSMovieViewState>()
    val ve = mutableListOf<MSMovieEffect>()
    backgroundScope.launch { viewModel.viewState.toList(vs) }
    backgroundScope.launch { viewModel.effects.toList(ve) }

    viewModel.processInput(SearchMovieEvent("blade runner 2049"))
    viewModel.processInput(AddToHistoryEvent(bladeRunner2049))

    runCurrent()

    vs[1].let {
      assertThat(it.adapterList).hasSize(1)
      assertThat(it.adapterList[0]).isEqualTo(bladeRunner2049)
    }
    assertThat(ve[0]).isEqualTo(AddedToHistoryToastEffect)
  }

  @Test
  @DisplayName("adding to history twice, should show two toasts")
  fun onClickingMovieSearchResultTwice_shouldShowToastEachTime() = runTest {
    viewModel = createTestViewModel()

    val ve = mutableListOf<MSMovieEffect>()
    backgroundScope.launch { viewModel.effects.toList(ve) }

    viewModel.processInput(SearchMovieEvent("blade runner 2049"))
    assertThat(ve.size).isEqualTo(0)

    viewModel.processInput(AddToHistoryEvent(bladeRunner2049))
    runCurrent()
    assertThat(ve[0]).isEqualTo(AddedToHistoryToastEffect)

    viewModel.processInput(AddToHistoryEvent(bladeRunner2049))
    runCurrent()
    assertThat(ve[1]).isEqualTo(AddedToHistoryToastEffect)
  }

  @Test
  fun onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo() = runTest {
    viewModel = createTestViewModel()
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
