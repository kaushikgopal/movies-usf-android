package co.kaush.msusf.genres

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import co.kaush.msusf.MSActivity
import co.kaush.msusf.R
import co.kaush.msusf.genres.DemoGenreVM.GenreVmFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_demo_genrechecklist.*
import timber.log.Timber
import javax.inject.Inject

class GenreChecklistDemoActivity : MSActivity() {

    @Inject
    lateinit var genreRepo: GenreRepository

    private lateinit var viewModel: DemoGenreVM
    private lateinit var listAdapter: GenreChecklistAdapter

    private var disposables: CompositeDisposable = CompositeDisposable()

    override fun inject(activity: MSActivity) = app.appComponent.inject(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_genrechecklist)

        setupListView()

        viewModel = ViewModelProviders.of(
                this,
                GenreVmFactory(app, genreRepo)
        ).get(DemoGenreVM::class.java)

        disposables.addAll(
                viewModel
                        .viewState
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(::render) {
                            Timber.e(it, "something went terribly wrong processing view state")
                        },
                viewModel
                        .viewEffects
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(::trigger) {
                            Timber.e(it, "something went terrible wrong processing view effects")
                        }
        )


        viewModel.processInput(GenreEvent.GenreLoadEvent)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun render(vs: GenreViewState) {
        ms_genreScreen_title.text = getString(vs.pageTitle)
        ms_genreScreen_description.text = getString(vs.pageDescription)
        listAdapter.submitList(vs.checkboxListViewState)
    }

    private fun trigger(effect: GenreViewEffect) {
        when (effect) {
            is GenreViewEffect.ToastError ->
                Toast.makeText(this, effect.errMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListView() {
        val layoutManager = LinearLayoutManager(this, VERTICAL, false)
        ms_genreScreen_checklist.layoutManager = layoutManager

        listAdapter = GenreChecklistAdapter(genreRepo) { genre: MSGenre ->
            viewModel.processInput(GenreEvent.GenreToggleEvent(genre))
        }

        ms_genreScreen_checklist.adapter = listAdapter
    }

}

