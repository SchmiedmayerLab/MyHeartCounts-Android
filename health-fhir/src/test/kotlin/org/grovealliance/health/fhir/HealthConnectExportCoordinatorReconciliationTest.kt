//
// This source file belongs to the My Heart Counts Android project
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
            .isEqualTo("ff3472f3e322b249a045ce1e39fe19a97d843342990f0c47aef249bbc88e4feb")
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
            .isEqualTo("20fe8abbfd9c5954eea5382e2323b5f942f894dd5187c17d073c393ca590ad50")

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
            .isEqualTo("95daabfdc2d020296d90130217489248a854caf8b7349fd4d08f6804fbeadc85")

        coordinator.upsert(stepRecord("fixture-deletion"), conversionTime)
        coordinator.delete("StepsRecord", "fixture-deletion", conversionTime.plusSeconds(2))
        File(exportDirectory, "health-connect-step-deletion-bundle.json")
            .writeText(parser.composeString(sink.batches.last().bundle) + "\n")
        File(wireExportDirectory, "health-connect-step-deletion-bundle.json")
            .writeText(sink.batches.last().bundleJson)
        assertThat(sink.batches.last().payloadSha256)
            .isEqualTo("0d2c0cee731e631a21f1761beddfd2391417f906cbdf32fcdb618fc9cae8aae8")

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
