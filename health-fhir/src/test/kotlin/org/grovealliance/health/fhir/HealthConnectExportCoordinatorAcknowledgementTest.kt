//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResearchStudy
import org.junit.Test

class HealthConnectExportCoordinatorAcknowledgementTest : HealthConnectExportCoordinatorTestSupport() {
    @Test
    fun `negative acknowledgement retains the prior journal state for retry`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(stepRecord("step-record"), conversionTime)
        sink.failNext = true

        val failure = runCatching {
            coordinator.delete("StepsRecord", "step-record", conversionTime.plusSeconds(1))
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)

        coordinator.delete("StepsRecord", "step-record", conversionTime.plusSeconds(1))
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)
    }

    @Test
    fun `incomplete acknowledgement retains the exact pending upsert`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink().apply { omitNextAcknowledgement = true }
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val record = stepRecord("step-record")

        val failure = runCatching { coordinator.upsert(record, conversionTime) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(journal.entry("StepsRecord", "step-record")).isNull()
        val pending = requireNotNull(journal.pending("StepsRecord", "step-record"))

        coordinator.upsert(record, conversionTime.plusSeconds(20))

        assertThat(sink.batches.last().eventSequence).isEqualTo(pending.eventSequence)
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).destinationReferences)
            .isNotEmpty()
    }

    @Test
    fun `an adapter-specific output reaches the sink through the active export gate`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)

        coordinator.upsert(basalMetabolicRateRecord("adapter-specific-output"), conversionTime)

        val observation = sink.batches.single().bundle.observations().single()
        assertThat(observation.meta.profile.map { it.value })
            .containsExactly(HealthConnectContract.HEALTH_CONNECT_BASAL_METABOLIC_RATE_PROFILE)
        assertThat(
            observation.getExtensionsByUrl(HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION),
        ).hasSize(1)
        assertThat(
            requireNotNull(journal.entry("BasalMetabolicRateRecord", "adapter-specific-output")).state,
        ).isEqualTo(HealthConnectExportState.ACTIVE)
    }

    @Test
    fun `journal snapshots destination references returned by a mutable sink`() = runTest {
        val journal = InMemoryJournal()
        val mutableReferences = mutableMapOf<FhirIdentifierKey, String>()
        val coordinator = HealthConnectExportCoordinator(
            converter,
            journal,
            HealthConnectExportSink { batch ->
                batch.bundle.groveOutputIdentifiers().forEach { output ->
                    mutableReferences[output.key()] = "Resource/server-id"
                }
                HealthConnectExportAcknowledgement(mutableReferences)
            },
        )

        coordinator.upsert(stepRecord("step-record"), conversionTime)
        mutableReferences.clear()

        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).destinationReferences)
            .isNotEmpty()
    }

    @Test
    fun `sink acknowledgement snapshots its mutable destination map`() {
        val mutableReferences = mutableMapOf(
            FhirIdentifierKey(TEST_EVENT_SYSTEM, "source-output") to "Observation/server-id",
        )
        val acknowledgement = HealthConnectExportAcknowledgement(mutableReferences)

        mutableReferences.clear()
        val returned = acknowledgement.destinationReferences.toMutableMap()
        returned.clear()

        assertThat(acknowledgement.destinationReferences).containsExactly(
            FhirIdentifierKey(TEST_EVENT_SYSTEM, "source-output"),
            "Observation/server-id",
        )
    }

    @Test
    fun `export batch rejects a Bundle that differs from its authoritative JSON`() {
        val conversion = converter.convert(stepRecord("step-record"), conversionTime)
        val bundleJson = HealthConnectWireFormat.bundleJson(conversion.bundle)
        val changedBundle = conversion.bundle.apply {
            timestampElement = org.hl7.fhir.r4.model.InstantType(conversionTime.plusSeconds(1).toString())
        }

        val failure = runCatching {
            HealthConnectExportBatch(
                eventSequence = EventSequence("1"),
                operation = HealthConnectExportOperation.ACTIVE,
                sourceRecordIdentifier = conversion.sourceRecordIdentifier,
                sourceVersion = conversion.sourceLastModified,
                bundle = changedBundle,
                bundleJson = bundleJson,
                payloadSha256 = HealthConnectWireFormat.sha256(bundleJson),
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `export batch snapshots mutable HAPI inputs and returns defensive copies`() {
        val conversion = converter.convert(
            stepRecord("immutable-batch"),
            conversionTime,
            EventSequence("1"),
        )
        val sourceRecordIdentifier = conversion.sourceRecordIdentifier
        val bundle = conversion.bundle
        val expectedSourceValue = sourceRecordIdentifier.value
        val bundleJson = HealthConnectWireFormat.bundleJson(bundle)
        val payloadSha256 = HealthConnectWireFormat.sha256(bundleJson)
        val batch = HealthConnectExportBatch(
            eventSequence = EventSequence("1"),
            operation = HealthConnectExportOperation.ACTIVE,
            sourceRecordIdentifier = sourceRecordIdentifier,
            sourceVersion = conversion.sourceLastModified,
            bundle = bundle,
            bundleJson = bundleJson,
            payloadSha256 = payloadSha256,
        )

        sourceRecordIdentifier.value = "mutated-constructor-input"
        bundle.timestampElement = InstantType(conversionTime.plusSeconds(1).toString())
        batch.sourceRecordIdentifier.value = "mutated-getter-copy"
        batch.bundle.timestampElement = InstantType(conversionTime.plusSeconds(2).toString())

        assertThat(batch.sourceRecordIdentifier.value).isEqualTo(expectedSourceValue)
        assertThat(HealthConnectWireFormat.bundleJson(batch.bundle)).isEqualTo(bundleJson)
        assertThat(batch.bundleJson).isEqualTo(bundleJson)
        assertThat(batch.payloadSha256).isEqualTo(payloadSha256)
    }

    @Test
    fun `journal debug representations redact native Health Connect ids`() {
        val rawId = "raw-secret-health-connect-id"
        val lease = HealthConnectSourceTransitionLease(
            repositoryScopeKey = synchronizationScope.repositoryScopeKey,
            recordType = "StepsRecord",
            healthConnectId = rawId,
            fence = HealthConnectJournalFence("1"),
        )
        val unmatchedDeletion = HealthConnectUnmatchedDeletion(
            repositoryScopeKey = synchronizationScope.repositoryScopeKey,
            projectionScopeKey = synchronizationScope.projectionScopeKey,
            recordType = "StepsRecord",
            healthConnectId = rawId,
            observedAt = conversionTime,
        )
        val rejectedRecord = HealthConnectRejectedRecord(
            repositoryScopeKey = synchronizationScope.repositoryScopeKey,
            projectionScopeKey = synchronizationScope.projectionScopeKey,
            recordType = "StepsRecord",
            healthConnectId = rawId,
            sourceLastModified = conversionTime,
            observedAt = conversionTime.plusSeconds(1),
            reason = "conversion failed",
        )

        listOf(lease, unmatchedDeletion, rejectedRecord).forEach { value ->
            assertThat(value.toString()).doesNotContain(rawId)
            assertThat(value.toString()).contains("<redacted>")
        }
    }

    @Test
    fun `retraction targets derive their Identifier role and close their resource types`() {
        val identifier = FhirIdentifierKey(TEST_EVENT_SYSTEM, "document-output")

        val admitted = HealthConnectRetractionTarget(
            identifier,
            "DocumentReference",
            HealthConnectRetractionTargetRole.SOURCE_ARTIFACT,
        )
        val wrongResourceType = runCatching {
            HealthConnectRetractionTarget(
                identifier,
                "Observation",
                HealthConnectRetractionTargetRole.SOURCE_ARTIFACT,
            )
        }.exceptionOrNull()

        assertThat(admitted.identifierRole).isEqualTo(GroveIdentifierRole.SOURCE_OUTPUT)
        assertThat(wrongResourceType).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `DocumentReference deletion retracts its source-output identity as a source artifact`() = runTest {
        val prior = documentJournalEntry("document-to-delete")
        val document = prior.bundle.entry.mapNotNull { it.resource as? DocumentReference }.single()
        val expectedIdentifier = document.typedGroveIdentifiers("DocumentReference test output")
            .getValue(GroveIdentifierRole.SOURCE_OUTPUT)
            .key()
        val journal = InMemoryJournal(startingSequence = 2).apply { storeLocal(prior) }
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)

        coordinator.delete("StepsRecord", "document-to-delete", conversionTime.plusSeconds(1))

        val target = sink.batches.single().retractedTargets.single {
            it.role == HealthConnectRetractionTargetRole.SOURCE_ARTIFACT
        }
        assertThat(target.identifier).isEqualTo(expectedIdentifier)
        assertThat(target.identifierRole).isEqualTo(GroveIdentifierRole.SOURCE_OUTPUT)
        assertThat(target.resourceType).isEqualTo("DocumentReference")
    }

    @Test
    fun `active DocumentReference rejects source-artifact as its selected entry key`() {
        val prior = documentJournalEntry("wrong-document-entry-key")
        val bundle = prior.bundle
        val documentEntry = bundle.entry.single { it.resource is DocumentReference }
        val document = documentEntry.resource as DocumentReference
        val sourceArtifact = document.typedGroveIdentifiers("DocumentReference test output")
            .getValue(GroveIdentifierRole.SOURCE_ARTIFACT)
        val oldFullUrl = documentEntry.fullUrl
        val newFullUrl = GroveExchangeIdentity.fullUrl(sourceArtifact)
        (documentEntry.extension.single {
            it.url == GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION
        }).value = sourceArtifact.copy()
        documentEntry.fullUrl = newFullUrl
        bundle.entry.flatMap { it.resource.groveReferenceNodes() }
            .filter { it.reference == oldFullUrl }
            .forEach { it.reference = newFullUrl }
        val bundleJson = HealthConnectWireFormat.bundleJson(bundle)

        val failure = runCatching {
            HealthConnectExportBatch(
                eventSequence = EventSequence("1"),
                operation = HealthConnectExportOperation.ACTIVE,
                sourceRecordIdentifier = prior.sourceRecordIdentifier,
                sourceVersion = prior.sourceLastModified,
                bundle = bundle,
                bundleJson = bundleJson,
                payloadSha256 = HealthConnectWireFormat.sha256(bundleJson),
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `ResearchStudy protocol rejects a resolved non-PlanDefinition target`() {
        val bundle = converter.convert(
            stepRecord("research-protocol"),
            conversionTime,
            EventSequence("1"),
        ).bundle.copy()
        val researchStudy = bundle.entry.mapNotNull { it.resource as? ResearchStudy }.single()
        val patientEntry = bundle.entry.single { it.resource is Patient }
        researchStudy.addProtocol(
            org.hl7.fhir.r4.model.Reference().apply {
                type = "Patient"
                reference = patientEntry.fullUrl
            },
        )

        val failure = runCatching { bundle.requireGroveReferencePolicy() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `ResearchStudy protocol admits only a complete identifier-only PlanDefinition`() {
        val validBundle = converter.convert(
            stepRecord("logical-research-protocol"),
            conversionTime,
            EventSequence("1"),
        ).bundle.copy()
        validBundle.entry.mapNotNull { it.resource as? ResearchStudy }.single().addProtocol(
            org.hl7.fhir.r4.model.Reference().apply {
                type = "PlanDefinition"
                identifier = identifier("https://study.example.org/fhir/protocol", "protocol-1")
            },
        )
        val invalidBundle = validBundle.copy().apply {
            entry.mapNotNull { it.resource as? ResearchStudy }.single().protocol.single().type = "ResearchStudy"
        }

        val validFailure = runCatching { validBundle.requireGroveReferencePolicy() }.exceptionOrNull()
        val invalidFailure = runCatching { invalidBundle.requireGroveReferencePolicy() }.exceptionOrNull()

        assertThat(validFailure).isNull()
        assertThat(invalidFailure).isInstanceOf(IllegalArgumentException::class.java)
    }
}
