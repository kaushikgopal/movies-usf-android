// Top-level build file where you can add configuration options common to all sub-projects/modules.
// TODO: Remove once KTIJ-19369 is fixed
plugins {
    // https://youtrack.jetbrains.com/issue/KT-46200
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.secrets.gradle.plugin) apply false
}
