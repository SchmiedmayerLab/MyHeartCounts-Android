//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.core.logging

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpeziLoggerTest {

    @Test
    fun `GLOBAL_CONFIG must be set to null`() {
        assertThat(SpeziLogger.GLOBAL_CONFIG).isNull()
    }
}
