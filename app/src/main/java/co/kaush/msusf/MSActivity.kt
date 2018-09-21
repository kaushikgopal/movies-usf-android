package co.kaush.msusf

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import co.kaush.msusf.movies.MSMovieApi
import co.kaush.msusf.movies.MSMovieResult
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

abstract class MSActivity : AppCompatActivity() {

    val app: MSApp by lazy { application as MSApp }

    @Inject lateinit var ctx: Context
    @Inject lateinit var movieApi: MSMovieApi

    abstract fun inject(activity: MSActivity)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(this)



        movieApi.searchMovie("blade")
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
                )
    }
}