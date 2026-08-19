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
    namespace = "org.grovealliance.core.lifecycle"
}

dependencies {
    api(libs.bundles.ktx.coroutines)
    api(project(":core"))
    implementation(libs.androidx.lifecycle.process)
}
