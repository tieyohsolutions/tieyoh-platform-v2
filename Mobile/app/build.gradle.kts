import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Google Maps API key, read from local.properties (git-ignored) so it never
// gets committed. Add MAPS_API_KEY=<your key> there -- see the map feature's
// commit message for how to get one. Left blank, the manifest placeholder
// below resolves to an empty string and the map screen renders with no
// tiles (a gray grid) rather than failing to build.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.tieyoh.mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tieyoh.mobile"
        minSdk = 24
        targetSdk = 37
        // App version -- the single source of truth (REQ-002). Shown to
        // users as "Version <versionName>" on the Profile screen via
        // BuildConfig.VERSION_NAME, and by Android under App info.
        // Format MAJOR.MINOR. Bump ONLY when Hany explicitly says
        // "minor" (right number +1) or "major" (left number +1, right
        // resets to 0). versionCode must go up by 1 on every bump and
        // never go down.
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // Generates BuildConfig.VERSION_NAME, read by AppVersion.kt.
        buildConfig = true
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    // Add to Google Wallet -- PayClient/Pay.getClient() below in
    // GoogleWalletHelper.kt. Verify this is still the latest
    // play-services-pay release in Android Studio; it can't be checked
    // against Maven Central from this sandbox.
    implementation("com.google.android.gms:play-services-pay:16.5.0")
    implementation("com.google.maps.android:maps-compose:6.4.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}