plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.tridivroy.streamly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tridivroy.streamly"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://api.example.com/\"") // change later , this one is dummy url for later
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Shared Kotlin Multiplatform layer (domain models, DTOs, Ktor networking, repositories)
    implementation(project(":shared"))

    // Core & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // WindowSizeClass for Adaptive Layouts
    implementation("androidx.compose.material3:material3-window-size-class")

    // Navigation 3 (Nav3) / Compose Navigation
    implementation("androidx.navigation3:navigation3-runtime:1.1.7")
    implementation("androidx.navigation3:navigation3-ui:1.1.7")
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Ktor is no longer declared here: the client and its engine come from :shared, which pins the
    // version for both platforms. Only the Hilt bridge in SharedBridgeModule still names the type.
    implementation(libs.ktor.client.core)

    // Was arriving transitively through Ktor until :shared took that over. DownloadTracker stores
    // video metadata in each download request with it, and the Nav3 keys are .
    implementation(libs.kotlinx.serialization.json)

    // Media3 (ExoPlayer, HLS, Download)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Image Loading. Coil 3 (the multiplatform line) because :shared uses it too — two Coil majors
    // in one process would mean two independent memory and disk caches for the same thumbnails.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // DataStore Preferences (Session persistence)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}