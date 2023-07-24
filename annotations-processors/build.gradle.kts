@file:Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed (or agp 8.1)
plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.ksp)
}

dependencies {
  implementation(project(":annotations"))
  implementation(libs.ksp)
  implementation(libs.kotlinpoet.core)
  implementation(libs.kotlinpoet.ksp)
}
