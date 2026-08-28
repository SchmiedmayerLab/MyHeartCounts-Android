//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.grovealliance.health.RecordType
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier

class HealthConnectFhirSupportCatalogTest {
    @Test
    fun `generates every Health Connect Quantity value domain from the reviewed catalog`() {
        assertThat(HealthConnectContract.quantityValueDomains.keys).containsExactly(
            "body-fat-percentage",
            "flights-climbed",
            "oxygen-saturation",
            "step-count",
            "wheelchair-push-count",
        )
    }

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
    fun `every public field of every supported AndroidX record has a reviewed disposition`() {
        val supported = RecordType.all.filter {
            it.identifier in HealthConnectCatalog.supportedRecordTypeIdentifiers
        }
        supported.forEach { recordType ->
            assertWithMessage("${recordType.identifier} public source fields")
                .that(HealthConnectFieldDispositions.records.getValue(recordType.identifier).keys)
                .containsExactlyElementsIn(publicFields(recordType.type.java))
        }
        assertWithMessage("Metadata public source fields")
            .that(HealthConnectFieldDispositions.metadata.keys)
            .containsExactlyElementsIn(publicFields(Metadata::class.java))
        nestedSourceTypes.forEach { (name, sourceType) ->
            assertWithMessage("$name public source fields")
                .that(HealthConnectFieldDispositions.nested.getValue(name).keys)
                .containsExactlyElementsIn(publicFields(sourceType))
        }
        assertThat(HealthConnectFieldDispositions.nested.keys)
            .containsExactlyElementsIn(nestedSourceTypes.keys)
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
        append("  \"fieldDispositionSourceVersion\": \"")
        append(HealthConnectFieldDispositions.SOURCE_VERSION)
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

    private fun publicFields(type: Class<*>): Set<String> = type.declaredMethods
        .asSequence()
        .filter { method ->
            Modifier.isPublic(method.modifiers) &&
                !Modifier.isStatic(method.modifiers) &&
                method.parameterCount == 0 &&
                method.name.matches(PUBLIC_GETTER)
        }
        .map { method ->
            method.name.removePrefix("get").replaceFirstChar(Char::lowercaseChar)
        }
        .toSet()

    private companion object {
        val PUBLIC_GETTER = Regex("get[A-Z][A-Za-z0-9]*")
        val nestedSourceTypes = mapOf(
            "HeartRateRecord.Sample" to HeartRateRecord.Sample::class.java,
            "CyclingPedalingCadenceRecord.Sample" to CyclingPedalingCadenceRecord.Sample::class.java,
            "PowerRecord.Sample" to PowerRecord.Sample::class.java,
            "SpeedRecord.Sample" to SpeedRecord.Sample::class.java,
            "StepsCadenceRecord.Sample" to StepsCadenceRecord.Sample::class.java,
            "SleepSessionRecord.Stage" to SleepSessionRecord.Stage::class.java,
            "SkinTemperatureRecord.Delta" to SkinTemperatureRecord.Delta::class.java,
            "ExerciseSegment" to ExerciseSegment::class.java,
            "ExerciseLap" to ExerciseLap::class.java,
        )
    }
}
