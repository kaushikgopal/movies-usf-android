package co.kaush.msusf.movies

import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import com.google.common.truth.Truth.assertThat
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.mockito.Mockito.mock



class MSMainVmTest {

    lateinit var viewModel: MSMainVm

    private val mockApp: MSApp by lazy { mock(MSApp::class.java) }

    @Test
    fun onSubscribing_shouldReceiveStartingViewState() {
        val mockMovieRepo: MSMovieRepository = mock(MSMovieRepository::class.java)

        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val eventTester = PublishSubject.create<MSMovieEvent>()
        val viewModelTester = viewModel.send(eventTester).test()

        viewModelTester.assertValueCount(1)
    }

    @Test
    fun onScreenLoad_searchBoxText_shouldBeCleared() {
        val mockMovieRepo: MSMovieRepository = mock(MSMovieRepository::class.java)

        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val eventTester = PublishSubject.create<MSMovieEvent>()
        val viewModelTester = viewModel.send(eventTester).test()

        eventTester.onNext(ScreenLoadEvent)

        viewModelTester.assertValueAt(1) {
            assertThat(it.searchBoxText).isEqualTo("")
            true
        }
    }
}