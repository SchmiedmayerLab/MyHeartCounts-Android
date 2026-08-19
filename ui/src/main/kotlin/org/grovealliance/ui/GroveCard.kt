//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import org.grovealliance.ui.theme.Colors

/**
 * Default Material Design card.
 *
 * @param modifier Modifier to be applied
 * @param shape card shape
 * @param content content of the card
 */
@Composable
fun GroveCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Colors.surfaceContainerLowest,
    shape: Shape = CardDefaults.shape,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        content = content,
    )
}

/**
 * Default Material Design elevated card
 *
 * @param modifier Modifier to be applied
 * @param shape card shape
 * @param content content of the card
 */
@Composable
fun GroveElevatedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Colors.surfaceContainerLowest,
    shape: Shape = CardDefaults.shape,
    content: @Composable (ColumnScope.() -> Unit),
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        content = content,
    )
}
