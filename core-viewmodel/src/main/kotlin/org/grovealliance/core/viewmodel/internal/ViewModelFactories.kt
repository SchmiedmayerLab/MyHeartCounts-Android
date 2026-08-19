//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core.viewmodel.internal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.grovealliance.core.DependenciesGraph
import org.grovealliance.core.groveError
import org.grovealliance.core.viewmodel.ViewModelFactoryScope
import kotlin.reflect.KClass

/**
 * Internal class that holds all [ViewModel] factories registered via
 * [org.grovealliance.core.viewmodel.viewModel] in the
 * [org.grovealliance.core.ConfigurationBuilder] DSL.
 *
 * This class is automatically registered and managed by the Grove framework. It is not part of
 * the public API – consumers should use [org.grovealliance.core.viewmodel.groveViewModel] to
 * retrieve ViewModel instances and [org.grovealliance.core.viewmodel.viewModel] to register them.
 */
@PublishedApi
internal class ViewModelFactories(
    private val graph: DependenciesGraph,
) {
    /**
     * Creates a [ViewModel] instance for [clazz] using its registered factory.
     *
     * @param clazz The [KClass] of the ViewModel to create.
     * @param savedStateHandle The [SavedStateHandle] provided by the nearest
     *   [androidx.lifecycle.ViewModelStoreOwner] via [androidx.lifecycle.viewmodel.CreationExtras].
     * @throws org.grovealliance.core.GroveError if no factory is registered for [clazz].
     */
    @Suppress("UNCHECKED_CAST")
    fun <VM : ViewModel> create(clazz: KClass<VM>, savedStateHandle: SavedStateHandle): VM {
        val identifier = ViewModelFactoryEntry.identifierFor(clazz)
        val factoryEntry = graph.optionalDependency<ViewModelFactoryEntry>(identifier)
            ?: groveError(
                "No ViewModel factory registered for ${clazz.simpleName}. " +
                    "Register it via viewModel { ${clazz.simpleName}(...) } " +
                    "in your Configuration block."
            )
        val scope = ViewModelFactoryScope(graph = graph, handle = savedStateHandle)
        return factoryEntry.factory.invoke(scope) as VM
    }
}
