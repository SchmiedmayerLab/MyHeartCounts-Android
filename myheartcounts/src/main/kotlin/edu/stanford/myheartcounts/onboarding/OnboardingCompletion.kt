//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import org.grovealliance.storage.local.KeyValueStorage

/**
 * Whether the participant has completed onboarding on this device.
 *
 * It decides which root the app launches into. It is set once onboarding finishes and cleared on
 * every sign-out, so a participant who signs out goes through onboarding again.
 */
interface OnboardingCompletion {

    /**
     * Whether onboarding has been completed since the last sign-out.
     */
    val isComplete: Boolean

    /**
     * Records that the participant finished onboarding.
     */
    fun markComplete()

    /**
     * Forgets a completed onboarding, so the next launch starts it again.
     */
    fun reset()
}

/**
 * [OnboardingCompletion] persisted in the app's [KeyValueStorage].
 */
class OnboardingCompletionImpl(
    private val storage: KeyValueStorage,
) : OnboardingCompletion {

    override val isComplete: Boolean
        get() = storage.getBoolean(key = KEY_ONBOARDING_COMPLETE, default = false)

    override fun markComplete() {
        storage.putBoolean(key = KEY_ONBOARDING_COMPLETE, value = true)
    }

    override fun reset() {
        storage.delete(key = KEY_ONBOARDING_COMPLETE)
    }

    private companion object {
        const val KEY_ONBOARDING_COMPLETE = "onboarding_flow_complete"
    }
}
