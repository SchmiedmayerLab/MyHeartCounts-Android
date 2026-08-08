//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.foundation.fixtures

import edu.stanford.spezi.foundation.UUID
import java.util.UUID

/**
 * Deterministic [UUID] values for tests.
 */
object UUIDFixtures {
    /**
     * The all-zero UUID.
     */
    val zero: UUID = UUID(0L, 0L)

    /**
     * The UUID whose every hex digit is [digit] (e.g. `'2'` -> `22222222-2222-2222-2222-222222222222`).
     */
    fun repeating(digit: Char): UUID {
        val hex = digit.toString()
        return UUID("${hex.repeat(8)}-${hex.repeat(4)}-${hex.repeat(4)}-${hex.repeat(4)}-${hex.repeat(12)}")
    }
}
