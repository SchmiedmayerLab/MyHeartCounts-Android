//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.spezi.library)
    alias(libs.plugins.spezi.serialization)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "edu.stanford.spezi.study"
    testFixtures { enable = true }
}

dependencies {
    api(project(":core"))
    api(project(":foundation"))
    api(project(":scheduler"))
    api(project(":study-definition"))

    implementation(project(":core-coroutines"))
    implementation(project(":core-logging"))
    implementation(project(":core-time"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.bundles.ktx.coroutines)
    implementation(libs.kotlinx.serialization.json)

    testFixturesImplementation(libs.bundles.ktx.coroutines)
    testFixturesImplementation(testFixtures(project(":foundation")))
    testFixturesImplementation(testFixtures(project(":study-definition")))

    testImplementation(libs.bundles.unit.testing)
    testImplementation(testFixtures(project(":core-time")))
    testImplementation(testFixtures(project(":foundation")))
    testImplementation(testFixtures(project(":scheduler")))
    testImplementation(testFixtures(project(":study-definition")))

    androidTestImplementation(libs.bundles.integration.testing)
    androidTestImplementation(testFixtures(project(":scheduler")))
}
