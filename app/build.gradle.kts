@file:Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed (or agp 8.1)
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.allopen)
  alias(libs.plugins.google.ksp)
  alias(libs.plugins.secrets.gradle.plugin)
}

allOpen { annotation("co.kaush.msusf.movies.OpenClass") }

android {
  namespace = "co.kaush.msusf.movies"

  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "co.kaush.msusf"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = libs.versions.appVersionCode.get().toInt()
    versionName = libs.versions.appVersionName.get()

    testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    buildConfig = true
    viewBinding = true
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    // after agp 8.1.0-alpha09, this is no longer needed
    // https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  ksp(libs.kotlin.inject.compiler)
  implementation(libs.kotlin.inject.runtime)

  ksp(project(":usf:annotations-processors")) // todo: put all usf in same module
  implementation(project(":usf:annotations"))
  implementation(project(":usf:api"))

  implementation(libs.kotlin.stdlib)
  implementation(libs.androidx.constraintlayout) // todo: move to compose
  implementation(libs.androidx.activity.ktx)
  implementation(libs.flow.binding)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.coil)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.savedstate)
  implementation(platform(libs.square.okhttp.bom))
  implementation(libs.square.okhttp)
  implementation(libs.square.okhttp.logging.interceptor)
  implementation(libs.square.retrofit.gson)
  implementation(libs.square.retrofit)
  implementation(libs.androidx.swiperefreshlayout)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.recyclerview)
  implementation(libs.timber) // todo: remove

  debugImplementation(libs.square.leakcanary)
  releaseImplementation(libs.square.leakcanary.noop)

  testImplementation(libs.google.truth) // todo: remove
  testImplementation(libs.junit)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)
  //  testImplementation(libs.atsl.runner)

  androidTestImplementation(libs.androidx.espresso.core)
}
