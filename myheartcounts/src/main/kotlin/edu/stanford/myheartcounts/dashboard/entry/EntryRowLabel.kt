//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import org.grovealliance.ui.theme.TextStyles
import kotlin.math.roundToInt

/**
 * The leading label of an entry row, at most a share of the row's width and wrapping onto at most two lines,
 * followed by the space that pushes the row's remaining content to its end.
 */
@Composable
internal fun RowScope.EntryRowLabel(text: String) {
    Text(
        text = text,
        style = TextStyles.bodyLarge,
        maxLines = LABEL_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.layout { measurable, constraints ->
            val maxWidth = if (constraints.hasBoundedWidth) {
                (constraints.maxWidth * LABEL_WIDTH_FRACTION).roundToInt()
            } else {
                constraints.maxWidth
            }
            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = maxWidth))
            layout(placeable.width, placeable.height) { placeable.place(x = 0, y = 0) }
        },
    )
    Spacer(modifier = Modifier.weight(1f))
}

/**
 * The share of the row's width the label may take before it wraps.
 */
private const val LABEL_WIDTH_FRACTION = 0.4f

/**
 * The number of lines the label may wrap onto.
 */
private const val LABEL_MAX_LINES = 2
