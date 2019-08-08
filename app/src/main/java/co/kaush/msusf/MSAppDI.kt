package co.kaush.msusf

import android.content.Context
import co.kaush.msusf.genres.GenreChecklistDemoActivity
import co.kaush.msusf.genres.GenreRepository
import co.kaush.msusf.movies.MovieSearchDemoActivity
import co.kaush.msusf.movies.MovieSearchApi
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class MSAppModule(private val app: MSApp) {

    @Provides
    @Singleton
    fun provideContext(): Context = app


    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {

        val interceptor = (HttpLoggingInterceptor())
                .apply { level = HttpLoggingInterceptor.Level.BODY }

        val okHttpClient: OkHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        return Retrofit.Builder()
                .baseUrl("http://www.omdbapi.com")
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    @Provides
    @Singleton
    fun provideMovieApi(retrofit: Retrofit): MovieSearchApi {
        return retrofit.create(MovieSearchApi::class.java)
    }

    @Provides
    fun provideGenreRepo(): GenreRepository = GenreRepository()
}

@Singleton
@Component(modules = [MSAppModule::class])
interface MSAppComponent {
    fun inject(activity: MovieSearchDemoActivity)
    fun inject(activity: GenreChecklistDemoActivity)
}
