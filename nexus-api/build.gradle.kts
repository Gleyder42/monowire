plugins {
    id("monowire-kotlin")
}

val kotest = "5.9.1"
val retrofit = "2.11.0"

dependencies {
    implementation(project(":common"))
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation ("io.kotest:kotest-assertions-core:$kotest")
    testImplementation ("io.kotest:kotest-property:$kotest")

    implementation("com.squareup.retrofit2:retrofit:$retrofit")
    implementation("com.squareup.retrofit2:converter-jackson:$retrofit")

    testImplementation(testFixtures(project(":common")))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    // https://mvnrepository.com/artifact/org.mockito/mockito-core
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("io.mockk:mockk:1.13.11")

    implementation(platform("com.fasterxml.jackson:jackson-bom:2.17.1"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation(platform("org.http4k:http4k-bom:5.23.0.0"))
    implementation("org.http4k:http4k-core")
}