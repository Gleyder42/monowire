plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:1.9.24-1.0.20")
}