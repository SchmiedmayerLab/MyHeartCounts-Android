//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.grovealliance.storage.local.KeyValueStorage

/**
 * The prompted actions the participant asked not to be suggested again on this device.
 */
interface PromptedActionRejections {

    /**
     * The rejected actions, emitted again whenever they change.
     */
    val rejected: StateFlow<Set<PromptedActionType>>

    /**
     * Stops suggesting [type].
     */
    fun reject(type: PromptedActionType)

    /**
     * Forgets every rejection.
     */
    fun clear()
}

/**
 * [PromptedActionRejections] persisted in the app's [KeyValueStorage].
 */
class PromptedActionRejectionsImpl(
    private val storage: KeyValueStorage,
) : PromptedActionRejections {

    private val mutableRejected = MutableStateFlow(
        storage.getString(key = KEY_REJECTED, default = "")
            .split(SEPARATOR)
            .mapNotNull { id -> PromptedActionType.entries.firstOrNull { it.id == id } }
            .toSet(),
    )

    override val rejected = mutableRejected.asStateFlow()

    override fun reject(type: PromptedActionType) {
        mutableRejected.update { it + type }
        storage.putString(
            key = KEY_REJECTED,
            value = mutableRejected.value.joinToString(separator = SEPARATOR) { it.id },
        )
    }

    override fun clear() {
        storage.delete(key = KEY_REJECTED)
        mutableRejected.value = emptySet()
    }

    private companion object {
        const val KEY_REJECTED = "rejected_prompted_actions"
        const val SEPARATOR = ","
    }
}
