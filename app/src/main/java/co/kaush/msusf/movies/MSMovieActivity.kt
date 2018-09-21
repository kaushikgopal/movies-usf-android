package co.kaush.msusf.movies

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import co.kaush.msusf.MSActivity
import co.kaush.msusf.R
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import javax.inject.Inject

class MSMovieActivity : MSActivity() {

    @Inject
    lateinit var movieRepo: MSMovieRepository

    lateinit var viewModel: MSMainVm

    var disposable: Disposable? = null

    override fun inject(activity: MSActivity) {
        app.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(
            this,
            MSMainVmFactory(app, movieRepo)
        ).get(MSMainVm::class.java)
    }

    override fun onResume() {
        super.onResume()

        val screenLoadEvents: Observable<ScreenLoadEvent> = Observable.just(ScreenLoadEvent)
        val searchMovieEvents: Observable<SearchMovieEvent> =
            RxView.clicks(ms_mainScreen_searchBtn)
                .map { SearchMovieEvent(ms_mainScreen_searchText.text.toString()) }

        disposable = viewModel.send(screenLoadEvents, searchMovieEvents)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { vs ->
                    ms_mainScreen_searchText.setText(vs.searchBoxText)
                    ms_mainScreen_title.text = vs.searchedMovieTitle
                    ms_mainScreen_rating.text = vs.searchedMovieRating

                    vs.searchedMoviePoster
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            Glide.with(ctx)
                                .load(vs.searchedMoviePoster)
                                .into(ms_mainScreen_poster)
                        } ?: run {
                        ms_mainScreen_poster.setImageResource(0)
                    }
                },
                { Timber.w("something went terribly wrong", it) }
            )
    }

    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }
}
