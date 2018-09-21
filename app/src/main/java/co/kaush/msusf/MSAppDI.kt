package co.kaush.msusf

import android.content.Context
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MSAppModule(private val app: MSApp) {

    @Provides
    @Singleton
    fun provideContext(): Context = app
}

@Singleton
@Component(modules = arrayOf(MSAppModule::class))
interface MSAppComponent {

    fun inject(activity: MSMainActivity)
}
