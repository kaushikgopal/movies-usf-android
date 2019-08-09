package co.kaush.msusf.genres

import co.kaush.msusf.MSApp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito

class DemoGenreVMTest {

    private lateinit var viewModel: DemoGenreVM
    private lateinit var genreRepository: GenreRepository

    @Test
    fun `when screen loaded for the first time, 2 out of 13 genres should be selected`() {
        genreRepository = GenreRepository()

        viewModel = DemoGenreVM(mockApp, genreRepository)
        val vsTester = viewModel.viewState.test()

        viewModel.processInput(GenreEvent.GenreLoadEvent)

        vsTester.assertValueAt(1) {
            assertThat(it.checkboxListViewState.size).isEqualTo(13)
            assertThat(it.checkboxListViewState.filter { it.isChecked }.size).isEqualTo(2)
            true
        }
    }

    @Test
    fun `when unselected genre is toggled, it should be selected`() {
        genreRepository = GenreRepository()

        viewModel = DemoGenreVM(mockApp, genreRepository)
        val vsTester = viewModel.viewState.test()

        viewModel.processInput(GenreEvent.GenreLoadEvent)
        viewModel.processInput(GenreEvent.GenreToggleEvent(MSGenre.Action))

        vsTester.assertValueAt(2) {
            assertThat(
                    it.checkboxListViewState
                            .find { it.checkboxName == "Action" }!!
                            .isChecked
            ).isTrue()
            true
        }
    }

    @Test
    fun `when selected genre is toggled, it should be unselected`() {
        genreRepository = GenreRepository()

        viewModel = DemoGenreVM(mockApp, genreRepository)
        val vsTester = viewModel.viewState.test()

        viewModel.processInput(GenreEvent.GenreLoadEvent)
        viewModel.processInput(GenreEvent.GenreToggleEvent(MSGenre.Comedy))

        vsTester.assertValueAt(2) {
            assertThat(
                    it.checkboxListViewState
                            .find { it.checkboxName == "Comedy" }!!
                            .isChecked
            ).isFalse()
            true
        }
    }

    private val mockApp: MSApp by lazy { Mockito.mock(MSApp::class.java) }
}


