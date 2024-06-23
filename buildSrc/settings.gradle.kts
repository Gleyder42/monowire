dependencyResolutionManagement {
    // https://github.com/gradle/gradle/issues/15383
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}