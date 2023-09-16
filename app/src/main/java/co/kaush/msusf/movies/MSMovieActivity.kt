package co.kaush.msusf.movies

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import co.kaush.msusf.di.AppComponent
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.databinding.ActivityMainBinding
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.view.clicks
import timber.log.Timber

class MSMovieActivity : ComponentActivity() {

  private lateinit var movieRepo: MSMovieRepository
  private val viewModel: MSMovieViewModel by viewModels {
    MSMovieViewModel.MSMovieViewModelFactory(movieRepo)
  }

  private lateinit var listAdapter: MSMovieSearchHistoryAdapter
  private lateinit var binding: ActivityMainBinding

  private val historyItemClick = MutableSharedFlow<MSMovie>()

  private val spinner: CircularProgressDrawable by lazy {
    val circularProgressDrawable = CircularProgressDrawable(this)
    circularProgressDrawable.strokeWidth = 5f
    circularProgressDrawable.centerRadius = 30f
    circularProgressDrawable.start()
    circularProgressDrawable
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val appComponent = AppComponent.from(this)
    movieRepo = appComponent.movieRepository

    setupListView()

    binding.longRunningTask.setOnClickListener {
      GlobalScope.launch(Dispatchers.Default) {
        viewModel.processInput(MSMovieEvent.LongRunningEvent)
      }
    }

    viewModel.viewState
        .onEach { render(it) }
        .catch { Timber.e(it, "error rendering view state") }
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .launchIn(lifecycleScope)

    viewModel.effects
        .onEach { trigger(it) }
        .catch { Timber.e(it, "error triggering side effect") }
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .launchIn(lifecycleScope)
  }

  override fun onResume() {
    super.onResume()

    val screenLoadEvents = flowOf(ScreenLoadEvent)

    val searchMovieEvents =
        binding.msMainScreenSearchBtn.clicks().map {
          SearchMovieEvent(binding.msMainScreenSearchText.text.toString())
        }

    val addToHistoryEvents =
        binding.msMainScreenPoster.clicks().map {
          binding.msMainScreenPoster.growShrink()
          AddToHistoryEvent(binding.msMainScreenPoster.getTag(R.id.TAG_MOVIE_DATA) as MSMovie)
        }

    val restoreFromHistoryEvents = historyItemClick.map { RestoreFromHistoryEvent(it) }

    merge(
            screenLoadEvents,
            searchMovieEvents,
            addToHistoryEvents,
            restoreFromHistoryEvents,
        )
        .onEach { viewModel.processInput(it) }
        .catch { Timber.e(it, "error processing input ") }
        .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
        .launchIn(lifecycleScope)
  }

  private fun trigger(effect: MSMovieEffect) {
    Timber.d("----- [trigger] ${Thread.currentThread().name}")
    when (effect) {
      is MSMovieEffect.AddedToHistoryToastEffect -> {
        Toast.makeText(this, "added to history", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun render(vs: MSMovieViewState) {
    Timber.d("----- [render] ${Thread.currentThread().name}")
    vs.searchBoxText.let { binding.msMainScreenSearchText.setText(it) }
    binding.msMainScreenTitle.text = vs.searchedMovieTitle
    binding.msMainScreenRating.text = vs.searchedMovieRating

    vs.searchedMoviePoster
        .takeIf { it.isNotBlank() }
        ?.let {
          binding.msMainScreenPoster.load(vs.searchedMoviePoster) { placeholder(spinner) }

          binding.msMainScreenPoster.setTag(R.id.TAG_MOVIE_DATA, vs.searchedMovieReference)
        }
        ?: run { binding.msMainScreenPoster.setImageResource(0) }

    listAdapter.submitList(vs.adapterList)
  }

  private fun setupListView() {
    val layoutManager = LinearLayoutManager(this, HORIZONTAL, false)
    binding.msMainScreenSearchHistory.layoutManager = layoutManager

    val dividerItemDecoration = DividerItemDecoration(this, HORIZONTAL)
    dividerItemDecoration.setDrawable(
        ContextCompat.getDrawable(this, R.drawable.ms_list_divider_space)!!,
    )
    binding.msMainScreenSearchHistory.addItemDecoration(dividerItemDecoration)

    listAdapter = MSMovieSearchHistoryAdapter {
      lifecycleScope.launch { historyItemClick.emit(it) }
    }
    binding.msMainScreenSearchHistory.adapter = listAdapter
  }
}
