//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * A [ComposableContent] wrapper around an icon-only button.
 *
 * @property image The icon shown inside the button.
 * @property enabled Whether the button is interactive.
 * @property onClick Action invoked when the button is pressed.
 */
data class GroveIconButton(
    val image: ImageResource,
    val enabled: Boolean = true,
    val onClick: OnActionVoid,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveIconButtonComposable(
            modifier = modifier,
            image = image,
            enabled = enabled,
            onClick = onClick
        )
    }

    companion object {
        /**
         * Creates a [GroveIconButton] with a default close icon.
         *
         * @param onClick Action invoked when the button is pressed.
         */
        fun close(onClick: OnActionVoid) = GroveIconButton(
            image = ImageResource(Icons.Default.Close),
            onClick = onClick
        )

        /**
         * Creates a [GroveIconButton] with a default back icon.
         *
         * @param onClick Action invoked when the button is pressed.
         */
        fun back(onClick: OnActionVoid) = GroveIconButton(
            image = ImageResource(Icons.Default.ArrowBackIosNew),
            onClick = onClick
        )

        /**
         * Creates a [GroveIconButton] with a default edit icon.
         *
         * @param onClick Action invoked when the button is pressed.
         */
        fun edit(onClick: OnActionVoid) = GroveIconButton(
            image = ImageResource(Icons.Default.Edit),
            onClick = onClick
        )
    }
}

/**
 * Displays an icon-only button.
 *
 * @param image The icon shown inside the button.
 * @param modifier The [Modifier] to apply to the [IconButton].
 * @param enabled Whether the button is interactive.
 * @param onClick Action invoked when the button is pressed.
 */
@Composable
fun GroveIconButtonComposable(
    image: ImageResource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: OnActionVoid,
) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        image.Content()
    }
}

/**
 * Displays a close icon button using the default close glyph.
 *
 * @param modifier The [Modifier] to apply to the button.
 * @param onClick Action invoked when the button is pressed.
 */
@Composable
fun CloseButton(
    modifier: Modifier = Modifier,
    onClick: OnActionVoid,
) {
    GroveIconButtonComposable(
        modifier = modifier,
        image = remember { ImageResource(Icons.Default.Close) },
        onClick = onClick,
    )
}
