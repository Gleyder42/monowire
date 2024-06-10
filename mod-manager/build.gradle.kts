plugins {
    id("monowire-kotlin")
    kotlin("plugin.serialization") version "2.0.0"
}

dependencies {
    implementation(project(":common"))
    implementation(project(":persistence"))
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation(testFixtures(project(":common")))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    // https://mvnrepository.com/artifact/org.mockito/mockito-core
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("io.mockk:mockk:1.13.11")
}