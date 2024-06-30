plugins {
    id("monowire-kotlin")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":persistence"))
    implementation(project(":nexus-api"))

    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(testFixtures(project(":common")))
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.datatest)
}