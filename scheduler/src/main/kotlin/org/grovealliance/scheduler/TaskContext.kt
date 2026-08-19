//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler

import kotlinx.serialization.KSerializer
import org.grovealliance.scheduler.internal.JsonContextStore

/**
 * A typed key for a value stored alongside a [Task] in its [TaskContext].
 *
 * Each key carries a stable [identifier] and a [serializer], so the value can be persisted and
 * restored without the context needing to know the concrete value types.
 */
interface TaskStorageKey<Value : Any> {
    val identifier: String
    val serializer: KSerializer<Value>
}

/**
 * A typed key for a value stored alongside an [Outcome] in its [OutcomeContext].
 */
interface OutcomeStorageKey<Value : Any> {
    val identifier: String
    val serializer: KSerializer<Value>
}

/**
 * Additional, typed information stored alongside a [Task].
 *
 * Values are read and written through [TaskStorageKey]s; the context preserves any entry it does
 * not have a key for, so unknown values survive read-modify-write cycles.
 */
class TaskContext internal constructor(
    private val store: JsonContextStore,
) {
    constructor() : this(JsonContextStore())

    /**
     * The value stored for [key], or `null` if none is present.
     */
    operator fun <Value : Any> get(key: TaskStorageKey<Value>): Value? =
        store.get(key.identifier, key.serializer)

    /**
     * Stores [value] for [key], or removes the entry when [value] is `null`.
     */
    operator fun <Value : Any> set(key: TaskStorageKey<Value>, value: Value?) {
        store.set(key.identifier, key.serializer, value)
    }

    internal fun encoded(): String = store.encoded()

    override fun equals(other: Any?): Boolean = other is TaskContext && other.store == store
    override fun hashCode(): Int = store.hashCode()

    internal companion object {
        fun decode(encoded: String?): TaskContext = TaskContext(JsonContextStore.decode(encoded))
    }
}

/**
 * Additional, typed information stored alongside an [Outcome].
 */
class OutcomeContext internal constructor(
    private val store: JsonContextStore,
) {
    constructor() : this(JsonContextStore())

    /**
     * The value stored for [key], or `null` if none is present.
     */
    operator fun <Value : Any> get(key: OutcomeStorageKey<Value>): Value? =
        store.get(key.identifier, key.serializer)

    /**
     * Stores [value] for [key], or removes the entry when [value] is `null`.
     */
    operator fun <Value : Any> set(key: OutcomeStorageKey<Value>, value: Value?) {
        store.set(key.identifier, key.serializer, value)
    }

    internal fun encoded(): String = store.encoded()

    override fun equals(other: Any?): Boolean = other is OutcomeContext && other.store == store
    override fun hashCode(): Int = store.hashCode()

    internal companion object {
        fun decode(encoded: String?): OutcomeContext = OutcomeContext(JsonContextStore.decode(encoded))
    }
}
