plugins {
    id("monowire-kotlin")
}

kotlin.compilerOptions {
    freeCompilerArgs.add("-Xcontext-receivers")
}

dependencies {
    implementation(project(":common"))

    testImplementation(libs.kotest.junitRunner)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.jackson)

    testImplementation(testFixtures(project(":common")))
    testImplementation(libs.mockk)

    implementation(libs.jackson.core)
    implementation(libs.jackson.toml)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dateTime)

    implementation(libs.arrow.retrofit)
    implementation(libs.kotlinx.coroutines.core)
}