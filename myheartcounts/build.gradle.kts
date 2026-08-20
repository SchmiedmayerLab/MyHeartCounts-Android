//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.mhc.studybundle)
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.grove.application)
    alias(libs.plugins.grove.compose)
    alias(libs.plugins.grove.serialization)
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
            isShrinkResources = true
            isMinifyEnabled = true
        }
        debug {
            isMinifyEnabled = false
            // Offline-instrumented classes cannot be read back by the Jacoco report task, which
            // reads the same class directories.
            enableAndroidTestCoverage = false
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

    androidTestImplementation(libs.bundles.integration.testing)
    androidTestImplementation(testFixtures(project(":study")))

    testImplementation(project(":testing-screenshot"))
    // The bundle archive is unpacked in a unit test, which needs the desktop native libraries the
    // plain jar carries rather than the on-device ones in the AAR.
    testImplementation(libs.zstd.jni)
    testImplementation(testFixtures(project(":scheduler")))
    testImplementation(testFixtures(project(":foundation")))
    testImplementation(testFixtures(project(":core-time")))
    testImplementation(testFixtures(project(":study")))
    testImplementation(testFixtures(project(":study-definition")))
}
