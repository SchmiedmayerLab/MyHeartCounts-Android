//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core.internal

import org.grovealliance.foundation.TypeReference
import org.grovealliance.foundation.simpleTypeName
import org.grovealliance.foundation.typeReference

/**
 * A unified key identifying any registered dependency – a [org.grovealliance.core.Module],
 * a singleton, or a transient factory – in the [DependencyRegistry].
 *
 * Uses [TypeReference] rather than [kotlin.reflect.KClass] so that generic type parameters are
 * preserved at runtime (e.g. `DependencyKey<List<String>>` and `DependencyKey<List<Int>>` are
 * distinct keys).
 *
 * @param type The full [TypeReference] of the dependency, capturing generic arguments.
 * @param identifier An optional string to disambiguate multiple registrations of the same type.
 */
@PublishedApi
internal data class DependencyKey<T : Any>(
    val type: TypeReference<T>,
    val identifier: String?,
) {
    override fun toString(): String {
        val id = if (identifier == null) "" else " (identifier: $identifier)"
        return "Dependency[${type.simpleTypeName}$id]"
    }

    companion object {
        inline operator fun <reified T : Any> invoke(identifier: String? = null) =
            DependencyKey(typeReference<T>(), identifier)
    }
}
