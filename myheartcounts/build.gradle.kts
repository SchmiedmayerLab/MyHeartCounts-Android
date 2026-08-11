//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.spezi.application)
    alias(libs.plugins.spezi.compose)
    alias(libs.plugins.spezi.serialization)
}

android {
    namespace = "edu.stanford.myheartcounts"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId =
            (project.findProperty("android.injected.application.id") as? String)
                ?: "edu.stanford.myheartcounts"
        versionCode =
            (project.findProperty("android.injected.version.code") as? String)?.toInt() ?: 1
        versionName =
            (project.findProperty("android.injected.version.name") as? String)
                ?: providers.gradleProperty("app.versionName").get()
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
    implementation(project(":consent"))
    implementation(project(":core"))
    implementation(project(":core-viewmodel"))
    implementation(project(":ui"))
    implementation(project(":onboarding"))
    implementation(project(":account"))
    implementation(project(":storage-local"))

    implementation(libs.bundles.navigation3)

    testImplementation(project(":testing-screenshot"))
}
