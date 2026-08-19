//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.testing.core

import android.app.Application
import org.grovealliance.core.Configuration
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveApplication
import org.grovealliance.core.Standard

/**
 * A minimal [Standard] implementation for use in unit tests.
 */
object TestStandard : Standard

/**
 * A concrete [GroveApplication] backed by an [Application] for use in unit tests.
 * Construct via [testGroveApplication].
 */
abstract class TestGroveApplication internal constructor(
    standard: Standard,
    scope: ConfigurationBuilder.() -> Unit,
) : Application(), GroveApplication {
    override val configuration: Configuration = Configuration(standard = standard, scope = scope)
}

/**
 * Creates and configures a [TestGroveApplication] with the given [standard] and [scope], then
 * registers it with [GroveApplication].
 *
 * Example:
 * ```kotlin
 *
 * @Test fun myTest() {
 *     testGroveApplication {
 *         singleton { MyService() }
 *     }
 * }
 * ```
 */
fun testGroveApplication(
    standard: Standard = TestStandard,
    scope: ConfigurationBuilder.() -> Unit = {},
): TestGroveApplication {
    GroveApplication.clear()
    val application = object : TestGroveApplication(standard = standard, scope = scope) {}
    GroveApplication.configure(application)
    return application
}

/**
 * Configures the Grove dependency graph for a test using a [ConfigurationBuilder] [scope],
 * without exposing the underlying [TestGroveApplication].
 *
 * Use this when you only need the graph to be set up and don't require a reference to the
 * application itself.
 *
 * Example:
 * ```kotlin
 *
 * @Test fun myTest() {
 *     testDependencies {
 *         singleton { MyService() }
 *         module { MyRepository(dependency()) }
 *     }
 * }
 * ```
 */
fun testDependencies(
    scope: ConfigurationBuilder.() -> Unit = {},
) {
    testGroveApplication(scope = scope)
}
