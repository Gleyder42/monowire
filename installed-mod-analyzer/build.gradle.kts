plugins {
    id("monowire-kotlin")
}

val kotest = "5.9.1"

dependencies {
    implementation(project(":common"))
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // https://mvnrepository.com/artifact/org.mockito/mockito-core
    testImplementation("org.mockito:mockito-core:5.12.0")
    testFixturesImplementation("org.mockito:mockito-core:5.12.0")

    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation("io.kotest:kotest-assertions-core:$kotest")
    testImplementation("io.kotest:kotest-property:$kotest")

    implementation(platform("com.fasterxml.jackson:jackson-bom:2.17.1"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}