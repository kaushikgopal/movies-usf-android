package co.kaush.msusf.di

import android.content.Context
import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieApi
import co.kaush.msusf.movies.MSMovieRepository
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** These are components that you'll need in both Test and Prod */
abstract class CommonAppComponent {
  abstract val movieRepository: MSMovieRepository
}

@AppScope
@Component
abstract class AppComponent(
    @get:Provides val app: MSApp,
) : CommonAppComponent() {

  @AppScope
  @Provides
  protected fun provideRetrofit(): Retrofit {
    val interceptor = (HttpLoggingInterceptor()).apply { level = HttpLoggingInterceptor.Level.BODY }

    val okHttpClient: OkHttpClient =
        OkHttpClient.Builder().addNetworkInterceptor(interceptor).build()

    return Retrofit.Builder()
        .baseUrl("http://www.omdbapi.com")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
  }

  @AppScope
  @Provides
  fun provideMovieApi(retrofit: Retrofit): MSMovieApi {
    return retrofit.create(MSMovieApi::class.java)
  }

  companion object {
    private var instance: AppComponent? = null

    fun from(context: Context): AppComponent =
        instance
            ?: AppComponent::class.create(context.applicationContext as MSApp).also {
              instance = it
            }
  }
}
