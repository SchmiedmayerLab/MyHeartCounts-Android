//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.grove.library)
}

android {
    namespace = "org.grovealliance.firebase"
}

dependencies {
    api(project(":core"))
    api(libs.firebase.common)

    implementation(project(":core-coroutines"))
    implementation(project(":core-logging"))

    testImplementation(project(":testing-core"))
}
