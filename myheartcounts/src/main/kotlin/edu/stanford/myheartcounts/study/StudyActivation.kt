//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.storage.local.KeyValueStorage
import java.time.Instant

/**
 * When the study was activated on this device.
 *
 * Unlike the account's enrollment date, which survives reinstalls and is reused on every
 * enrollment, the activation date is the moment the participant last enrolled on this device. It is
 * set on every enrollment and forgotten on sign-out.
 */
interface StudyActivation {

    /**
     * The moment of the most recent enrollment on this device, or null when there was none since
     * the last sign-out, emitted again whenever it changes.
     */
    val activationDate: StateFlow<Instant?>

    /**
     * The current [activationDate].
     */
    val currentActivationDate: Instant?

    /**
     * Records the current moment as the activation date.
     */
    fun markActivated()

    /**
     * Forgets the activation date.
     */
    fun clear()
}

/**
 * [StudyActivation] persisted in the app's [KeyValueStorage].
 */
class StudyActivationImpl(
    private val storage: KeyValueStorage,
    private val timeProvider: TimeProvider,
) : StudyActivation {

    private val mutableActivationDate = MutableStateFlow(
        storage.getLong(key = KEY_ACTIVATION_DATE, default = NO_DATE)
            .takeIf { it != NO_DATE }
            ?.let { Instant.ofEpochMilli(it) },
    )

    override val activationDate = mutableActivationDate.asStateFlow()

    override val currentActivationDate: Instant? get() = activationDate.value

    override fun markActivated() {
        val now = Instant.ofEpochMilli(timeProvider.nowInstant().toEpochMilli())
        storage.putLong(key = KEY_ACTIVATION_DATE, value = now.toEpochMilli())
        mutableActivationDate.value = now
    }

    override fun clear() {
        storage.delete(key = KEY_ACTIVATION_DATE)
        mutableActivationDate.value = null
    }

    private companion object {
        const val KEY_ACTIVATION_DATE = "study_activation_date"
        const val NO_DATE = Long.MIN_VALUE
    }
}
