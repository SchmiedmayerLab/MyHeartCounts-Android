//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.grove.library)
    alias(libs.plugins.google.devtools.ksp)
}

ksp {
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.absolutePath)
}

android {
    namespace = "org.grovealliance.health.fhir"

    buildTypes {
        debug {
            // The FHIR reference implementation jars cannot be instrumented by JaCoCo.
            enableAndroidTestCoverage = false
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            test.extensions.configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
                // The reference implementation ships parser classes beyond JaCoCo's JVM class
                // size ceiling. They are a dependency, not producer code, so do not transform them.
                excludes = (excludes.orEmpty() + "org.hl7.fhir.*").distinct()
            }
            val configuredExport = System.getenv("GROVE_CONFORMANCE_EXPORT")
            val defaultExport = layout.buildDirectory.dir("conformance-fixtures").get().asFile.absolutePath
            test.systemProperty("grove.conformance.export", configuredExport ?: defaultExport)
            val configuredWireExport = System.getenv("GROVE_WIRE_EXPORT")
            val defaultWireExport = layout.buildDirectory.dir("wire-fixtures").get().asFile.absolutePath
            test.systemProperty("grove.wire.export", configuredWireExport ?: defaultWireExport)
            val configuredCapabilityExport = System.getenv("GROVE_CAPABILITY_EXPORT")
            val defaultCapabilityExport = layout.buildDirectory
                .file("producer-capabilities/health-connect.json")
                .get()
                .asFile
                .absolutePath
            test.systemProperty(
                "grove.capability.export",
                configuredCapabilityExport ?: defaultCapabilityExport,
            )
            test.systemProperty(
                "grove.health-connect.version",
                libs.versions.healthConnectClient.get(),
            )
            System.getenv("GROVE_EXCHANGE_PROTOCOL_CATALOG")?.let { catalog ->
                test.systemProperty("grove.exchange-protocol.catalog", catalog)
            }
            System.getenv("GROVE_MOBILE_EXCHANGE_CORPUS_DIRECTORY")?.let { corpusDirectory ->
                test.systemProperty("grove.mobile-exchange.corpus-directory", corpusDirectory)
            }
        }
    }
}

dependencies {
    api(project(":health"))
    api(libs.hl7.fhir.r4)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.bundles.ktx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)

    // connect-testing currently declares a 1.2 alpha client transitively. Keep the FHIR producer's
    // tests on the exact stable 1.1.0 API that the library compiles and claims in its capability
    // manifest, so an alpha-only field cannot silently enter the supported source contract.
    testImplementation(libs.androidx.health.connect.testing) {
        exclude(group = "androidx.health.connect", module = "connect-client")
        exclude(group = "androidx.health.connect", module = "connect-client-proto")
        exclude(group = "androidx.health.connect", module = "connect-client-external-protobuf")
    }
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
