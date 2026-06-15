// ============================================================
//  APP-LEVEL build.gradle.kts
//  Place this file at:  <project_root>/app/build.gradle.kts
// ============================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace   = "com.example.musicplayer"
    compileSdk  = 35

    defaultConfig {
        applicationId  = "com.example.musicplayer"
        minSdk         = 26          // Oreo — safe for MediaStore audio & Media3
        targetSdk      = 35
        versionCode    = 1
        versionName    = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema exports (useful for migration testing)
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental",    "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // Allow Room schema directory to be exported
    sourceSets {
        getByName("main").assets.srcDirs("$projectDir/schemas")
    }
}

dependencies {
    // ── Core ──────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)

    // ── Compose BOM (keeps all Compose versions in sync) ──
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // ── Navigation ────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Room ──────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)              // KSP annotation processor

    // ── Hilt ──────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)              // KSP annotation processor
    implementation(libs.hilt.navigation.compose)

    // ── Media3 / ExoPlayer ────────────────────────────────
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)

    // ── Coil (album art image loading) ────────────────────
    implementation(libs.coil.compose)

    // ── Test ──────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
