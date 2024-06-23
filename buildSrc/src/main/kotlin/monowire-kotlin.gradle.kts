plugins {
    `java-library`
    kotlin("jvm")
    id("com.google.devtools.ksp")
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

sourceSets.main {
    kotlin.srcDirs("build/generated/ksp/main/kotlin")
}

kotlin {
    compilerOptions.optIn.add("kotlin.contracts.ExperimentalContracts")
}

// https://github.com/gradle/gradle/issues/15383
val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencies {
    // Every project should have a logger enabled
    implementation(libs.logback)
    testImplementation(libs.logback)

    implementation(libs.arrow.core)
    implementation(libs.arrow.fxCoroutines)

    // Koin is our dependency injection framework
    implementation(libs.koin.core)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.testJunit)
    compileOnly(libs.koin.annotation)
    testCompileOnly(libs.koin.annotation)
    ksp(libs.koin.kspCompiler)

    testImplementation(libs.junit.jupiter.params)
    testFixturesImplementation(libs.junit.jupiter.params)

    testImplementation(libs.assertj.core)
    testFixturesImplementation(libs.assertj.core)

    testImplementation(kotlin("test"))
    testFixturesImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}