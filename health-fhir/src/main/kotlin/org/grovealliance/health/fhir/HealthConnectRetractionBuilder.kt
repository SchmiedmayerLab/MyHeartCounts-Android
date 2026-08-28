//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import java.time.Instant

/** Selects and renders the complete prior graph named by one retraction event. */
internal class HealthConnectRetractionBuilder(
    private val converter: HealthConnectConverter,
) {
    fun targets(entry: HealthConnectExportJournalEntry): Set<HealthConnectRetractionTarget> {
        val childOutputIdentifiers = entry.childOutputIdentifiers()
        return (
            entry.observationRetractionTargets(childOutputIdentifiers) +
                entry.nonObservationOutputRetractionTargets() +
                entry.deviceSnapshotRetractionTargets()
            ).toSet()
    }

    fun bundle(
        entry: HealthConnectExportJournalEntry,
        targets: Set<HealthConnectRetractionTarget>,
        retractedAt: Instant,
        eventSequence: EventSequence,
    ): Bundle = Bundle().apply {
        identifier = converter.bundleIdentifier(eventSequence)
        meta.addProfile(HealthConnectContract.MOBILE_RETRACTION_BUNDLE_PROFILE)
        type = Bundle.BundleType.COLLECTION
        timestampElement = InstantType(retractedAt.toString())
        addGroveEntry(
            HealthConnectIdentity.retractionNode(
                converter.entryNodeIdentifierSystem,
                identifier,
            ),
            entry.provenance(targets, retractedAt, eventSequence),
        )
    }

    private fun HealthConnectExportJournalEntry.childOutputIdentifiers(): Set<FhirIdentifierKey> =
        bundle.entry
            .mapNotNull { it.resource as? Observation }
            .flatMap(Observation::getHasMember)
            .map { member -> resolveMemberOutput(member) }
            .toSet()

    private fun HealthConnectExportJournalEntry.resolveMemberOutput(member: Reference): FhirIdentifierKey {
        require(member.hasReference()) {
            "An active member output must use a literal reference inside its event Bundle."
        }
        val memberResource = bundle.entry.singleOrNull { it.fullUrl == member.reference }?.resource
            ?: throw IllegalArgumentException(
                "An active member output reference must resolve exactly once inside its event Bundle.",
            )
        require(memberResource is Observation) {
            "An active Observation.hasMember target must resolve to an Observation."
        }
        return observationIdentity(memberResource).key()
    }

    private fun HealthConnectExportJournalEntry.observationRetractionTargets(
        childOutputIdentifiers: Set<FhirIdentifierKey>,
    ): List<HealthConnectRetractionTarget> = observations.map { observation ->
        val identifier = observationIdentity(observation)
        val identifierKey = identifier.key()
        HealthConnectRetractionTarget(
            identifier = identifierKey,
            identifierRole = GroveIdentifierRole.SOURCE_OUTPUT,
            resourceType = observation.fhirType(),
            role = if (identifierKey in childOutputIdentifiers) {
                HealthConnectRetractionTargetRole.CHILD_OUTPUT
            } else {
                HealthConnectRetractionTargetRole.PRIMARY_OUTPUT
            },
        )
    }

    private fun HealthConnectExportJournalEntry.nonObservationOutputRetractionTargets() =
        bundle.entry.mapNotNull { entry ->
            if (entry.resource is Observation) return@mapNotNull null
            entry.resource.typedGroveIdentifiers(
                "Retraction ${entry.resource.fhirType()} output",
            )[GroveIdentifierRole.SOURCE_OUTPUT]?.let { identifier ->
                val targetRole = when (entry.resource.fhirType()) {
                    "DocumentReference" -> HealthConnectRetractionTargetRole.SOURCE_ARTIFACT
                    "Specimen" -> HealthConnectRetractionTargetRole.SPECIMEN
                    "VisionPrescription", "MedicationAdministration", "MedicationStatement" ->
                        HealthConnectRetractionTargetRole.PRIMARY_OUTPUT
                    else -> return@let null
                }
                HealthConnectRetractionTarget(
                    identifier = identifier.key(),
                    identifierRole = GroveIdentifierRole.SOURCE_OUTPUT,
                    resourceType = entry.resource.fhirType(),
                    role = targetRole,
                )
            }
        }

    private fun HealthConnectExportJournalEntry.deviceSnapshotRetractionTargets() =
        bundle.entry.mapNotNull { entry ->
            if (entry.resource !is org.hl7.fhir.r4.model.Device) return@mapNotNull null
            entry.resource.typedGroveIdentifiers(
                "Retraction Device snapshot",
            )[GroveIdentifierRole.DEVICE_SNAPSHOT]?.let { identifier ->
                HealthConnectRetractionTarget(
                    identifier = identifier.key(),
                    identifierRole = GroveIdentifierRole.DEVICE_SNAPSHOT,
                    resourceType = entry.resource.fhirType(),
                    role = HealthConnectRetractionTargetRole.DEVICE_SNAPSHOT,
                )
            }
        }

    private fun HealthConnectExportJournalEntry.provenance(
        targets: Set<HealthConnectRetractionTarget>,
        retractedAt: Instant,
        eventSequence: EventSequence,
    ): Provenance {
        val sourceIdentifier = sourceRecordIdentifier
        return Provenance().apply {
            meta.addProfile(HealthConnectContract.MOBILE_RETRACTION_PROVENANCE_PROFILE)
            occurred = DateTimeType(retractedAt.toString())
            recordedElement = InstantType(retractedAt.toString())
            activity = concept(
                HealthConnectContract.GROVE_LIFECYCLE_EVENT,
                "source-record-retracted",
                "Source record retracted",
            )
            addAgent().apply {
                type = concept(HealthConnectContract.PROVENANCE_PARTICIPANT, "assembler", "Assembler")
                who = Reference().apply {
                    type = "Device"
                    identifier = converter.assemblerSnapshotIdentifier(eventSequence)
                }
            }
            addEntity().apply {
                role = Provenance.ProvenanceEntityRole.SOURCE
                what = Reference().setIdentifier(sourceIdentifier)
            }
            targets.sortedWith(compareBy({ it.identifier }, { it.resourceType }, { it.role.code })).forEach {
                addTarget(it.reference())
            }
        }
    }

    private fun HealthConnectRetractionTarget.reference(): Reference = Reference().apply {
        type = resourceType
        identifier = this@reference.identifier.identifier().apply {
            type = org.hl7.fhir.r4.model.CodeableConcept(
                Coding(
                    HealthConnectContract.GROVE_IDENTIFIER_ROLE,
                    identifierRole.code,
                    identifierRole.display,
                ),
            )
        }
        addExtension(
            Extension(
                HealthConnectContract.GROVE_RETRACTION_TARGET_ROLE,
                CodeType(role.code),
            ),
        )
    }
}
