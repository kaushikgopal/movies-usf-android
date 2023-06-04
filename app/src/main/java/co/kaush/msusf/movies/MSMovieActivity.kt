package co.kaush.msusf.movies

import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import co.kaush.msusf.MSActivity
import co.kaush.msusf.movies.MSMainVm.MSMainVmFactory
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.databinding.ActivityMainBinding
import com.jakewharton.rxbinding2.view.RxView
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class MSMovieActivity : MSActivity() {

    @Inject
    lateinit var movieRepo: MSMovieRepository

    private lateinit var viewModel: MSMainVm
    private lateinit var listAdapter: MSMovieSearchHistoryAdapter
    private lateinit var binding: ActivityMainBinding

    private var uiDisposable: Disposable? = null
    private var disposables: CompositeDisposable = CompositeDisposable()
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListView()

        viewModel = ViewModelProviders.of(
            this,
            MSMainVmFactory(app, movieRepo)
        ).get(MSMainVm::class.java)

        disposables.add(
            viewModel
                .viewState()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { Timber.d("----- onNext VS $it") }
                .subscribe(
                    ::render
                ) { Timber.w(it, "something went terribly wrong processing view state") }
        )

        disposables.add(
            viewModel
                .viewEffect()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    ::trigger
                ) { Timber.w(it, "something went terribly wrong processing view effects") }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun trigger(effect: MSMovieViewEffect?) {
        effect ?: return
        when (effect) {
            is MSMovieViewEffect.AddedToHistoryToastEffect -> {
                Toast.makeText(this, "added to history", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun render(vs: MSMovieViewState) {
        vs.searchBoxText?.let {
            binding.msMainScreenSearchText.setText(it)
        }
        binding.msMainScreenTitle.text = vs.searchedMovieTitle
        binding.msMainScreenRating.text = vs.searchedMovieRating

        vs.searchedMoviePoster
            .takeIf { it.isNotBlank() }
            ?.let {
                Picasso.get()
                    .load(vs.searchedMoviePoster)
                    .placeholder(spinner)
                    .into(binding.msMainScreenPoster)

                binding.msMainScreenPoster.setTag(R.id.TAG_MOVIE_DATA, vs.searchedMovieReference)
            }
            ?: run { binding.msMainScreenPoster.setImageResource(0) }

        listAdapter.submitList(vs.adapterList)
    }

    override fun onResume() {
        super.onResume()

        val screenLoadEvents: Observable<ScreenLoadEvent> = Observable.just(ScreenLoadEvent)
        val searchMovieEvents: Observable<SearchMovieEvent> = RxView.clicks(binding.msMainScreenSearchBtn)
            .map { SearchMovieEvent(binding.msMainScreenSearchText.text.toString()) }
        val addToHistoryEvents: Observable<AddToHistoryEvent> = RxView.clicks(binding.msMainScreenPoster)
            .map {
                binding.msMainScreenPoster.growShrink()
                AddToHistoryEvent(binding.msMainScreenPoster.getTag(R.id.TAG_MOVIE_DATA) as MSMovie)
            }
        val restoreFromHistoryEvents: Observable<RestoreFromHistoryEvent> = historyItemClick
            .map { RestoreFromHistoryEvent(it) }

        uiDisposable =
            Observable.merge(
                screenLoadEvents,
                searchMovieEvents,
                addToHistoryEvents,
                restoreFromHistoryEvents
            )
                .subscribe(
                    { viewModel.processInput(it) },
                    { Timber.e(it, "error processing input ") }
                )
    }

    override fun onPause() {
        super.onPause()
        uiDisposable?.dispose()
    }

    private fun setupListView() {
        val layoutManager = LinearLayoutManager(this, HORIZONTAL, false)
        binding.msMainScreenSearchHistory.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(this, HORIZONTAL)
        dividerItemDecoration.setDrawable(
            ContextCompat.getDrawable(this, R.drawable.ms_list_divider_space)!!
        )
        binding.msMainScreenSearchHistory.addItemDecoration(dividerItemDecoration)

        listAdapter = MSMovieSearchHistoryAdapter { historyItemClick.onNext(it) }
        binding.msMainScreenSearchHistory.adapter = listAdapter
    }
}
