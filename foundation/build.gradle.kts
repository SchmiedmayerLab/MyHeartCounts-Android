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
    namespace = "org.grovealliance.foundation"
    defaultConfig { consumerProguardFiles("consumer-rules.pro") }
    testFixtures { enable = true }
}

dependencies {
    implementation(libs.kotlin.reflect)
}
