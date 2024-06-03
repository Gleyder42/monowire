plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
    idea
}

group = "com.github.gleyder42"
version = "1.0-SNAPSHOT"


idea {
    module {
        isDownloadSources = true
    }
}