//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.spezi.library)
    alias(libs.plugins.spezi.compose)
    alias(libs.plugins.spezi.desugaring)
}

android {
    namespace = "edu.stanford.spezi.questionnaire"

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
        }
    }
}

dependencies {
    api(libs.android.fhir.data.capture)

    implementation(project(":core-coroutines"))

    api(project(":ui"))

    implementation(libs.androidx.fragment.compose)
    androidTestImplementation(project(":testing-ui"))
}

configurations.configureEach {
    resolutionStrategy {
        force(libs.guava)
    }
}
