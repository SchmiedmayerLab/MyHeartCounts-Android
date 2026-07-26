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
    namespace = "edu.stanford.spezi.testing.core"
}

dependencies {
    api(project(":core"))
    api(project(":core-viewmodel"))
    api(libs.bundles.unit.testing)
}
