plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.0-1.0.22")
}