plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.android.application)
    alias(libs.plugins.secrets.gradle.plugin)
    alias(libs.plugins.kotlin.allopen)
}

allOpen {
    annotation("co.kaush.msusf.movies.OpenClass")
}

android {
    namespace = "co.kaush.msusf.movies"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "co.kaush.msusf"

        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        versionCode = 2
        versionName = "2.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    kapt(libs.dagger.compiler)
    kapt(libs.lifecycle.compiler)

    implementation(libs.kotlin.stdlib)
    implementation(libs.dagger)
    implementation(libs.constraintlayout)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.picasso)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit.adapter.rxjava)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit)
    implementation(libs.rx.android)
    implementation(libs.rx.bindings)
    implementation(libs.rx.java)
    implementation(libs.rx.replayingShare)
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

    androidTestImplementation(libs.atsl.runner)
    androidTestImplementation(libs.espresso.core)
}
