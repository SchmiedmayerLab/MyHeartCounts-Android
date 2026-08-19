//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.scheduler

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.noRippleClickable
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold
import org.grovealliance.ui.tinted

/**
 * A summary of the actions the participant is being prompted to take, opening the full list.
 *
 * At most [MAX_DISPLAYED_ICONS] icons are shown; a larger number of [icons] is abbreviated with a
 * trailing overflow badge. An empty [icons] renders the settled state, where nothing is outstanding.
 *
 * @param icons a symbol per outstanding action, in the order the actions are listed
 * @param title the headline summarising what is outstanding
 * @param subtitle how much is outstanding, already formatted for display
 * @param onClick opens the full list of actions; omitted when nothing is outstanding
 */
data class PromptedActionsCard(
    val icons: List<ImageResource>,
    val title: StringResource,
    val subtitle: StringResource,
    val onClick: (() -> Unit)? = null,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier) {
            val rowModifier = onClick
                ?.let { Modifier.noRippleClickable(onClick = it) }
                ?: Modifier

            Row(
                modifier = rowModifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacings.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icons.isEmpty()) SettledBadge() else IconCluster()

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacings.tiny),
                ) {
                    Text(
                        text = title.text(),
                        style = TextStyles.titleMedium.bold(),
                        color = Colors.onSurface,
                    )
                    Text(
                        text = subtitle.text(),
                        style = TextStyles.bodySmall,
                        color = Colors.onSurfaceVariant,
                    )
                }

                if (icons.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Colors.onSurfaceVariant,
                        modifier = Modifier.size(Sizes.Icon.extraSmall),
                    )
                }
            }
        }
    }

    /**
     * The outstanding actions' symbols, each badge tucked behind the one before it.
     */
    @Composable
    private fun IconCluster() {
        val hasOverflow = icons.size > MAX_DISPLAYED_ICONS
        val displayed = if (hasOverflow) icons.take(MAX_DISPLAYED_ICONS - 1) else icons
        val badgeCount = displayed.size + if (hasOverflow) 1 else 0

        Row(horizontalArrangement = Arrangement.spacedBy(-BADGE_OVERLAP)) {
            displayed.forEachIndexed { index, icon ->
                Badge(
                    icon = icon,
                    containerColor = Colors.primary,
                    modifier = Modifier.zIndex((badgeCount - index).toFloat()),
                )
            }
            if (hasOverflow) {
                Badge(
                    icon = ImageResource(image = Icons.Default.MoreHoriz),
                    containerColor = Colors.onSurfaceVariant,
                )
            }
        }
    }

    /**
     * The badge shown when no action is outstanding.
     */
    @Composable
    private fun SettledBadge() {
        Badge(
            icon = ImageResource(image = Icons.Default.Check),
            containerColor = Colors.primary,
        )
    }

    @Composable
    private fun Badge(
        icon: ImageResource,
        containerColor: Color,
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier
                .size(BADGE_SIZE)
                .background(color = containerColor, shape = GroveShapes.circle)
                .border(
                    width = Sizes.Border.small,
                    color = Colors.surfaceContainerLowest,
                    shape = GroveShapes.circle,
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon.tinted(tint = Colors.onPrimary).Content(modifier = Modifier.size(Sizes.Icon.extraSmall))
        }
    }

    private companion object {
        /**
         * The largest number of badges rendered before the cluster is abbreviated.
         */
        const val MAX_DISPLAYED_ICONS = 3
        val BADGE_SIZE = 32.dp

        /**
         * How far each badge is tucked behind the one before it.
         */
        val BADGE_OVERLAP = 11.dp
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val outstanding = PromptedActionsCard(
        icons = listOf(
            ImageResource(image = Icons.Default.Notifications),
            ImageResource(image = Icons.Default.MonitorHeart),
            ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
            ImageResource(image = Icons.AutoMirrored.Filled.DirectionsWalk),
        ),
        title = StringResource("Finish setting up"),
        subtitle = StringResource("4 steps remaining"),
        onClick = {},
    )

    val settled = PromptedActionsCard(
        icons = emptyList(),
        title = StringResource("You're all set"),
        subtitle = StringResource("Nothing left to set up"),
    )

    GroveTheme {
        Column(
            modifier = Modifier.padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            outstanding.Content(modifier = Modifier.fillMaxWidth())
            settled.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}
