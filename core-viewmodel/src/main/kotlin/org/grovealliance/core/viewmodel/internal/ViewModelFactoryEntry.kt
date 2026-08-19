//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core.viewmodel.internal

import androidx.lifecycle.ViewModel
import org.grovealliance.core.viewmodel.ViewModelFactoryScope
import kotlin.reflect.KClass

/**
 * Wraps a single [ViewModel] factory lambda registered via
 * [org.grovealliance.core.viewmodel.viewModel].
 */
@PublishedApi
internal data class ViewModelFactoryEntry(
    val factory: ViewModelFactoryScope.() -> ViewModel,
) {
    companion object {
        fun <VM : ViewModel> identifierFor(clazz: KClass<VM>): String = "viewmodel:${clazz.qualifiedName}"
    }
}
