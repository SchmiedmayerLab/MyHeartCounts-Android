//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Sizes
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.ThemePreviews

data class ItemDisplayRow(
    val leadingImage: ImageResource?,
    val label: StringResource,
    val value: StringResource?,
    val showArrow: Boolean = false,
    val onClick: (() -> Unit)? = null,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        ItemDisplayRowComposable(
            modifier = modifier,
            leadingImage = leadingImage,
            label = label.text(),
            valueContent = value?.let { resource ->
                {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = resource.text(),
                        color = Colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            showArrow = showArrow,
            onClick = onClick,
        )
    }
}

@Composable
fun ItemDisplayRowComposable(
    modifier: Modifier = Modifier,
    leadingImage: ImageResource?,
    label: String,
    valueContent: @Composable (RowScope.() -> Unit)?,
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = if (onClick != null) {
        modifier.noRippleClickable(onClick = onClick)
    } else {
        modifier
    }
    Row(
        modifier = rowModifier.padding(Spacings.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacings.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingImage?.Content(modifier = Modifier.size(Sizes.Icon.small))
        Text(text = label)
        Spacer(modifier = Modifier.weight(1f))
        valueContent?.invoke(this)
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                modifier = Modifier.size(Sizes.Icon.extraSmall),
                tint = Colors.onSurfaceVariant,
                contentDescription = null,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val row = ItemDisplayRow(
        leadingImage = ImageResource(Icons.Default.AccountCircle),
        label = StringResource("Your account"),
        value = StringResource("john.smith2"),
        showArrow = true,
        onClick = {},
    )

    SpeziTheme {
        row.Content(modifier = Modifier.fillMaxWidth())
    }
}
