//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core

import org.grovealliance.core.internal.Grove

/**
 * Base interface that all Grove applications must implement to provide the Grove modules dependency graph
 */
interface GroveApplication {

    /**
     * The [Configuration] of the [GroveApplication] that contains all registered modules.
     */
    val configuration: Configuration

    companion object {
        /**
         * Constructs the [DependenciesGraph] out of the [Configuration] of [GroveApplication],
         * registers [ApplicationModule] module and invokes [Module.configure] on all registered modules in the graph.
         *
         * Note that there is no need to call this method directly, as it is invoked automatically on app start up time by Grove Framework,
         * This method can be used to rebuild the dependency graph in case of a configuration change.
         *
         * @param application the [GroveApplication] instance to configure
         */
        fun configure(application: GroveApplication) {
            Grove.configure(application = application)
        }

        /**
         * Constructs the [DependenciesGraph] out of the [Configuration] of [GroveApplication],
         * registers [ApplicationModule] module and invokes [Module.configure] on all registered modules in the graph.
         *
         * Note that there is no need to call this method directly, as it is invoked automatically on app start up time by Grove Framework,
         * This method can be used to rebuild the dependency graph in case of a configuration change.
         *
         * @param standard the [Standard] to configure the Grove framework with
         * @param scope the configuration block to configure the [DependenciesGraph]
         */
        fun configure(
            standard: Standard = DefaultStandard,
            scope: ConfigurationBuilder.() -> Unit,
        ) {
            Grove.configure(standard = standard, scope = scope)
        }

        /**
         * Clears the [DependenciesGraph] and all registered modules.
         *
         * Note that there is no need to call this method directly, as it is invoked automatically on app start up time by Grove Framework,
         * This method can be used to clear the dependency graph in case of a configuration change.
         */
        fun clear() {
            Grove.clear()
        }
    }
}
