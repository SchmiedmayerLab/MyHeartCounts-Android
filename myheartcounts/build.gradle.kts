//
// This source file is part of the My Heart Counts open-source project
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
            (project.findProperty("android.injected.version.name") as? String) ?: "1.0.0"
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
    implementation(project(":core-coroutines"))
    implementation(project(":core-time"))
    implementation(project(":core-viewmodel"))
    implementation(project(":markdown"))
    implementation(project(":ui"))
    implementation(project(":ui-scheduler"))
    implementation(project(":onboarding"))
    implementation(project(":account"))
    implementation(project(":scheduler"))
    implementation(project(":storage-local"))
    implementation(project(":study"))
    implementation(project(":study-definition"))

    implementation(libs.bundles.navigation3)

    testImplementation(project(":testing-screenshot"))
    testImplementation(testFixtures(project(":scheduler")))
    testImplementation(testFixtures(project(":foundation")))
    testImplementation(testFixtures(project(":core-time")))
    testImplementation(testFixtures(project(":study")))
    testImplementation(testFixtures(project(":study-definition")))
}
