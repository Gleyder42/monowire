plugins {
    id("monowire-kotlin")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":persistence"))

    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(testFixtures(project(":common")))
    testImplementation(libs.mockk)
}