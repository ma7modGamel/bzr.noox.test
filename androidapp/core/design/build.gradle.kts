plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("app.cash.paparazzi")
}

android {
    namespace = "noox.bzr.design"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        // RTL is mandatory (DEC-047, 43 §13): start/end only.
        error += listOf("RtlHardcoded", "RtlCompat", "RtlEnabled")
        abortOnError = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // XML Views only (DEC-047).
    api("com.google.android.material:material:1.12.0")
    api("androidx.appcompat:appcompat:1.7.0")
    api("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-ktx:1.15.0")

    testImplementation("junit:junit:4.13.2")
}
