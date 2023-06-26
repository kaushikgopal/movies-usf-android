package co.kaush.msusf

import android.app.Application
import co.kaush.msusf.di.AppComponent
import co.kaush.msusf.movies.OpenClassOnDebug
import timber.log.Timber

@OpenClassOnDebug
class MSApp : Application() {

  private val appComponent by lazy(LazyThreadSafetyMode.NONE) { AppComponent.from(this) }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
