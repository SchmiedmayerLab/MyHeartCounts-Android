//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.foundation.fixtures

import java.time.Instant

/**
 * Fixed [Instant] values for tests.
 */
object InstantFixtures {
    /**
     * A fixed reference instant for deterministic tests.
     */
    val reference: Instant = Instant.parse("2025-01-01T00:00:00Z")
}
