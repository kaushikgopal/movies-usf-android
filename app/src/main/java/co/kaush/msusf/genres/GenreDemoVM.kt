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
    val viewEffects: Observable<GenreViewEffect>

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

                    viewEffects = result
                            .resultToViewEffect()
                            .doOnNext { Timber.d("----- ve $it") }
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
                    o.ofType(GenreEvent.GenreToggleEvent::class.java).onGenreToggled()
            )
        }
    }


    private fun Observable<out GenreResult>.resultToViewEffect(): Observable<GenreViewEffect> {
        return map { result ->
            when (result) {
                is GenreResult.GenreToggleResult -> {
                    if (!result.wasToggled) {
                        GenreViewEffect.ToastError("You need atleast one genre toggled")
                    } else {
                        GenreViewEffect.NoEffect
                    }
                }
                else -> GenreViewEffect.NoEffect
            }
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

                is GenreResult.GenreToggleResult -> {
                    if (result.wasToggled) {
                        return@scan vs.copy()
                    }

                    // genre wasn't toggled

                    // we'll have to toggle it back on for the UI
                    val newCheckboxListViewState: ArrayList<GenreCheckBoxViewState> =
                            ArrayList(vs.checkboxListViewState)
                    val checkboxVS = GenreCheckBoxViewState(result.toggledGenre.title, true)
                    newCheckboxListViewState.add(checkboxVS)

                    // send an error toast


                    vs.copy(checkboxListViewState = newCheckboxListViewState)
                }

                else -> throw IllegalStateException()
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

        return map { it.genre }.map { genre ->
            val wasToggled = genreRepo.toggleGenreSelection(genre)
            GenreResult.GenreToggleResult(wasToggled, genre)
        }
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

interface GenreResult {
    data class GenreLoadResult(val list: List<GenreCheckBoxViewState>) : GenreResult
    data class GenreToggleResult(
            val wasToggled: Boolean,
            val toggledGenre: MSGenre

    ) : GenreResult
}

interface GenreViewEffect {
    object NoEffect : GenreViewEffect
    data class ToastError(val errMsg: String) : GenreViewEffect
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