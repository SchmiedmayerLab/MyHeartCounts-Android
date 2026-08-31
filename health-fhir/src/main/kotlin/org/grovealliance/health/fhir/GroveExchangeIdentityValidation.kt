//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.MedicationAdministration
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResearchStudy
import org.hl7.fhir.r4.model.ResearchSubject
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Specimen
import org.hl7.fhir.r4.model.VisionPrescription
import java.util.Collections
import java.util.IdentityHashMap

/** Enforces the catalog's role-priority selection for every deterministic exchange entry key. */
internal fun Bundle.requireGroveEntryIdentitySelection() {
    require(entry.map { it.fullUrl }.distinct().size == entry.size) {
        "Exchange entry identity validation requires unique fullUrl values."
    }
    requireGroveIdentitySystemRoleSeparation()
    entry.forEachIndexed { index, bundleEntry ->
        val label = "Bundle.entry[$index]"
        val extensions = bundleEntry.extension.filter {
            it.url == GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION
        }
        val selected = extensions.singleOrNull()?.value as? Identifier
            ?: throw IllegalArgumentException("$label requires one Identifier-valued Grove entry key extension.")
        val selectedRole = selected.groveRole("$label entry key")
            ?: throw IllegalArgumentException("$label entry key requires one known Grove Identifier role.")
        selected.key()
        require(bundleEntry.hasFullUrl() && bundleEntry.hasResource()) {
            "$label requires a fullUrl and resource."
        }
        require(bundleEntry.fullUrl == GroveExchangeIdentity.fullUrl(selected)) {
            "$label.fullUrl must derive from its exact selected entry Identifier."
        }

        val resourceIdentifiers = bundleEntry.resource.typedGroveIdentifiers(
            "$label ${bundleEntry.resource.fhirType()}",
        )
        val expectedRole = HealthConnectContract.entryIdentifierPriority.firstOrNull(resourceIdentifiers::containsKey)
        if (expectedRole == null) {
            require(selectedRole == GroveIdentifierRole.ENTRY_NODE) {
                "$label must use an entry-node key when its resource has no prioritized business Identifier."
            }
        } else {
            require(
                selectedRole == expectedRole &&
                    resourceIdentifiers.getValue(expectedRole).matchesIdentifierPair(selected),
            ) {
                "$label must select the resource's exact ${expectedRole.code} Identifier by catalog priority."
            }
        }
    }
}

/** One event cannot reuse a namespace for another role or split one role across namespaces. */
private fun Bundle.requireGroveIdentitySystemRoleSeparation() {
    val systemsByRole = mutableMapOf<GroveIdentifierRole, String>()
    val rolesBySystem = mutableMapOf<String, GroveIdentifierRole>()
    visitPopulatedElements { element ->
        val identifier = element as? Identifier ?: return@visitPopulatedElements
        val role = identifier.groveRole("Exchange graph Identifier") ?: return@visitPopulatedElements
        val key = identifier.key()
        require(systemsByRole[role]?.let { it == key.system } != false) {
            "One exchange event cannot split the ${role.code} role across Identifier.system values."
        }
        require(rolesBySystem[key.system]?.let { it == role } != false) {
            "One exchange event cannot reuse an Identifier.system across Grove roles."
        }
        systemsByRole[role] = key.system
        rolesBySystem[key.system] = role
    }
}

/** Traverses the populated FHIR object tree without assuming any resource-specific element path. */
internal fun Base.visitPopulatedElements(visitor: (Base) -> Unit) {
    val visited = Collections.newSetFromMap(IdentityHashMap<Base, Boolean>())
    fun visit(element: Base) {
        if (!visited.add(element)) return
        visitor(element)
        element.children().forEach { property -> property.values.forEach(::visit) }
    }
    visit(this)
}

internal fun Resource.typedGroveIdentifiers(label: String): Map<GroveIdentifierRole, Identifier> {
    val result = linkedMapOf<GroveIdentifierRole, Identifier>()
    directIdentifiers().forEachIndexed { index, identifier ->
        val role = identifier.groveRole("$label.identifier[$index]") ?: return@forEachIndexed
        identifier.key()
        require(result.put(role, identifier) == null) { "$label repeats the ${role.code} Identifier role." }
    }
    return result
}

private fun Identifier.groveRole(label: String): GroveIdentifierRole? {
    val codings = type.coding.filter { it.system == HealthConnectContract.GROVE_IDENTIFIER_ROLE }
    if (codings.isEmpty()) return null
    require(codings.size == 1) { "$label repeats the Grove role Coding." }
    return GroveIdentifierRole.entries.singleOrNull { it.code == codings.single().code }
        ?: throw IllegalArgumentException("$label uses an unknown Grove role.")
}

private fun Resource.directIdentifiers(): List<Identifier> = when (this) {
    is Observation -> identifier
    is DocumentReference -> identifier
    is Specimen -> identifier
    is VisionPrescription -> identifier
    is MedicationAdministration -> identifier
    is MedicationStatement -> identifier
    is Device -> identifier
    is Patient -> identifier
    is ResearchStudy -> identifier
    is ResearchSubject -> identifier
    is PlanDefinition -> identifier
    is QuestionnaireResponse -> listOf(identifier)
    else -> emptyList()
}

internal fun Identifier.matchesIdentifierPair(other: Identifier): Boolean =
    system == other.system && value == other.value
