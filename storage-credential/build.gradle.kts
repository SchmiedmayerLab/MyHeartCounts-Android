//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.grove.library)
    alias(libs.plugins.grove.serialization)
}

android {
    namespace = "org.grovealliance.storage.credential"
}

dependencies {
    api(project(":core"))

    implementation(project(":storage-local"))
    androidTestImplementation(libs.bundles.integration.testing)
}
