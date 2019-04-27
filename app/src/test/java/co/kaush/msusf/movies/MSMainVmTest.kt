package co.kaush.msusf.movies

import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.test.MvRxTestRule
import com.airbnb.mvrx.withState
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.ClassRule
import org.junit.Test
import org.mockito.Mockito.mock

class MSMainVmTest {

    private lateinit var viewModel: MSMainVm

    @Test
    fun onSubscribing_shouldReceiveStartingViewState() {
        viewModel = MSMainVm(MSMovieViewState(), mockMovieRepo, false)

        withState(viewModel) {
            assertThat(it.searchMovieRequest is Uninitialized)
        }
    }

    @Test
    fun onScreenLoad_searchBoxText_shouldBeCleared() {
        viewModel = MSMainVm(MSMovieViewState(), mockMovieRepo, false)

        withState(viewModel) {
            assertThat(it.searchBoxText).isEqualTo("")
        }
    }

    @Test
    fun onSearchingMovie_shouldSeeSearchResults() {
        val repoSubject = PublishSubject.create<MSMovie>()
        whenever(mockMovieRepo.searchMovie("blade runner 2049")).thenReturn(repoSubject)

        viewModel = MSMainVm(MSMovieViewState(), mockMovieRepo, false)

        viewModel.searchMovie("blade runner 2049")

        verify(mockMovieRepo).searchMovie("blade runner 2049")
        withState(viewModel) { assertThat(it.searchMovieRequest is Loading) }

        repoSubject.onNext(bladeRunner2049)

        withState(viewModel) {
            assertThat(it.searchMovieRequest is Success)
            assertThat(it.searchBoxText).isEqualTo("blade runner 2049")
            assertThat(it.searchMovieRequest.invoke()).isEqualTo(bladeRunner2049)
        }
    }

    @Test
    fun onClickingMovieSearchResult_shouldPopulateHistoryList() {
        viewModel = MSMainVm(MSMovieViewState(), mockMovieRepo, false)

        viewModel.searchMovie("blade runner 2049")

        withState(viewModel) {
            assertThat(it.searchMovieRequest is Success)
            assertThat(it.adapterList.isEmpty())
            assertThat(!it.addedToHistory)
        }

        viewModel.addToHistory()

        withState(viewModel) {
            assertThat(it.adapterList).hasSize(1)
            assertThat(it.adapterList.contains(bladeRunner2049))
            assertThat(it.addedToHistory)
        }

        withState(viewModel) {
            assertThat(it.adapterList).hasSize(1)
            assertThat(it.adapterList.contains(bladeRunner2049))
            assertThat(!it.addedToHistory)
        }
    }

    @Test
    fun onClickingMovieSearchResultTwice_shouldShowToastEachTime() {
        viewModel = MSMainVm(MSMovieViewState(), mockMovieRepo, false)

        viewModel.searchMovie("blade runner 2049")

        withState(viewModel) {
            assertThat(it.searchMovieRequest is Success)
            assertThat(it.adapterList.isEmpty())
            assertThat(!it.addedToHistory)
        }

        viewModel.addToHistory()
        viewModel.addToHistory()

        withState(viewModel) {
            assertThat(it.adapterList).hasSize(1)
            assertThat(it.adapterList.contains(bladeRunner2049))
            assertThat(it.addedToHistory)
        }

        withState(viewModel) {
            assertThat(it.adapterList).hasSize(1)
            assertThat(it.adapterList.contains(bladeRunner2049))
            assertThat(!it.addedToHistory)
        }

        withState(viewModel) {
            assertThat(it.adapterList).hasSize(1)
            assertThat(it.adapterList.contains(bladeRunner2049))
            assertThat(it.addedToHistory)
        }

        withState(viewModel) {
            assertThat(it.adapterList).hasSize(1)
            assertThat(it.adapterList.contains(bladeRunner2049))
            assertThat(!it.addedToHistory)
        }
    }

    @Test
    fun onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo() {
        viewModel = MSMainVm(MSMovieViewState(), mockMovieRepo, false)

        viewModel.searchMovie("blade runner 2049")
        viewModel.addToHistory()
        viewModel.searchMovie("blade")
        viewModel.addToHistory()

        withState(viewModel) {
            assertThat(it.searchBoxText).isEqualTo("blade")
            assertThat(it.searchMovieRequest.invoke()).isEqualTo(blade)
        }

        viewModel.restoreFromHistory(bladeRunner2049)

        withState(viewModel) {
            assertThat(it.searchBoxText).isEqualTo("Blade Runner 2049")
            assertThat(it.searchMovieRequest.invoke()).isEqualTo(bladeRunner2049)
        }

        viewModel.restoreFromHistory(blade)

        withState(viewModel) {
            assertThat(it.searchBoxText).isEqualTo("Blade")
            assertThat(it.searchMovieRequest.invoke()).isEqualTo(blade)
        }
    }

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

    companion object {
        @JvmField
        @ClassRule
        val mvrxTestRule = MvRxTestRule()
    }
}