//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.spezi.library)
    alias(libs.plugins.spezi.serialization)
}

android {
    namespace = "edu.stanford.spezi.storage.local"
}

dependencies {
    api(project(":core"))
    implementation(project(":core-coroutines"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.security.crypto.ktx)
    androidTestImplementation(libs.bundles.integration.testing)
}
