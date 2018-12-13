package co.kaush.msusf

import android.app.Application
import co.kaush.msusf.movies.OpenClassOnDebug
import com.squareup.leakcanary.LeakCanary
import timber.log.Timber


@OpenClassOnDebug
class MSApp : Application() {

    lateinit var appComponent: MSAppComponent

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        LeakCanary.install(this);

        appComponent =
                DaggerMSAppComponent
                        .builder()
                        .mSAppModule(MSAppModule(this))
                        .build()

        Timber.plant(Timber.DebugTree())
    }
}