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
    namespace = "edu.stanford.spezi.studydefinition"
    testFixtures { enable = true }
}

dependencies {
    api(project(":core"))
    api(project(":foundation"))
    api(project(":scheduler"))

    implementation(project(":core-logging"))
    implementation(libs.kotlinx.serialization.json)

    testFixturesImplementation(testFixtures(project(":foundation")))

    testImplementation(libs.bundles.unit.testing)
    testImplementation(testFixtures(project(":foundation")))
}
