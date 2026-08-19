//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.TimedWalkingTestConfiguration
import java.time.Duration

/**
 * Fixture for [TimedWalkingTestConfiguration].
 */
object TimedWalkingTestConfigurationFixtures {
    fun create(
        duration: Duration = Duration.ZERO,
        kind: TimedWalkingTestConfiguration.Kind = TimedWalkingTestConfiguration.Kind.WALKING,
    ): TimedWalkingTestConfiguration = TimedWalkingTestConfiguration(
        duration = duration,
        kind = kind,
    )
}
