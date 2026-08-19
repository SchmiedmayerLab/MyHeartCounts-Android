//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.grove.application)
    alias(libs.plugins.grove.compose)
    alias(libs.plugins.grove.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "org.grovealliance.sample.app"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "org.grovealliance.sample.app"
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
