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
    namespace = "org.grovealliance.account"
}

dependencies {
    api(project(":core"))
    api(project(":foundation"))
    api(project(":ui"))
    api(project(":ui-validation"))
    api(project(":ui-account"))
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.reflect)
    implementation(project(":core-viewmodel"))
    implementation(project(":core-coroutines"))
    implementation(project(":core-lifecycle"))
    implementation(project(":storage-local"))
}
