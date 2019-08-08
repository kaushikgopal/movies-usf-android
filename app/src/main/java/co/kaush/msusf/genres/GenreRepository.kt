package co.kaush.msusf.genres

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import javax.inject.Singleton

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
    val selectedGenres: MutableSet<MSGenre> = mutableSetOf(MSGenre.Comedy, MSGenre.Romance)

    fun allGenres(): Observable<List<MSGenre>> = Observable.just(MSGenre.values().asList())

    fun toggleGenreSelection(genre: MSGenre) {
        if (genre in selectedGenres) {
            selectedGenres.remove(genre)
        } else {
            selectedGenres.add(genre)
        }

        genreUpdates.onNext(Unit)
    }

    fun selectedGenres(): Observable<List<MSGenre>> = genreUpdates.hide().map { selectedGenres.toList() }
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


