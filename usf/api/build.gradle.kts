@file:Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed (or agp 8.1)
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    // (Required) Writing and executing Unit Tests on the JUnit Platform
    testImplementation(libs.testing.junit5.api)
    testRuntimeOnly(libs.testing.junit5.engine)

    testImplementation(libs.testing.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)

    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.testing.junit5.api)
}
