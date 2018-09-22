package co.kaush.msusf.movies

import android.animation.AnimatorSet
import android.animation.ObjectAnimator.ofPropertyValuesHolder
import android.animation.PropertyValuesHolder.ofFloat
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.CircularProgressDrawable
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutCompat.HORIZONTAL
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import co.kaush.msusf.MSActivity
import co.kaush.msusf.R
import co.kaush.msusf.movies.MSMovieEvent.ClickMovieEvent
import co.kaush.msusf.movies.MSMovieEvent.ClickMovieFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import javax.inject.Inject

class MSMovieActivity : MSActivity() {

    @Inject
    lateinit var movieRepo: MSMovieRepository

    lateinit var viewModel: MSMainVm
    lateinit var listAdapter: MSMovieSearchHistoryAdapter

    private var disposable: Disposable? = null
    private val historyItemClick: PublishSubject<MSMovie> = PublishSubject.create()

    private val spinner: CircularProgressDrawable by lazy {
        val circularProgressDrawable = CircularProgressDrawable(this)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()
        circularProgressDrawable
    }

    override fun inject(activity: MSActivity) {
        app.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupListView()

        viewModel = ViewModelProviders.of(
            this,
            MSMainVmFactory(app, movieRepo)
        ).get(MSMainVm::class.java)
    }

    override fun onResume() {
        super.onResume()

        val screenLoadEvents: Observable<ScreenLoadEvent> = Observable.just(ScreenLoadEvent)
        val searchMovieEvents: Observable<SearchMovieEvent> = RxView.clicks(ms_mainScreen_searchBtn)
            .map { SearchMovieEvent(ms_mainScreen_searchText.text.toString()) }
        val movieSelectEvents: Observable<ClickMovieEvent> = RxView.clicks(ms_mainScreen_poster)
            .map {
                ms_mainScreen_poster.growShrink()
                ClickMovieEvent
            }
        val movieHistoryClickEvents: Observable<ClickMovieFromHistoryEvent> = historyItemClick
            .map { ClickMovieFromHistoryEvent(it) }

        disposable = viewModel.send(
            screenLoadEvents,
            searchMovieEvents,
            movieSelectEvents,
            movieHistoryClickEvents
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { vs ->
                    vs.searchBoxText?.let {
                        ms_mainScreen_searchText.setText(it)
                    }
                    ms_mainScreen_title.text = vs.searchedMovieTitle
                    ms_mainScreen_rating.text = vs.searchedMovieRating

                    vs.searchedMoviePoster
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            Glide.with(ctx)
                                .load(vs.searchedMoviePoster)
                                .placeholder(spinner)
                                .into(ms_mainScreen_poster)
                        } ?: run {
                        ms_mainScreen_poster.setImageResource(0)
                    }

                    listAdapter.submitList(vs.adapterList)
                },
                { Timber.w(it, "something went terribly wrong") }
            )
    }

    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }

    private fun setupListView() {
        val layoutManager = LinearLayoutManager(this, HORIZONTAL, false)
        ms_mainScreen_searchHistory.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(this, HORIZONTAL)
        dividerItemDecoration.setDrawable(
            ContextCompat.getDrawable(this, R.drawable.ms_list_divider_space)!!
        )
        ms_mainScreen_searchHistory.addItemDecoration(dividerItemDecoration)

        listAdapter = MSMovieSearchHistoryAdapter { historyItemClick.onNext(it) }
        ms_mainScreen_searchHistory.adapter = listAdapter
    }

    private fun ImageView.growShrink() {
        val expansionFactor: Float = 0.2F
        val growX = ofFloat(View.SCALE_X, 1f + expansionFactor)
        val growY = ofFloat(View.SCALE_Y, 1f + expansionFactor)
        val growAnimation = ofPropertyValuesHolder(this, growX, growY)
        growAnimation.interpolator = OvershootInterpolator()

        val shrinkX = ofFloat(View.SCALE_X, 1f)
        val shrinkY = ofFloat(View.SCALE_Y, 1f)
        val shrinkAnimation = ofPropertyValuesHolder(this, shrinkX, shrinkY)
        shrinkAnimation.interpolator = OvershootInterpolator()

        val animSetXY = AnimatorSet()
        animSetXY.playSequentially(growAnimation, shrinkAnimation)
        animSetXY.start()
    }
}
