@file:Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed (or agp 8.1)
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.allopen)
  alias(libs.plugins.ksp)
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

tasks.withType<Test> {
  testLogging {
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    events("skipped", "passed", "failed")
    showStandardStreams = true
  }
}

dependencies {
  ksp(libs.kotlin.inject.compiler)
  implementation(libs.kotlin.inject.runtime)

  ksp(project(":annotations-processors"))
  implementation(project(":annotations"))

  implementation(libs.kotlin.stdlib)
  implementation(libs.constraintlayout)
  implementation(libs.activity.ktx)
  implementation(libs.flowbinding)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.coil)
  implementation(libs.lifecycle.runtime.ktx)
  implementation(libs.lifecycle.viewmodel)
  implementation(libs.lifecycle.viewmodel.ktx)
  implementation(libs.lifecycle.viewmodel.savedstate)
  implementation(platform(libs.okhttp.bom))
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.retrofit.gson)
  implementation(libs.retrofit)
  implementation(libs.swiperefreshlayout)
  implementation(libs.appcompat)
  implementation(libs.recyclerview)
  implementation(libs.timber)

  debugImplementation(libs.leakcanary)
  releaseImplementation(libs.leakcanary.noop)

  testImplementation(libs.google.truth)
  testImplementation(libs.junit)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)
  //  testImplementation(libs.atsl.runner)

  androidTestImplementation(libs.espresso.core)
}
