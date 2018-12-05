package co.kaush.msusf.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import co.kaush.msusf.MSActivity
import co.kaush.msusf.R
import com.airbnb.mvrx.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_main.*
import javax.inject.Inject

class MsMovieFragment : BaseMvRxFragment() {

    private val viewModel: MSMainVm by fragmentViewModel()

    private lateinit var listAdapter: MSMovieSearchHistoryAdapter
    private val spinner: CircularProgressDrawable by lazy {
        CircularProgressDrawable(requireContext()).apply {
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_main, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ms_mainScreen_searchBtn.setOnClickListener {
            viewModel.searchMovie(ms_mainScreen_searchText.text.toString())
        }

        ms_mainScreen_poster.setOnClickListener {
            ms_mainScreen_poster.growShrink()
            viewModel.addToHistory()
        }

        setupListView { viewModel.restoreFromHistory(it) }

        viewModel.selectSubscribe(MSMovieViewState::addedToHistory, uniqueOnly = true) { showToast ->
            if (showToast) {
                Toast.makeText(requireContext(), "added to history", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun invalidate() {
        withState(viewModel) { vs ->
            vs.searchBoxText?.let(ms_mainScreen_searchText::setText)

            ms_mainScreen_title.text = when (vs.searchMovieRequest) {
                is Loading -> "Searching Movie..."
                is Success -> vs.searchMovieRequest()?.title
                is Fail -> vs.searchMovieRequest.error.message
                else -> null
            }

            val movie = vs.searchMovieRequest()

            ms_mainScreen_rating.text = movie?.ratingSummary

            movie?.posterUrl
                .takeUnless { it.isNullOrBlank() }
                ?.let {
                    Picasso.get()
                        .load(it)
                        .placeholder(spinner)
                        .into(ms_mainScreen_poster)
                } ?: run {
                ms_mainScreen_poster.setImageResource(0)
            }

            listAdapter.submitList(vs.adapterList)
        }
    }

    private fun setupListView(historyClickListener: (MSMovie) -> Unit) {
        val layoutManager = LinearLayoutManager(requireContext(), HORIZONTAL, false)
        ms_mainScreen_searchHistory.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(requireContext(), HORIZONTAL)
        dividerItemDecoration.setDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.ms_list_divider_space)!!
        )
        ms_mainScreen_searchHistory.addItemDecoration(dividerItemDecoration)

        listAdapter = MSMovieSearchHistoryAdapter(historyClickListener)
        ms_mainScreen_searchHistory.adapter = listAdapter
    }
}

class MSMovieActivity : MSActivity() {

    @Inject
    lateinit var movieRepo: MSMovieRepository

    override fun inject(activity: MSActivity) {
        app.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager
            .beginTransaction()
            .apply { add(R.id.fragment_host, MsMovieFragment()) }
            .commit()
    }
}
