plugins {
    id("monowire-kotlin")
    kotlin("plugin.serialization") version "2.0.0"
    id("app.cash.sqldelight") version "2.0.2"
}

repositories {
    google()
}


sqldelight {
    databases {
        create("Database") {
            packageName.set("com.github.gleyder42.monowire.persistence")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.2")
            generateAsync.set(true)
        }
    }
}

dependencies {
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
    implementation(project(":common"))
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}