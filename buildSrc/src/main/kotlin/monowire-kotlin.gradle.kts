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

dependencies {
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-core
    implementation("ch.qos.logback:logback-classic:1.5.6")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")

    implementation(platform("io.insert-koin:koin-bom:3.5.6"))
    implementation("io.insert-koin:koin-core")

    implementation(platform("io.insert-koin:koin-annotations-bom:1.3.1"))
    compileOnly("io.insert-koin:koin-annotations:1.3.1")
    ksp("io.insert-koin:koin-ksp-compiler:1.3.1")

    testImplementation(platform("io.insert-koin:koin-annotations-bom:1.3.1"))
    testCompileOnly("io.insert-koin:koin-annotations:1.3.1")

    testImplementation(kotlin("test"))
    testImplementation("io.insert-koin:koin-test")
    testImplementation("io.insert-koin:koin-test-junit5")

    testImplementation("com.google.jimfs:jimfs:1.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.1")

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testFixturesImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}