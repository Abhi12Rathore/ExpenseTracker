plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.expensetrackerpro"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.pebblework.expensetracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Adds en-XA (longer, accented text) and ar-XB, for finding clipped layouts
            isPseudoLocalesEnabled = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        // For BuildConfig.DEBUG, which keeps the developer options out of release builds
        buildConfig = true
    }
    bundle {
        language {
            // Every language ships to every phone: the app's language is chosen in the app, so
            // it can be one the phone itself is not set to
            enableSplit = false
        }
    }
    lint {
        // A string added in English only would show up untranslated in every other language
        error += "MissingTranslation"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Runs Room against a real SQLite on the JVM, for DAO query tests
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material.icons.extended)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    // The backup file format
    implementation(libs.kotlinx.serialization.json)

    // App lock
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.process)
    // Declared explicitly: biometric would otherwise bring in a much older fragment and appcompat
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)

    // Bill scanning: Google's scanner screen and on-device text recognition, both delivered by
    // Google Play services rather than bundled, which keeps the APK small
    implementation(libs.mlkit.document.scanner)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.play.services.base)
    implementation(libs.kotlinx.coroutines.play.services)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

// Compose + Hilt
    implementation(libs.androidx.hilt.navigation.compose)
}