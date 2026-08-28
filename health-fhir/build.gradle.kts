//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

plugins {
    alias(libs.plugins.grove.library)
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
        unitTests.all { test ->
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
        }
    }
}

dependencies {
    api(project(":health"))
    api(libs.hl7.fhir.r4)

    testImplementation(libs.androidx.health.connect.testing)
}
