//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.grove.library)
    alias(libs.plugins.grove.compose)
}

android {
    namespace = "org.grovealliance.ui"
}

dependencies {

    api(project(":resources"))
    api(project(":ui-theme"))
    api(project(":core-coroutines"))

    implementation(project(":foundation"))
    androidTestImplementation(project(":testing-ui"))
}
