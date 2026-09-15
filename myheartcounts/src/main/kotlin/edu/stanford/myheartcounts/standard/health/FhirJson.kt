//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import org.json.JSONArray
import org.json.JSONObject

/**
 * Bridges HAPI's FHIR JSON into the primitives Firestore stores.
 *
 * HAPI serializes to a JSON string and Firestore writes maps of native values, so a document written
 * from a FHIR resource has to be converted rather than handed over. Going through the resource's own
 * JSON is deliberate: it keeps one FHIR serialization in the app, so a resource written to Firestore
 * and the same resource written into a Cloud Storage archive cannot drift apart.
 */
internal object FhirJson {

    /**
     * Converts a serialized FHIR resource into a Firestore document body.
     *
     * @param json A JSON object, as produced by HAPI's `IParser`.
     */
    fun toFirestoreMap(json: String): Map<String, Any?> = convert(JSONObject(json))

    private fun convert(value: JSONObject): Map<String, Any?> =
        value.keys().asSequence().associateWith { key -> convertValue(value.get(key)) }

    private fun convert(value: JSONArray): List<Any?> =
        (0 until value.length()).map { index -> convertValue(value.get(index)) }

    private fun convertValue(value: Any?): Any? = when (value) {
        JSONObject.NULL -> null
        is JSONObject -> convert(value)
        is JSONArray -> convert(value)
        else -> value
    }
}
