//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.spezi.library)
    alias(libs.plugins.spezi.compose)
    alias(libs.plugins.spezi.serialization)
}

android {
    namespace = "edu.stanford.spezi.markdown"
}

dependencies {
    api(project(":foundation"))
    implementation(project(":ui-theme"))
    api(libs.kotlinx.serialization.json)

    testImplementation(project(":testing-core"))
    testImplementation(project(":ui"))
}
