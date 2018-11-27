package co.kaush.msusf.movies

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.CircularProgressDrawable
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutCompat.HORIZONTAL
import android.support.v7.widget.LinearLayoutManager
import co.kaush.msusf.MSActivity
import co.kaush.msusf.R
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
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

    private lateinit var viewModel: MSMainVm
    private lateinit var listAdapter: MSMovieSearchHistoryAdapter

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
        val addToHistoryEvents: Observable<AddToHistoryEvent> = RxView.clicks(ms_mainScreen_poster)
            .map {
                ms_mainScreen_poster.growShrink()
                AddToHistoryEvent
            }
        val restoreFromHistoryEvents: Observable<RestoreFromHistoryEvent> = historyItemClick
            .map { RestoreFromHistoryEvent(it) }

        disposable = viewModel.viewChanges(
            screenLoadEvents,
            searchMovieEvents,
            addToHistoryEvents,
            restoreFromHistoryEvents
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { change ->

                    change.vs.searchBoxText?.let {
                        ms_mainScreen_searchText.setText(it)
                    }
                    ms_mainScreen_title.text = change.vs.searchedMovieTitle
                    ms_mainScreen_rating.text = change.vs.searchedMovieRating

                    change.vs.searchedMoviePoster
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            Glide.with(ctx)
                                .load(change.vs.searchedMoviePoster)
                                .placeholder(spinner)
                                .into(ms_mainScreen_poster)
                        } ?: run {
                        ms_mainScreen_poster.setImageResource(0)
                    }

                    listAdapter.submitList(change.vs.adapterList)
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
}
