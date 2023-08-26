package co.kaush.msusf.movies

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.di.TestAppComponent
import co.kaush.msusf.movies.di.bladeRunner2049
import co.kaush.msusf.movies.di.create
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
  private lateinit var eventProcessor: EventProcessor
  private lateinit var uselessRepo: MSUselessRepository

  // Use a fake repository to be injected into the viewModel
  private val testAppComponent = TestAppComponent::class.create()
  private var fakeMovieAppRepository: MSMovieRepository = testAppComponent.movieRepository

  @Before
  fun setupViewModel() {
    uselessRepo = MSUselessRepository()
    viewModel = MSMovieViewModel(fakeMovieAppRepository, uselessRepo)
    eventProcessor = EventProcessor(viewModel.viewModelScope)
  }

  @Test
  fun onSubscription_InitialStateIsEmitted() = runTest {
    viewModel.viewState.test {
      val viewState = awaitItem()
      assertThat(viewState.searchBoxText).isEqualTo("Blade")
    }
  }

  @Test
  fun onScreenLoad_searchBoxText_shouldBeCleared() = runTest {
//    viewModel.viewState.test {
//      assertThat(awaitItem().searchBoxText).isEqualTo("Blade")
//      processQueue(ScreenLoadEvent)
//      assertThat(awaitItem().searchBoxText).isEmpty()
//      expectNoEvents()
//    }
  }

  @Test
  fun onSearchingMovie_showLoadingIndicator_ThenResult() = runTest {
    viewModel.viewState.test {
      awaitItem() // starting state

      processQueue(MSMovieEvent.ScreenLoadEvent2)

      processQueue(SearchMovieEvent("blade runner 2049"))
      val firstItem = awaitItem()
      assertThat(firstItem.searchedMovieTitle).isEqualTo("Searching Movie...")

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
      processQueue(MSMovieEvent.ScreenLoadEvent2)

      processQueue(SearchMovieEvent("blade runner 2049"))
      processQueue(MSMovieEvent.AddToHistoryEvent(bladeRunner2049))

      vsTester.skipItems(3)
      with(vsTester.awaitItem()) {
        assertThat(adapterList).hasSize(1)
        assertThat(adapterList[0]).isEqualTo(bladeRunner2049)
      }

      assertThat(veTester.awaitItem()).isEqualTo(MSMovieViewEffect.AddedToHistoryToastEffect)

      vsTester.expectNoEvents()
      veTester.expectNoEvents()
    }
  }

  @Test
  fun onClickingMovieSearchResultTwice_shouldShowToastEachTime() = runTest {
    viewModel.viewEffect.test {
      processQueue(MSMovieEvent.ScreenLoadEvent2)

      processQueue(SearchMovieEvent("blade runner 2049"))

      processQueue(MSMovieEvent.AddToHistoryEvent(bladeRunner2049))
      assertThat(awaitItem()).isEqualTo(MSMovieViewEffect.AddedToHistoryToastEffect)

      processQueue(MSMovieEvent.AddToHistoryEvent(bladeRunner2049))
      assertThat(awaitItem()).isEqualTo(MSMovieViewEffect.AddedToHistoryToastEffect)

      expectNoEvents()
    }
  }

  @Test
  fun onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo() = runTest {
    println("START onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo")
    viewModel.viewState.test {

      println("BASE")
      val baseState = awaitItem()
      assertThat(baseState.searchBoxText).isEqualTo("Blade")

      processQueue(MSMovieEvent.ScreenLoadEvent2)
      println("${System.currentTimeMillis()} uselessRepo.emit")
      eventProcessor.addToQueue { uselessRepo.emit() }

      delay(300)

      val batman = awaitItem()
      println("${batman.searchedMovieTitle}")
      assertThat(batman.searchedMovieTitle.lowercase()).contains("batman")

//       populate history
      processQueue(SearchMovieEvent("blade runner 2049"))

      awaitItem()
      val bladeRunner = awaitItem()
      println("${bladeRunner.searchedMovieTitle}")
      assertThat(bladeRunner.searchedMovieTitle.lowercase()).contains("blade runner 2049")

      processQueue(SearchMovieEvent("blade"))
      skipItems(2)
//
      // click blade runner 2049 from history
      processQueue(RestoreFromHistoryEvent(bladeRunner2049))
      with(awaitItem()) {
        assertThat(searchedMovieTitle).isEqualTo("Blade Runner 2049")
        assertThat(searchedMovieRating).isEqualTo(bladeRunner2049.ratingSummary)
      }
//
//      // click blade again
//      processQueue(RestoreFromHistoryEvent(blade))
//      with(awaitItem()) {
//        assertThat(searchedMovieTitle).isEqualTo("Blade")
//        assertThat(searchedMovieRating).isEqualTo(blade.ratingSummary)
//      }
//    }
    }
    println("END onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo")
  }

  fun processQueue(event: MSMovieEvent) {
    eventProcessor.addToQueue { viewModel.processInput(event) }
  }
}
