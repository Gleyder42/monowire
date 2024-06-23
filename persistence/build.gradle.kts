import app.cash.sqldelight.gradle.VerifyMigrationTask

plugins {
    id("monowire-kotlin")
    alias(libs.plugins.sqldelight)
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

tasks.verifySqlDelightMigration {
    enabled = false
}

tasks.withType<VerifyMigrationTask>() {
    enabled = false
}

dependencies {
    implementation(project(":common"))
    implementation(libs.sqliteJdbc)
    implementation(libs.sqldelight.coroutinesExtensions)
    implementation(libs.sqldelight.sqliteDriver)
}