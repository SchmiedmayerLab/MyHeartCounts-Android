//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.DomainResource
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.MedicationAdministration
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResearchStudy
import org.hl7.fhir.r4.model.ResearchSubject
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Specimen
import org.hl7.fhir.r4.model.VisionPrescription
/** Enforces the exchange protocol's literal closure and governed Reference-shape table. */
internal fun Bundle.requireGroveReferencePolicy() {
    val resourcesByFullUrl = entry.associate { bundleEntry ->
        bundleEntry.fullUrl to bundleEntry.resource
    }
    require(resourcesByFullUrl.size == entry.size) {
        "Reference validation requires unique Bundle entry fullUrl values."
    }

    entry.forEachIndexed { entryIndex, bundleEntry ->
        val root = bundleEntry.resource
        require((root as? DomainResource)?.contained.isNullOrEmpty()) {
            "Bundle.entry[$entryIndex] must not contain hidden resources; every graph node is a Bundle entry."
        }
        root.groveReferenceNodes().filter(Reference::hasReference).forEach { reference ->
            reference.requireResolvedLiteral(
                resourcesByFullUrl,
                "Bundle.entry[$entryIndex] literal Reference",
            )
        }
        root.governedReferences().forEach { governed ->
            governed.reference.requireGovernedShape(
                governed.targetTypes,
                resourcesByFullUrl,
                "Bundle.entry[$entryIndex].${governed.path}",
            )
        }
    }
}

internal fun Resource.groveReferenceNodes(): List<Reference> =
    buildList { visitPopulatedElements { if (it is Reference) add(it) } }

private fun Reference.requireResolvedLiteral(
    resourcesByFullUrl: Map<String, Resource>,
    label: String,
): Resource {
    val literal = reference
    val target = resourcesByFullUrl[literal]
    require(target != null) { "$label must resolve inside its exchange graph." }
    require(!hasType() || type == target.fhirType()) {
        "$label.type must equal the referenced ${target.fhirType()} resource type."
    }
    return target
}

private fun Reference.requireGovernedShape(
    targetTypes: Set<String>,
    resourcesByFullUrl: Map<String, Resource>,
    label: String,
) {
    if (hasReference()) {
        require(!hasIdentifier()) {
            "$label must not mix a resolving literal with a logical identifier."
        }
        val target = requireResolvedLiteral(resourcesByFullUrl, label)
        require(target.fhirType() in targetTypes) {
            "$label must reference ${targetTypes.sorted().joinToString(" or ")}."
        }
        return
    }

    require(hasType() && type in targetTypes && hasIdentifier()) {
        "$label identifier-only logical Reference requires an exact admitted type and one complete Identifier."
    }
    identifier.key()
    if (type == "Patient") identifier.requireLogicalPatientPseudonym(label)
}

internal fun Identifier.requireLogicalPatientPseudonym(label: String) {
    require(system !in HealthConnectContract.reservedPatientIdentifierSystems) {
        "$label logical Patient pseudonym must not use a protocol-reserved system."
    }
    require(type.coding.none { it.system == HealthConnectContract.GROVE_IDENTIFIER_ROLE }) {
        "$label logical Patient pseudonym must not claim a Grove identifier role."
    }
}

private fun Resource.governedReferences(): List<GovernedReference> =
    governedElementReferences() + buildList {
        allExtensions().forEach { extension ->
            val targetTypes = HealthConnectContract.governedExtensionReferenceTargets[extension.url]
                ?: return@forEach
            val reference = extension.value as? Reference
            require(reference != null) { "${extension.url} must carry a valueReference." }
            add(GovernedReference("extension('${extension.url}')", reference, targetTypes))
        }
    }

private fun Resource.governedElementReferences(): List<GovernedReference> = when (this) {
    is Observation -> governedReferences()
    is DocumentReference -> optionalGovernedReference(hasSubject(), "DocumentReference.subject", subject)
    is QuestionnaireResponse ->
        optionalGovernedReference(hasSubject(), "QuestionnaireResponse.subject", subject)
    is Specimen -> governedReferences()
    is MedicationAdministration ->
        optionalGovernedReference(hasSubject(), "MedicationAdministration.subject", subject)
    is MedicationStatement ->
        optionalGovernedReference(hasSubject(), "MedicationStatement.subject", subject)
    is VisionPrescription ->
        optionalGovernedReference(hasPatient(), "VisionPrescription.patient", patient)
    is ResearchSubject -> buildList {
        if (hasIndividual()) add(governed("ResearchSubject.individual", individual))
        if (hasStudy()) add(governed("ResearchSubject.study", study))
    }
    is ResearchStudy -> protocol.map { governed("ResearchStudy.protocol", it) }
    is Device -> optionalGovernedReference(hasParent(), "Device.parent", parent)
    else -> emptyList()
}

private fun Observation.governedReferences(): List<GovernedReference> = buildList {
    require(hasSubject()) { "A Grove Observation requires one Patient subject." }
    add(governed("Observation.subject", subject))
    if (hasDevice()) add(governed("Observation.device", device))
    if (hasSpecimen()) add(governed("Observation.specimen", specimen))
    focus.forEach { add(governed("Observation.focus", it)) }
    hasMember.forEach { add(governed("Observation.hasMember", it)) }
    derivedFrom.forEach { add(governed("Observation.derivedFrom", it)) }
}

private fun Specimen.governedReferences(): List<GovernedReference> {
    require(hasSubject()) { "A Grove Health Connect Specimen requires one Patient subject." }
    return listOf(governed("Specimen.subject", subject))
}

private fun governed(path: String, reference: Reference): GovernedReference =
    GovernedReference(path, reference, targets(path))

private fun optionalGovernedReference(
    present: Boolean,
    path: String,
    reference: Reference,
): List<GovernedReference> = if (present) listOf(governed(path, reference)) else emptyList()

private fun Resource.allExtensions(): List<Extension> =
    buildList { visitPopulatedElements { if (it is Extension) add(it) } }

private data class GovernedReference(
    val path: String,
    val reference: Reference,
    val targetTypes: Set<String>,
)

/** Every governed path this validator resolves; the catalog owns which paths exist. */
private val HANDLED_GOVERNED_PATHS = setOf(
    "Observation.subject",
    "Observation.device",
    "Observation.specimen",
    "Observation.focus",
    "Observation.hasMember",
    "Observation.derivedFrom",
    "DocumentReference.subject",
    "QuestionnaireResponse.subject",
    "Specimen.subject",
    "MedicationAdministration.subject",
    "MedicationStatement.subject",
    "VisionPrescription.patient",
    "ResearchSubject.individual",
    "ResearchSubject.study",
    "ResearchStudy.protocol",
    "Device.parent",
)

private val GOVERNED_TARGETS: Map<String, Set<String>> =
    HealthConnectContract.governedReferenceTargets.also {
        check(it.keys == HANDLED_GOVERNED_PATHS) {
            "Every catalog-governed Reference path requires an explicit adapter disposition."
        }
    }

private fun targets(path: String): Set<String> = GOVERNED_TARGETS.getValue(path)
