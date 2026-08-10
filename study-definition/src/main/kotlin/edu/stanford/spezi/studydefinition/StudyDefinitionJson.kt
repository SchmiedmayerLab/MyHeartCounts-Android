//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import edu.stanford.spezi.foundation.JsonSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Decodes [StudyDefinition]s from their on-disk JSON form.
 *
 * The JSON carries a top-level `schemaVersion`; decoding fails when it does not match
 * [SCHEMA_VERSION], since the shape of older or newer schemas is not guaranteed to be compatible.
 */
object StudyDefinitionJson {
    /**
     * The schema version this model decodes.
     */
    const val SCHEMA_VERSION: String = "0.12.1"

    private const val SCHEMA_VERSION_KEY = "schemaVersion"

    /**
     * Decodes a [StudyDefinition] from [text].
     *
     * @throws IncompatibleSchemaException when the encoded schema version is not [SCHEMA_VERSION].
     */
    fun decode(text: String): StudyDefinition {
        val root = JsonSerializer.parseToElement(text).jsonObject
        val schemaVersion = root[SCHEMA_VERSION_KEY]?.jsonPrimitive?.content
        if (schemaVersion != null && schemaVersion != SCHEMA_VERSION) {
            throw IncompatibleSchemaException(
                found = schemaVersion,
                expected = SCHEMA_VERSION,
            )
        }
        return JsonSerializer.decodeFromElement(root, StudyDefinition.serializer())
    }
}

/**
 * A study definition was encoded with a schema version this model cannot decode.
 */
class IncompatibleSchemaException(
    found: String,
    expected: String,
) : Exception("Incompatible study definition schema: found '$found', expected '$expected'")
