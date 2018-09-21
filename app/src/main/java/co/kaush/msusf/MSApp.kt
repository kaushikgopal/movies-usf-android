package co.kaush.msusf

import android.app.Application
import timber.log.Timber



class MSApp : Application() {

    lateinit var appComponent: MSAppComponent

    override fun onCreate() {
        super.onCreate()

        appComponent =
                DaggerMSAppComponent
                        .builder()
                        .mSAppModule(MSAppModule(this))
                        .build()

        Timber.plant(Timber.DebugTree())
    }
}