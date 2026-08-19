//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.grove.library)
    alias(libs.plugins.grove.compose)
    alias(libs.plugins.grove.serialization)
}

android {
    namespace = "org.grovealliance.markdown"
}

dependencies {
    api(project(":foundation"))
    implementation(project(":ui-theme"))
    api(libs.kotlinx.serialization.json)

    testImplementation(project(":testing-core"))
    testImplementation(project(":ui"))
}
