//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.formats.IParser
import org.hl7.fhir.r4.formats.JsonParser
import org.junit.Test
import java.io.File
import java.time.Instant

class HealthConnectExportCoordinatorReconciliationTest : HealthConnectExportCoordinatorTestSupport() {
    @Test
    fun `expired-token full read reconciles absence and tolerates deletion replay`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(stepRecord("retained"), conversionTime)
        coordinator.upsert(stepRecord("removed"), conversionTime)
        val constraint = HealthConnectSynchronizationConstraint(
            coordinator = coordinator,
            fullReader = HealthConnectFullReader { listOf(stepRecord("retained")) },
            now = { conversionTime.plusSeconds(30) },
        )

        constraint.onFullyResyncRequired(RecordType.steps)

        assertThat(requireNotNull(journal.entry("StepsRecord", "retained")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)
        assertThat(requireNotNull(journal.entry("StepsRecord", "removed")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)
        val batchCount = sink.batches.size
        constraint.handleDeletedRecords(setOf("removed"), RecordType.steps)
        assertThat(sink.batches).hasSize(batchCount)
    }

    @Test
    fun `an upsert leaving the collection filter invalidates its prior output`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(stepRecord("filtered-record"), conversionTime)
        val constraint = HealthConnectSynchronizationConstraint(
            coordinator = coordinator,
            fullReader = HealthConnectFullReader { emptyList() },
            now = { conversionTime.plusSeconds(1) },
        )

        constraint.handleExcludedRecords(setOf("filtered-record"), RecordType.steps)

        assertThat(requireNotNull(journal.entry("StepsRecord", "filtered-record")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)
        assertThat(sink.batches.last().operation).isEqualTo(HealthConnectExportOperation.RETRACTION)
    }

    @Test
    @Suppress("LongMethod")
    fun `emits deterministic complete conformance fixtures`() = runTest {
        val exportDirectory = File(requireNotNull(System.getProperty("grove.conformance.export")))
        exportDirectory.mkdirs()
        exportDirectory.listFiles()?.forEach { it.delete() }
        val wireExportDirectory = File(requireNotNull(System.getProperty("grove.wire.export")))
        wireExportDirectory.mkdirs()
        wireExportDirectory.listFiles()?.forEach { it.delete() }
        val conversions = completeConformanceRecords().map { (name, record) ->
            name to converter.convert(record, conversionTime)
        }
        val parser = JsonParser().setOutputStyle(IParser.OutputStyle.PRETTY)

        conversions.forEach { (name, conversion) ->
            File(exportDirectory, "health-connect-$name-bundle.json")
                .writeText(parser.composeString(conversion.bundle) + "\n")
            conversion.observations.forEachIndexed { index, observation ->
                File(exportDirectory, "health-connect-$name-observation-${index + 1}.json")
                    .writeText(parser.composeString(observation) + "\n")
            }
            File(exportDirectory, "health-connect-$name-provenance.json")
                .writeText(parser.composeString(requireNotNull(conversion.provenance)) + "\n")
        }

        semanticVectorRecords().forEach { vector ->
            val conversion = converter.convert(vector.record, semanticConversionTime)
            val observation = conversion.observations.single { candidate ->
                candidate.meta.profile.any { it.value == vector.profile }
            }
            File(exportDirectory, "health-connect-semantic-${vector.id}-observation.json")
                .writeText(parser.composeString(observation) + "\n")
        }

        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(heartRateRecord(twoHeartRateSamples()), conversionTime)
        File(wireExportDirectory, "health-connect-heart-rate-upsert-bundle.json")
            .writeText(sink.batches.last().bundleJson)
        assertThat(sink.batches.last().payloadSha256)
            .isEqualTo("bb034e7909dcd78f3a7e0d7077a823bec4f24fa85ef76797ddb9b563243de4ec")
        coordinator.upsert(
            heartRateRecord(
                samples = twoHeartRateSamples().take(1),
                lastModified = Instant.parse("2026-08-19T17:30:02Z"),
            ),
            conversionTime.plusSeconds(1),
        )
        File(exportDirectory, "health-connect-heart-rate-update-bundle.json")
            .writeText(parser.composeString(sink.batches.last().bundle) + "\n")
        File(wireExportDirectory, "health-connect-heart-rate-update-bundle.json")
            .writeText(sink.batches.last().bundleJson)
        assertThat(sink.batches.last().payloadSha256)
            .isEqualTo("c06527d5531d0c42f4630ad1e1a712aad02102cf594d1fc45b2da5106b2311fa")

        coordinator.upsert(
            heartRateRecord(
                samples = emptyList(),
                lastModified = Instant.parse("2026-08-19T17:30:03Z"),
            ),
            conversionTime.plusSeconds(2),
        )
        val zeroOutputRetraction = sink.batches.last()
        File(wireExportDirectory, "health-connect-heart-rate-zero-output-retraction-bundle.json")
            .writeText(zeroOutputRetraction.bundleJson)
        assertThat(zeroOutputRetraction.operation).isEqualTo(HealthConnectExportOperation.RETRACTION)
        assertThat(zeroOutputRetraction.eventSequence.value).isEqualTo("4")
        assertThat(zeroOutputRetraction.wireSourceVersion).isEqualTo("1787160602000000000")
        assertThat(zeroOutputRetraction.bundle.observations()).isEmpty()
        assertThat(zeroOutputRetraction.bundle.entry.map { it.resource.fhirType() })
            .containsExactly("Provenance")
        assertThat(zeroOutputRetraction.payloadSha256)
            .isEqualTo("129afc137d1ce3c54fe50027707d46bacc57e4e3f5e8275d6a6e96469f4405f2")

        coordinator.upsert(stepRecord("fixture-deletion"), conversionTime)
        coordinator.delete("StepsRecord", "fixture-deletion", conversionTime.plusSeconds(2))
        File(exportDirectory, "health-connect-step-deletion-bundle.json")
            .writeText(parser.composeString(sink.batches.last().bundle) + "\n")
        File(wireExportDirectory, "health-connect-step-deletion-bundle.json")
            .writeText(sink.batches.last().bundleJson)
        assertThat(sink.batches.last().payloadSha256)
            .isEqualTo("e4632db5eef66b0810bc8fd641c70865eb9a26d1500cd62086e8d515929a297e")

        assertThat(
            exportDirectory.listFiles()?.map { it.name }
                ?.filter { it.startsWith("health-connect-semantic-") }
                ?.sorted()
                .orEmpty(),
        ).containsExactlyElementsIn(
            semanticVectorRecords().map { "health-connect-semantic-${it.id}-observation.json" }.sorted(),
        ).inOrder()
        assertThat(exportDirectory.listFiles()?.map { it.name }.orEmpty()).hasSize(75)
        assertThat(wireExportDirectory.listFiles()?.map { it.name }.orEmpty()).hasSize(4)
    }
}
