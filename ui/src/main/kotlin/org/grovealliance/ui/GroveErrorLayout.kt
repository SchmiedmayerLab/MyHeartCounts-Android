//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold

/**
 * A [ComposableContent] that displays a centered error layout with an icon, title, message,
 * and action buttons.
 *
 * @property image Optional [ImageResource] shown above the title.
 * @property title The primary error title.
 * @property message Supporting error message text.
 * @property primaryButton Primary recovery action.
 * @property secondaryButton Optional secondary action.
 * @property closeButton Optional close action shown in the top-left corner.
 */
data class GroveErrorLayout(
    val image: ImageResource? = ImageResource(Icons.Default.Warning),
    val title: StringResource,
    val message: StringResource,
    val primaryButton: AsyncTextButton,
    val secondaryButton: AsyncTextButton? = null,
    val closeButton: GroveIconButton? = null,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveErrorLayoutComposable(
            modifier = modifier,
            image = image,
            title = title,
            message = message,
            primaryButton = primaryButton,
            secondaryButton = secondaryButton,
            closeButton = closeButton,
        )
    }
}

/**
 * Displays a centered error layout with an optional icon, title, message, and actions.
 *
 * @param modifier The [Modifier] to apply to the outer container.
 * @param image Optional [ImageResource] shown above the title.
 * @param title The primary error title.
 * @param message Supporting error message text.
 * @param primaryButton Primary recovery action.
 * @param secondaryButton Optional secondary action.
 * @param closeButton Optional close action shown in the top-left corner.
 */
@Composable
fun GroveErrorLayoutComposable(
    modifier: Modifier = Modifier,
    image: ImageResource? = ImageResource(Icons.Default.Warning),
    title: StringResource,
    message: StringResource,
    primaryButton: AsyncTextButton,
    secondaryButton: AsyncTextButton? = null,
    closeButton: GroveIconButton? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        closeButton?.Content(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacings.small)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacings.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacings.large),
        ) {
            image?.Content(modifier = Modifier.size(Sizes.Icon.large))

            Text(
                text = title.text(),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = TextStyles.titleLarge.bold(),
            )

            Text(
                text = message.text(),
                modifier = Modifier.fillMaxWidth(),
                color = Colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = TextStyles.bodyMedium,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                primaryButton.Content(modifier = Modifier.fillMaxWidth())
                secondaryButton?.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val layout = GroveErrorLayout(
        image = ImageResource(Icons.Default.Warning),
        title = StringResource("Something went wrong!"),
        message = StringResource("An unexpected error occurred while loading the content. Please try again later."),
        primaryButton = AsyncTextButton(
            title = StringResource("Try again"),
            action = {}
        ),
        secondaryButton = AsyncTextButton(
            title = StringResource("Dismiss"),
            action = {}
        ),
        closeButton = GroveIconButton.close { },
    )
    GroveTheme {
        layout.Content()
    }
}
