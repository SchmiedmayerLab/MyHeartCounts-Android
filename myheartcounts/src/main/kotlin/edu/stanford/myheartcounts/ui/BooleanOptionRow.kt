//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.VerticalSpacer
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.medium
import kotlinx.coroutines.flow.Flow

/**
 * A unified boolean-input row that renders either a [Style.Switch] toggle or a [Style.Label] choice
 * list of [confirmLabel] / [declineLabel], depending on [style].
 */
data class BooleanOptionRow(
    val text: StringResource,
    val subtext: StringResource?,
    val value: Flow<Boolean?>,
    val onChange: (Boolean) -> Unit,
    val style: Style,
    val confirmLabel: StringResource,
    val declineLabel: StringResource,
) : ComposableContent {

    /**
     * The visual style used to present the boolean option.
     */
    enum class Style {
        /**
         * Renders a [Switch] toggle aligned to the end of the row.
         */
        Switch,

        /**
         * Renders a confirm / decline label list with a checkmark on the selected option.
         */
        Label,
    }

    @Composable
    override fun Content(modifier: Modifier) {
        when (style) {
            Style.Switch -> SwitchContent(modifier)
            Style.Label -> LabelContent(modifier)
        }
    }

    @Composable
    private fun SwitchContent(modifier: Modifier) {
        val isChecked by value.collectAsStateWithLifecycle(initialValue = false)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Spacings.medium)
                .padding(vertical = Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = text.text(),
                style = LocalTextStyle.current.medium(),
            )
            Switch(
                checked = isChecked ?: false,
                onCheckedChange = onChange,
            )
        }
    }

    @Composable
    private fun LabelContent(modifier: Modifier) {
        val current by value.collectAsStateWithLifecycle(initialValue = null)
        val choices = listOf(true to confirmLabel, false to declineLabel)
        Column(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.tiny),
            ) {
                Text(
                    modifier = Modifier.padding(top = Spacings.medium),
                    text = text.text(),
                    style = LocalTextStyle.current.medium(),
                )
                subtext?.let {
                    Text(
                        text = it.text(),
                        style = TextStyles.bodyMedium,
                        color = Colors.onSurfaceVariant,
                    )
                }
            }
            VerticalSpacer(height = Spacings.small)
            HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
            choices.forEachIndexed { index, (answer, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(answer) }
                        .padding(Spacings.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = label.text(),
                    )
                    Icon(
                        modifier = Modifier.alpha(if (current == answer) 1f else 0f),
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Colors.primary,
                    )
                }
                if (index < choices.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                }
            }
        }
    }
}
