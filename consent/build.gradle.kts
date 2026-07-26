//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.spezi.library)
    alias(libs.plugins.spezi.compose)
}

android {
    namespace = "edu.stanford.spezi.consent"
}

dependencies {
    api(project(":core"))
    api(project(":markdown"))
    api(project(":ui"))

    implementation(project(":core-time"))
    implementation(project(":core-viewmodel"))

    testImplementation(project(":testing-core"))
    testImplementation(project(":testing-screenshot"))
}
