//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.mhc.studybundlefixture)
    alias(libs.plugins.grove.library)
    alias(libs.plugins.grove.serialization)
}

android {
    namespace = "org.grovealliance.studydefinition"
    defaultConfig { consumerProguardFiles("consumer-rules.pro") }
    testFixtures { enable = true }
}

dependencies {
    api(project(":core"))
    api(project(":foundation"))
    api(project(":scheduler"))

    implementation(project(":core-logging"))
    implementation(libs.kotlinx.serialization.json)
    // zstd-jni is the standard JVM binding for zstd (the one facebook/zstd references), vendoring
    // the upstream sources. Its Android AAR artifact carries the on-device native libraries; the
    // plain jar carries the desktop ones the JVM unit tests load.
    implementation(variantOf(libs.zstd.jni) { artifactType("aar") })

    testFixturesImplementation(testFixtures(project(":foundation")))

    testImplementation(libs.bundles.unit.testing)
    testImplementation(libs.zstd.jni)
    testImplementation(testFixtures(project(":foundation")))
}
