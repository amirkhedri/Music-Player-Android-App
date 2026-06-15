// ============================================================
//  PROJECT-LEVEL build.gradle.kts
//  Place this file at:  <project_root>/build.gradle.kts
// ============================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.ksp)                 apply false   // KSP for Room & Hilt
    alias(libs.plugins.hilt)                apply false
}
