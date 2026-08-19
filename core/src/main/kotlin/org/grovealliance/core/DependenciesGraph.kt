//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core

import android.content.Context
import org.grovealliance.core.internal.DependencyKey
import org.grovealliance.core.internal.DependencyRegistry
import org.grovealliance.core.internal.groveCoreLogger
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

/**
 * A graph of dependencies built at app start up via the configuration of the [GroveApplication].
 *
 * Holds three kinds of registrations, all treated uniformly:
 * - **Modules** – types implementing [Module], registered via [ConfigurationBuilder.module].
 *   [Module.configure] is invoked on each after the graph is fully built.
 * - **Singletons** – any type registered via [ConfigurationBuilder.singleton]; the factory runs
 *   once and the result is cached for the lifetime of the graph.
 * - **Transient factories** – any type registered via [ConfigurationBuilder.factory]; the factory
 *   runs on every [dependency] / [optionalDependency] call and is never cached.
 *
 * @param registry The [DependencyRegistry] containing all registered dependencies.
 */
class DependenciesGraph internal constructor(
    @PublishedApi internal val registry: DependencyRegistry,
) {
    @PublishedApi
    internal val logger by groveCoreLogger()

    @PublishedApi
    internal val currentlyResolvingKeys = ThreadLocal.withInitial { mutableSetOf<Any>() }

    /**
     * Retrieves a dependency of type [T] from the graph, or returns `null` when not found.
     *
     * Checks cached instances and registered factories (singleton and transient) in order.
     *
     * @param identifier An optional identifier to disambiguate multiple registrations of the same type.
     */
    inline fun <reified T : Any> optionalDependency(identifier: String? = null): T? =
        registry.resolveDependency(DependencyKey<T>(identifier), this)

    /**
     * Retrieves a dependency of type [T] from the graph by a pre-built [key], or returns `null`
     * when not found. Prefer this overload in internal code that already holds a [DependencyKey]
     * to avoid constructing a second key object.
     */
    @PublishedApi
    internal fun <T : Any> optionalDependency(key: DependencyKey<T>): T? =
        registry.resolveDependency(key, this)

    /**
     * Retrieves a dependency of type [T] from the graph, throwing if not found.
     *
     * When no factory is registered for [T], attempts auto-instantiation via a no-arg constructor,
     * a `Context`-arg constructor, or a [DefaultInitializer] companion object.
     *
     * @param identifier An optional identifier to disambiguate multiple registrations of the same type.
     * @throws [GroveError] if the dependency is not found and cannot be auto-instantiated.
     */
    inline fun <reified T : Any> dependency(identifier: String? = null): T =
        optionalDependency<T>(identifier) ?: createDependencyOrThrow(DependencyKey<T>(identifier), T::class)

    /**
     * Returns the application [Context] of the [GroveApplication] backing this graph.
     *
     * Convenience for factory scopes that need an application [Context] without resolving the
     * [ApplicationModule] manually.
     */
    fun appContext(): Context =
        dependency<ApplicationModule>().application.requireApplicationContext()

    /**
     * Attempts to instantiate a type [T] via reflection fallback strategies when no factory was
     * explicitly registered (no-arg constructor → Context constructor → [DefaultInitializer]
     * companion object).
     */
    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> createDependencyOrThrow(key: DependencyKey<T>, clazz: KClass<T>): T {
        val appContext = registry.resolveDependency(DependencyKey<ApplicationModule>(), this)
            ?.application?.applicationContext
        val result = runCatching {
            clazz.constructors.find { it.parameters.isEmpty() }?.call()
                ?: clazz.constructors.find {
                    it.parameters.size == 1 && it.parameters[0].type.classifier == Context::class
                }?.call(appContext)
                ?: appContext?.let {
                    (clazz.companionObjectInstance as? DefaultInitializer<T>)?.create(it)
                }
                ?: groveError("No suitable constructor found for $key")
        }
        val instance = result.getOrNull()
        return if (instance != null) {
            registry.instances[key] = instance
            logger.w { "Instantiated $key manually via fallback mechanism. Consider registering it explicitly." }
            instance
        } else {
            groveError(
                message = "$key not found. Please make sure to register it in the configuration block of your app.",
                cause = result.exceptionOrNull()
            )
        }
    }

    /** Resolves all registered lazy factories and invokes [Module.configure] on each module. */
    internal fun configure() {
        logger.i { "Configuring dependencies graph" }
        resolveFactories()
        registry.instances.forEach { (key, value) ->
            if (value is Module) {
                logger.i { "Configuring $key" }
                value.configure()
            }
        }
    }

    private fun resolveFactories() = with(registry) {
        val keys = factories.keys.toList()
        keys.forEach { key ->
            logger.i { "Resolving factory for $key" }
            val factory = factories[key]
            if (!instances.containsKey(key) && factory != null) {
                keyResolvingScope(key) {
                    instances[key] = factory()
                }
            }
        }
        factories.clear()
    }

    @PublishedApi
    internal inline fun <R> keyResolvingScope(key: Any, block: () -> R): R {
        logger.i { "Started resolving dependency for $key" }
        if (currentlyResolvingKeys.get()?.add(key) == false) {
            logger.e { "Circular dependency detected for $key." }
            val path = ((currentlyResolvingKeys.get()?.toList() ?: emptyList()) + key)
                .joinToString(" => ") { it.toString() }
            groveError("Circular dependency detected while resolving: $path")
        }
        return try {
            logger.i { "Executing resolving of $key" }
            block()
        } finally {
            logger.i { "Finished resolving dependency for $key" }
            currentlyResolvingKeys.get()?.remove(key)
        }
    }
}
