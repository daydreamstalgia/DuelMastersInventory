plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "ro.ddnostalgia.duelmastersinventory"
    compileSdk = 34

    defaultConfig {
        applicationId = "ro.ddnostalgia.duelmastersinventory"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // This environment's NDK has no working `strip` tool, so the OpenCV AAR's native
            // libs (see decisions/0009-orb-feature-matching.md) ship unstripped across all four
            // ABIs at once on a local assembleDebug, producing a ~560MB debug APK. Restricting
            // debug installs to the one ABI local test devices/emulators actually use keeps
            // `installDebug` usable; a real Play-bound AAB is unaffected either way.
            ndk {
                abiFilters += "arm64-v8a"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/INDEX.LIST",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/DEPENDENCIES"
            )
        }
    }
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.exifinterface:exifinterface:1.3.7")


    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Preferences (theme mode, dynamic color, grid density)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // CameraX (card scan)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ORB local-feature card matching (card scan) - see decisions/0009-orb-feature-matching.md
    implementation("org.opencv:opencv:5.0.0.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // JSON for set-pack data.json / Discover catalog parsing (see "dynamic sets" spec)
    implementation("com.google.code.gson:gson:2.10.1")

    // Semantic search (on-device MiniLM embedding model)
    implementation("org.tensorflow:tensorflow-lite:2.17.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Google API Client for Android
    implementation("com.google.api-client:google-api-client-android:1.34.0")

    // Google Sheets API
    //implementation 'com.google.apis:google-api-services-sheets:v4-rev20230823-2.0.0'

    // JSON parser for Sheets
    implementation("com.google.http-client:google-http-client-gson:1.42.0")
    // Google Sheets API (v4)
    implementation("com.google.apis:google-api-services-sheets:v4-rev20250616-2.0.0")

    // Lifecycle service
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}