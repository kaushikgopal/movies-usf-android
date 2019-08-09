package co.kaush.msusf.genres

import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.kaush.msusf.MSApp
import co.kaush.msusf.R
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class DemoGenreVM(
        app: MSApp, private val genreRepo: GenreRepository
) : AndroidViewModel(app) {

    private val eventEmitter: PublishSubject<GenreEvent> = PublishSubject.create()

    private lateinit var disposable: Disposable

    val viewState: Observable<GenreViewState>
//    val viewEffects: Observable<GenreViewEffect>

    init {
        eventEmitter
                .doOnNext { Timber.d("----- event $it") }
                .eventToResult()
                .doOnNext { Timber.d("----- result $it") }
                .share()
                .also { result ->
                    viewState = result
                            .resultToViewState()
                            .doOnNext { Timber.d("----- vs $it") }
                            .replay(1)
                            .autoConnect(1) { disposable = it }

//                    viewEffects = result
//                            .resultToViewEffect()
//                            .doOnNext { Timber.d("----- ve $it") }
                }
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }

    fun processInput(event: GenreEvent) = eventEmitter.onNext(event)

    private fun Observable<GenreEvent>.eventToResult(): Observable<out GenreResult> {
        return publish { o ->
            Observable.merge(
                    o.ofType(GenreEvent.GenreLoadEvent::class.java).onScreenLoad(),
                    o.ofType(GenreEvent.GenreLoadEvent::class.java).onScreenLoad(),
                    o.ofType(GenreEvent.GenreToggleEvent::class.java).onGenreToggled()
            )
        }
    }

    private fun Observable<out GenreResult>.resultToViewState(): Observable<GenreViewState> {
        return scan(
                GenreViewState(pageTitle = R.string.loading, pageDescription = R.string.loading)
        ) { vs: GenreViewState, result: GenreResult ->
            when (result) {
                is GenreResult.GenreLoadResult ->
                    vs.copy(
                            pageTitle = R.string.genreScreen_pageTitle,
                            pageDescription = R.string.genreScreen_pageDescription,
                            checkboxListViewState = result.list
                    )
                is GenreResult.GenreToggleResult -> vs
            }
        }.distinctUntilChanged()
    }

    // -------------------------------------------------------------------------
    // use cases

    private fun Observable<GenreEvent.GenreLoadEvent>.onScreenLoad()
            : Observable<GenreResult.GenreLoadResult> {
        return flatMap {
            genreRepo
                    .genresWithSelection()
                    .map { list ->
                        list.map { GenreCheckBoxViewState(it.first.title, it.second) }
                    }
                    .map { GenreResult.GenreLoadResult(it) }
        }
    }

    private fun Observable<GenreEvent.GenreToggleEvent>.onGenreToggled()
            : Observable<GenreResult.GenreToggleResult> {

        if (1 == 1) {
            return Observable.empty()
        }
        return map { it.genre }
                .map { genreRepo.toggleGenreSelection(it) }
                .map { GenreResult.GenreToggleResult }
    }

    // ------------------------------------------------------------------------

    class GenreVmFactory(
            private val app: MSApp,
            private val genreRepo: GenreRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return DemoGenreVM(app, genreRepo) as T
        }
    }
}


sealed class GenreEvent {
    object GenreLoadEvent : GenreEvent()
    data class GenreToggleEvent(val genre: MSGenre) : GenreEvent()
}

sealed class GenreResult {
    data class GenreLoadResult(val list: List<GenreCheckBoxViewState>) : GenreResult()
    object GenreToggleResult : GenreResult()
}

sealed class GenreViewEffect {

}

data class GenreViewState(
        @StringRes val pageTitle: Int = -1,
        @StringRes val pageDescription: Int = -1,
        val checkboxListViewState: List<GenreCheckBoxViewState> = emptyList()
)

data class GenreCheckBoxViewState(
        val checkboxName: String,
        val isChecked: Boolean
)