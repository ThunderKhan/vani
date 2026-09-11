plugins {
    id("com.android.application")
}

val gitCommit = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { output ->
    output.trim().takeIf { it.matches(Regex("^[0-9a-f]{40}$")) } ?: "0000000"
}.get()

android {
    namespace = "dev.syntax6.vani.m0probe"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.syntax6.vani.m0probe"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0-m1"
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
