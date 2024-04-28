@file:Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed (or agp 8.1)
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.google.ksp)
}

dependencies {
  implementation(project(":annotations"))
  implementation(libs.google.ksp)
  implementation(libs.square.kotlinpoet.core)
  implementation(libs.square.kotlinpoet.ksp)
}
