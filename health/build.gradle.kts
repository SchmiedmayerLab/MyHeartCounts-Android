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
    namespace = "org.grovealliance.health"
}

dependencies {
    api(libs.androidx.health.connect.client)
    api(project(":core"))
    api(project(":ui"))
    implementation(project(":core-coroutines"))
    implementation(project(":core-lifecycle"))
    implementation(project(":storage-local"))
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.androidx.health.connect.testing)
}
