//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Base
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
import java.util.Collections
import java.util.IdentityHashMap

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

internal fun Resource.groveReferenceNodes(): List<Reference> {
    val references = mutableListOf<Reference>()
    val visited = Collections.newSetFromMap(IdentityHashMap<Base, Boolean>())
    fun visit(element: Base) {
        if (!visited.add(element)) return
        if (element is Reference) references += element
        element.children().flatMap { it.values }.forEach(::visit)
    }
    visit(this)
    return references
}

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
    if (targetTypes == PATIENT_TARGET) identifier.requireLogicalPatientPseudonym(label)
}

internal fun Identifier.requireLogicalPatientPseudonym(label: String) {
    require(system !in LOGICAL_PATIENT_RESERVED_SYSTEMS) {
        "$label logical Patient Identifier.system must not use a Grove protocol code system."
    }
    require(type.coding.none { it.system == HealthConnectContract.GROVE_IDENTIFIER_ROLE }) {
        "$label logical Patient Identifier must not claim a Grove identifier role."
    }
}

private fun Resource.governedReferences(): List<GovernedReference> =
    governedElementReferences() + buildList {
        allExtensions().forEach { extension ->
            val targets = EXTENSION_TARGETS[extension.url] ?: return@forEach
            val reference = extension.value as? Reference
            require(reference != null) { "${extension.url} must carry a valueReference." }
            add(GovernedReference("extension('${extension.url}')", reference, targets))
        }
    }

private fun Resource.governedElementReferences(): List<GovernedReference> = when (this) {
    is Observation -> governedReferences()
    is DocumentReference -> optionalGovernedReference(
        hasSubject(),
        "DocumentReference.subject",
        subject,
        PATIENT_TARGET,
    )
    is QuestionnaireResponse -> optionalGovernedReference(
        hasSubject(),
        "QuestionnaireResponse.subject",
        subject,
        PATIENT_TARGET,
    )
    is Specimen -> governedReferences()
    is MedicationAdministration ->
        optionalGovernedReference(hasSubject(), "MedicationAdministration.subject", subject, PATIENT_TARGET)
    is MedicationStatement ->
        optionalGovernedReference(hasSubject(), "MedicationStatement.subject", subject, PATIENT_TARGET)
    is VisionPrescription ->
        optionalGovernedReference(hasPatient(), "VisionPrescription.patient", patient, PATIENT_TARGET)
    is ResearchSubject -> buildList {
        if (hasIndividual()) add(GovernedReference("ResearchSubject.individual", individual, PATIENT_TARGET))
        if (hasStudy()) add(GovernedReference("ResearchSubject.study", study, RESEARCH_STUDY_TARGET))
    }
    is ResearchStudy -> protocol.map {
        GovernedReference("ResearchStudy.protocol", it, PLAN_DEFINITION_TARGET)
    }
    is Device -> optionalGovernedReference(hasParent(), "Device.parent", parent, DEVICE_TARGET)
    else -> emptyList()
}

private fun Observation.governedReferences(): List<GovernedReference> = buildList {
    require(hasSubject()) { "A Grove Observation requires one Patient subject." }
    add(GovernedReference("Observation.subject", subject, PATIENT_TARGET))
    if (hasDevice()) add(GovernedReference("Observation.device", device, DEVICE_TARGET))
    if (hasSpecimen()) add(GovernedReference("Observation.specimen", specimen, SPECIMEN_TARGET))
    hasMember.forEach { add(GovernedReference("Observation.hasMember", it, OBSERVATION_TARGET)) }
    derivedFrom.forEach { add(GovernedReference("Observation.derivedFrom", it, DERIVED_FROM_TARGETS)) }
}

private fun Specimen.governedReferences(): List<GovernedReference> {
    require(hasSubject()) { "A Grove Health Connect Specimen requires one Patient subject." }
    return listOf(GovernedReference("Specimen.subject", subject, PATIENT_TARGET))
}

private fun optionalGovernedReference(
    present: Boolean,
    path: String,
    reference: Reference,
    targetTypes: Set<String>,
): List<GovernedReference> = if (present) {
    listOf(GovernedReference(path, reference, targetTypes))
} else {
    emptyList()
}

private fun Resource.allExtensions(): List<Extension> {
    val extensions = mutableListOf<Extension>()
    val visited = Collections.newSetFromMap(IdentityHashMap<Base, Boolean>())
    fun visit(element: Base) {
        if (!visited.add(element)) return
        if (element is Extension) extensions += element
        element.children().flatMap { it.values }.forEach(::visit)
    }
    visit(this)
    return extensions
}

private data class GovernedReference(
    val path: String,
    val reference: Reference,
    val targetTypes: Set<String>,
)

private val PATIENT_TARGET = setOf("Patient")
private val DEVICE_TARGET = setOf("Device")
private val SPECIMEN_TARGET = setOf("Specimen")
private val OBSERVATION_TARGET = setOf("Observation")
private val RESEARCH_STUDY_TARGET = setOf("ResearchStudy")
private val PLAN_DEFINITION_TARGET = setOf("PlanDefinition")
private val DERIVED_FROM_TARGETS = setOf("DocumentReference", "QuestionnaireResponse")
private val LOGICAL_PATIENT_RESERVED_SYSTEMS = setOf(
    HealthConnectContract.GROVE_IDENTIFIER_ROLE,
    HealthConnectContract.GROVE_LIFECYCLE_EVENT,
    HealthConnectContract.GROVE_RETRACTION_TARGET_ROLE_CS,
)
private val EXTENSION_TARGETS = mapOf(
    "http://hl7.org/fhir/StructureDefinition/observation-gatewayDevice" to DEVICE_TARGET,
    "http://hl7.org/fhir/StructureDefinition/workflow-researchStudy" to RESEARCH_STUDY_TARGET,
)
