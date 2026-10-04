import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("app.cash.paparazzi")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}

val mobileApiBaseUrl = providers.gradleProperty("BZR_API_BASE_URL")
    .orElse(providers.environmentVariable("BZR_API_BASE_URL"))
    .orElse("https://dg.dnbscy.com/api/v1/")
val mapsAndroidApiKey = providers.gradleProperty("MAPS_ANDROID_API_KEY")
    .orElse(providers.environmentVariable("MAPS_ANDROID_API_KEY"))
    .orElse(providers.provider { localProperties.getProperty("MAPS_ANDROID_API_KEY", "") })
    .orElse("")
// DEC-058 — App Links host for `https://{APP_DOMAIN}/app/*`. Set per environment once DEP-ID-01 fixes the domain.
val appLinkHost = providers.gradleProperty("BZR_APP_LINK_HOST")
    .orElse(providers.environmentVariable("BZR_APP_LINK_HOST"))
    .orElse("dg.dnbscy.com")

android {
    namespace = "noox.bzr.gallery"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bremo.app" // DEC-048
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", "\"${mobileApiBaseUrl.get()}\"")
        manifestPlaceholders["MAPS_ANDROID_API_KEY"] = mapsAndroidApiKey.get()
        manifestPlaceholders["APP_LINK_HOST"] = appLinkHost.get()
    }

    buildFeatures {
        buildConfig = true
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
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:design"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("com.google.firebase:firebase-crashlytics:20.1.1")
    implementation("com.google.firebase:firebase-messaging:25.0.1") // DEC-058
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    // Network: Retrofit over OkHttp, with request/response and connection logging in debug builds.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

// Optional filter while converting screens: -Pbzr.screens=SCR-C01,SCR-C02 (all screens when absent).
tasks.withType<Test>().configureEach {
    providers.gradleProperty("bzr.screens").orNull?.let { systemProperty("bzr.screens", it) }
}
