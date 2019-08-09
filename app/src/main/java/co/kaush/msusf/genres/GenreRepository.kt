package co.kaush.msusf.genres

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class GenreRepository @Inject constructor() {

    /**
     * This is literally a subject that's used to listen and publish updates
     * to the "selected genres" list.
     *
     * A more proficient database like SQLDelight or Room would handle this much better
     */
    private val genreUpdates: PublishSubject<Unit> = PublishSubject.create()

    /**
     *  starter list of selected Genres
     *  Really this field exists in lieu of a proper database like SQLDelight or Room
     */
    private val selectedGenres: MutableSet<MSGenre> = mutableSetOf(MSGenre.Comedy, MSGenre.Romance)

    fun toggleGenreSelection(genre: MSGenre): Boolean {
        if (genre in selectedGenres) {
            selectedGenres.remove(genre)
        } else {
            selectedGenres.add(genre)
        }

        genreUpdates.onNext(Unit)

        return selectedGenres.isNotEmpty()
    }

    fun genresWithSelection(): Observable<List<Pair<MSGenre, Boolean>>> {
        return genreUpdates
                .startWith(Unit)
                .withLatestFrom(
                        Observable.just(MSGenre.values().asList()),
                        BiFunction { _: Unit, fullGenreList: List<MSGenre> ->
                            fullGenreList
                                    .map { Pair(it, (it in selectedGenres)) }
                                    .sortedBy { it.first.title }
                        }
                )
                .doOnComplete { Timber.d(" -- ⚠️ this shouldn't be completing ") }

    }

    fun findGenre(checkboxName: String): MSGenre = MSGenre.valueOf(checkboxName)

}

enum class MSGenre(val title: String) {
    Whimsical(title = "Whimsical"),
    Adventure(title = "Adventure"),
    Action(title = "Action"),
    Comedy(title = "Comedy"),
    Crime(title = "Crime"),
    Drama(title = "Drama"),
    Fantasy(title = "Fantasy"),
    Historical(title = "Historical"),
    Fiction(title = "Fiction"),
    Horror(title = "Horror"),
    Romance(title = "Romance"),
    SciFi(title = "SciFi"),
    Thriller(title = "Thriller")
}


