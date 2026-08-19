//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health

import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl

/**
 * Convenience for registering the [Health] module to the Grove configuration.
 */
@GroveDsl
fun ConfigurationBuilder.health(block: HealthModuleBuilder.() -> Unit) {
    module {
        val builder = HealthModuleBuilder().apply(block)
        Health(builder = builder)
    }
}
