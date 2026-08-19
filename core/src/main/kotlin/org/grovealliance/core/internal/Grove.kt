//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core.internal

import org.grovealliance.core.ApplicationModule
import org.grovealliance.core.Configuration
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.ConfigurationImpl
import org.grovealliance.core.DefaultStandard
import org.grovealliance.core.DependenciesGraph
import org.grovealliance.core.GroveApplication
import org.grovealliance.core.Module
import org.grovealliance.core.Standard
import org.grovealliance.core.groveError
import org.grovealliance.core.optionalDependency
import java.util.concurrent.atomic.AtomicReference

/**
 * Singleton instance that holds the constructed [DependenciesGraph] via [Configuration] of [GroveApplication]s. There is no direct need to
 * interact with this object, as the configuration is done via [GroveApplicationContentProvider] on app start up time for applications that
 * conform to [GroveApplication].
 */
@PublishedApi
internal object Grove {
    val logger by groveCoreLogger()

    @PublishedApi
    internal val graph = AtomicReference<DependenciesGraph>(null)

    @PublishedApi
    internal fun requireGraph(): DependenciesGraph = graph.get()
        ?: run {
            val message = """
                Grove is not configured configured yet. Please make sure your main application conforms to [GroveApplication],
                and you did not request dependencies in the configuration block outside of module factories.
            """.trimMargin()
            groveError(message)
        }

    /**
     * Constructs the [DependenciesGraph] out of the [Configuration] of [GroveApplication], registers [ApplicationModule] module and invokes
     * [Module.configure] on all registered modules in the graph.
     */
    fun configure(application: GroveApplication) {
        logger.i { "Configuring grove application $application" }
        val configuration = application.configuration as ConfigurationImpl
        val registry = configuration.registry
        registry.register(
            key = DependencyKey<ApplicationModule>(),
            factory = { ApplicationModule(application) },
        )
        val dependenciesGraph = DependenciesGraph(registry = registry)
        graph.set(dependenciesGraph)
        dependenciesGraph.configure()
    }

    /**
     * Constructs the [DependenciesGraph] out of the [Configuration] of [GroveApplication], registers [ApplicationModule] module and invokes
     * [Module.configure] on all registered modules in the graph.
     *
     * @param standard the [Standard] to configure the Grove framework with
     * @param scope the configuration block to configure the [DependenciesGraph]
     */
    fun configure(
        standard: Standard = DefaultStandard,
        scope: ConfigurationBuilder.() -> Unit,
    ) {
        val builder = ConfigurationBuilder(standard = standard).apply(scope)
        val applicationModule = optionalDependency<ApplicationModule>().value
        if (applicationModule != null) {
            builder.module { applicationModule }
        }
        val dependenciesGraph = DependenciesGraph(registry = builder.registry)
        graph.set(dependenciesGraph)
        dependenciesGraph.configure()
    }

    /**
     * Clears the singleton instance of [DependenciesGraph] and resets the configuration.
     *
     * This method is used for testing purposes only and should not be used in production code.
     */
    fun clear() {
        graph.set(null)
    }
}
