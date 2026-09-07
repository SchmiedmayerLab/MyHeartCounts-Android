//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordTypeTest {
    @Test
    fun `closed inventory exposes every declared Health Connect record type exactly once`() {
        val declared = RecordType.Companion::class.java.methods
            .filter { method -> method.parameterCount == 0 && method.returnType == RecordType::class.java }
            .map { method -> method.invoke(RecordType.Companion) as RecordType<*> }
            .toSet()

        assertThat(RecordType.all).containsExactlyElementsIn(declared)
        assertThat(RecordType.all.map { it.identifier }).containsNoDuplicates()
        assertThat(RecordType.all.map { it.type }).containsNoDuplicates()
        assertThat(RecordType.all.map { it.identifier }).containsAtLeast(
            "ActiveCaloriesBurnedRecord",
            "BloodGlucoseRecord",
            "BloodPressureRecord",
            "HeartRateRecord",
            "SleepSessionRecord",
            "StepsRecord",
            "WeightRecord",
        )
    }
}
