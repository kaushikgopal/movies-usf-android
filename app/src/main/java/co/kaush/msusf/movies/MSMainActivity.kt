package co.kaush.msusf.movies

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import co.kaush.msusf.MSActivity
import co.kaush.msusf.R
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MSMainActivity : MSActivity() {


    @Inject lateinit var movieRepo: MSMovieRepository

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

        ms_mainScreen_title.text = ctx.resources.getString(R.string.app_name)
    }

    override fun onResume() {
        super.onResume()


        /*movieApi.searchMovie("blade")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {

                            it.body()?.let {
                                Timber.d("-------- movie search results ${it}")
                            }

                            it.errorBody()?.let { body ->
                                val errorResponse: MSMovieResult = Gson()
                                        .fromJson(body.string(), MSMovieResult::class.java)
                                Timber.d("-------- movie search result error $errorResponse")
                            }
                        },
                        {
                            Timber.e(it, "-------- Something went wrong")
                        }
                )*/

    }

    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }
}
