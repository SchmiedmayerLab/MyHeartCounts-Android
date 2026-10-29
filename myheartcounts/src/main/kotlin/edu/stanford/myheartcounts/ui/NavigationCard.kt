//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ItemDisplayRowComposable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveShapes

/**
 * A card with a labeled navigation row and a trailing disclosure arrow.
 */
data class NavigationCard(
    val title: StringResource,
    val onClick: () -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier, shape = GroveShapes.large) {
            ItemDisplayRowComposable(
                modifier = Modifier.fillMaxWidth(),
                leadingImage = null,
                label = title.text(),
                valueContent = null,
                showArrow = true,
                onClick = onClick,
            )
        }
    }
}
