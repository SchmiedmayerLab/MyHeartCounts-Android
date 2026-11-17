//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import com.google.common.truth.Truth.assertThat
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.junit.Test
import java.time.Instant

class StudyActivationTest {

    private val storage = InMemoryKeyValueStorage()
    private val timeProvider = FakeTimeProvider().apply {
        setNow(instant = Instant.parse("2026-01-01T09:30:00Z"))
    }

    @Test
    fun `has no activation date before the study was activated`() {
        // when
        val activationDate = activation().currentActivationDate

        // then
        assertThat(activationDate).isNull()
    }

    @Test
    fun `remembers the moment of activation across instances`() {
        // given
        activation().markActivated()

        // when
        val activationDate = activation().currentActivationDate

        // then
        assertThat(activationDate).isEqualTo(Instant.parse("2026-01-01T09:30:00Z"))
    }

    @Test
    fun `replaces the activation date on a later activation`() {
        // given
        val activation = activation()
        activation.markActivated()
        timeProvider.setNow(instant = Instant.parse("2026-01-02T10:00:00Z"))

        // when
        activation.markActivated()

        // then
        assertThat(activation.currentActivationDate).isEqualTo(Instant.parse("2026-01-02T10:00:00Z"))
    }

    @Test
    fun `forgets the activation date once cleared`() {
        // given
        val activation = activation()
        activation.markActivated()

        // when
        activation.clear()

        // then
        assertThat(activation().currentActivationDate).isNull()
    }

    private fun activation() = StudyActivationImpl(
        storage = storage,
        timeProvider = timeProvider,
    )
}
