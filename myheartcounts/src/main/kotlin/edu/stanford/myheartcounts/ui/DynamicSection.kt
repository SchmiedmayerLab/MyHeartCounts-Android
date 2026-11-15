//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.GroveSection
import org.grovealliance.ui.LazyListContent

/**
 * The items of the current [section], following its changes while the hosting list is shown.
 */
data class DynamicSection(
    val section: StateFlow<GroveSection>,
) : LazyListContent {

    @Composable
    override fun items(): LazyListScope.() -> Unit {
        val section by section.collectAsStateWithLifecycle()
        return section.items()
    }
}
