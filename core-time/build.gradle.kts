//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.spezi.library)
}

android {
    namespace = "edu.stanford.spezi.core.time"
    testFixtures { enable = true }
}

dependencies {
    api(project(":core"))

    testFixturesImplementation(project(":foundation"))
    testFixturesImplementation(testFixtures(project(":foundation")))
}
