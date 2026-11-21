plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.irene.pastel"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.irene.pastel"
        minSdk = 30          // Pixel 4a runs Android 13 (API 33)
        targetSdk = 34
        // every GitHub build gets a higher number, so new versions install right over the old one
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// No extra libraries on purpose: plain Android framework only, so it builds clean.
