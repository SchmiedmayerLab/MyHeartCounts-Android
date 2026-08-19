//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A lightweight in-app toast banner that appears at the top of the screen.
 *
 * Renders a rounded pill containing an optional leading [image] icon and a [message].
 * The entire toast is tappable via [onClick], making it suitable for actionable notifications
 * (e.g. "Undo", "Retry") as well as passive ones.
 *
 * @property image Optional leading icon displayed to the left of the message. Pass `null` to show text only.
 * @property message The text content of the toast, capped at 3 lines with ellipsis overflow.
 * @property onClick Invoked when the user taps anywhere on the toast.
 */
data class GroveToast(
    val image: ImageResource? = null,
    val message: StringResource,
    val onClick: OnActionVoid,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Box(
            modifier = modifier
                .systemBarsPadding()
                .fillMaxWidth()
                .padding(Spacings.medium),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(elevation = 6.dp, shape = GroveShapes.medium)
                    .clickable(onClick = onClick)
                    .fillMaxWidth()
                    .background(color = Colors.surface, shape = GroveShapes.medium)
                    .padding(horizontal = Spacings.large)
                    .padding(vertical = Spacings.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
            ) {
                image?.Content(modifier = Modifier.size(28.dp))
                Text(
                    text = message.text(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Controls how long a [GroveToast] remains visible after being shown via
 * [MutableGroveScaffoldState.showToast].
 *
 * Pass one of the built-in constants or construct a custom [AutoDismissible] duration:
 * ```kotlin
 * scaffoldState.showToast(
 *     message = StringResource(Strings.saved),
 *     displayStyle = GroveToastDisplayStyle.DefaultShort,
 * )
 * ```
 */
sealed interface GroveToastDisplayStyle {

    /**
     * The toast remains visible until the user taps it or [MutableGroveScaffoldState.hideToast]
     * is called explicitly.
     */
    data object Sticky : GroveToastDisplayStyle

    /**
     * The toast is automatically dismissed after [after] has elapsed.
     *
     * @property after The duration to wait before hiding the toast.
     */
    data class AutoDismissible(val after: Duration) : GroveToastDisplayStyle

    companion object {
        /** Short auto-dismiss preset — suitable for brief confirmations (2 seconds). */
        val DefaultShort = AutoDismissible(after = 2.seconds)

        /** Long auto-dismiss preset — suitable for messages requiring slightly more reading time (3.5 seconds). */
        val DefaultLong = AutoDismissible(after = 3.5.seconds)
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val toast = GroveToast(
        image = ImageResource(Icons.Default.Error),
        message = StringResource("This is a toast message"),
        onClick = {}
    )

    GroveTheme { toast.Content() }
}
