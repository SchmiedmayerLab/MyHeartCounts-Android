//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.spezi.application)
    alias(libs.plugins.spezi.compose)
    alias(libs.plugins.spezi.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "edu.stanford.spezi.sample.app"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "edu.stanford.spezi.sample.app"
        versionCode = 1
        versionName = "1.0.0"
        targetSdk = libs.versions.targetSdk.get().toInt()

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-viewmodel"))
    implementation(project(":core-coroutines"))
    implementation(project(":health"))
    implementation(project(":ui"))
    implementation(project(":account"))

    implementation(libs.bundles.navigation3)

    androidTestImplementation(project(":testing-ui"))
}
