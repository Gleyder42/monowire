plugins {
    id("monowire-kotlin")
}

dependencies {
    implementation(project(":common"))

    testImplementation(libs.kotest.junitRunner)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)

    implementation(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.databind)
}