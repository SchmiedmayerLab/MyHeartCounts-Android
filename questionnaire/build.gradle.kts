//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.grove.library)
    alias(libs.plugins.grove.compose)
    alias(libs.plugins.grove.desugaring)
}

android {
    namespace = "org.grovealliance.questionnaire"

    buildTypes {
        debug {
            // JaCoCo cannot instrument the HAPI FHIR 6.0.22 jars: mergeExtDex fails with
            // "Execution failed for JacocoTransform". Re-enable once HAPI FHIR is upgraded.
            enableAndroidTestCoverage = false
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
