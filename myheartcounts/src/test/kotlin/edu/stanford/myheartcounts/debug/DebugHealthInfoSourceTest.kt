//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.grovealliance.health.FakeHealth
import org.grovealliance.health.HealthConnectAvailability
import org.grovealliance.health.RecordType
import org.junit.Test

class DebugHealthInfoSourceTest {

    private val health = FakeHealth()
    private val source = DebugHealthInfoSourceImpl(health = health)

    @Test
    fun `lists each required data type with whether access is granted`() = runTest {
        // given
        health.updateConfiguration {
            requestReadAccess(RecordType.steps, RecordType.heartRate)
            requestWriteAccess(RecordType.weight)
        }
        health.setAuthorizedToRead(types = setOf(RecordType.steps))
        health.setHistoryReadable(value = true)

        // when
        val info = source.info.first()

        // then
        assertThat(info.availability).isEqualTo(HealthConnectAvailability.Available)
        assertThat(info.read).containsExactly(
            DebugHealthInfo.TypeAccess(
                name = "HeartRate",
                isGranted = false,
            ),
            DebugHealthInfo.TypeAccess(
                name = "Steps",
                isGranted = true,
            ),
        ).inOrder()
        assertThat(info.write).containsExactly(
            DebugHealthInfo.TypeAccess(
                name = "Weight",
                isGranted = false,
            ),
        )
        assertThat(info.historyAccess).isTrue()
        assertThat(info.backgroundAccess).isFalse()
    }

    @Test
    fun `reports no history or background access without read types`() = runTest {
        // when
        val info = source.info.first()

        // then
        assertThat(info.read).isEmpty()
        assertThat(info.historyAccess).isNull()
        assertThat(info.backgroundAccess).isNull()
    }
}
