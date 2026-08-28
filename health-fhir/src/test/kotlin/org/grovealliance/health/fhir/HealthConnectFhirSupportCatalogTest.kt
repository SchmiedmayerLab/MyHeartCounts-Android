//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import com.google.common.truth.Truth.assertThat
import org.grovealliance.health.RecordType
import org.junit.Test
import java.io.File

class HealthConnectFhirSupportCatalogTest {
    @Test
    fun `classifies the complete AndroidX 1_1 source inventory exactly once`() {
        assertThat(HealthConnectCatalog.allRecordTypeIdentifiers)
            .containsExactlyElementsIn(RecordType.all.map { it.identifier })
        assertThat(HealthConnectCatalog.allRecordTypeIdentifiers).hasSize(41)
        assertThat(HealthConnectCatalog.supportedRecordTypeIdentifiers).hasSize(40)
        assertThat(HealthConnectCatalog.deferredRecordTypeIdentifiers).hasSize(1)
        assertThat(
            HealthConnectCatalog.supportedRecordTypeIdentifiers.intersect(
                HealthConnectCatalog.deferredRecordTypeIdentifiers,
            ),
        ).isEmpty()
    }

    @Test
    fun `exports the exact implementation capability inventory`() {
        val destination = File(checkNotNull(System.getProperty("grove.capability.export")))
        destination.parentFile?.let { parent ->
            check(parent.isDirectory || parent.mkdirs()) { "Cannot create capability export directory: $parent" }
        }
        destination.writeText(
            capabilityManifest(
                all = HealthConnectCatalog.allRecordTypeIdentifiers,
                supported = HealthConnectCatalog.supportedRecordTypeIdentifiers,
                deferred = HealthConnectCatalog.deferredRecordTypeIdentifiers,
            ),
        )
    }

    private fun capabilityManifest(
        all: Set<String>,
        supported: Set<String>,
        deferred: Set<String>,
    ): String = buildString {
        append("{\n")
        append("  \"schemaVersion\": 1,\n")
        append("  \"sourcePackage\": \"androidx.health.connect:connect-client\",\n")
        append("  \"sourceVersion\": \"")
        append(checkNotNull(System.getProperty("grove.health-connect.version")))
        append("\",\n")
        append("  \"sourceTypeExtension\": \"")
        append(HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION)
        append("\",\n")
        appendArray("allRecordTypes", all)
        append(",\n")
        appendArray("supportedRecordTypes", supported)
        append(",\n")
        appendArray("deferredRecordTypes", deferred)
        append("\n}\n")
    }

    private fun StringBuilder.appendArray(label: String, values: Set<String>) {
        append("  \"")
        append(label)
        append("\": [")
        values.sorted().forEachIndexed { index, value ->
            if (index > 0) append(", ")
            append('"')
            append(value)
            append('"')
        }
        append(']')
    }
}
