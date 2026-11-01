//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ItemDisplayRowComposable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.account.AccountValueText
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A card of labelled values, one [Row] each, separated by dividers.
 */
data class DebugRowsCard(
    val rows: List<Row>,
) : ComposableContent {

    /**
     * A [label] and its [value]; a `null` value is shown as unavailable.
     */
    data class Row(
        val label: StringResource,
        val value: String?,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier) {
            rows.forEachIndexed { index, row ->
                ItemDisplayRowComposable(
                    modifier = Modifier.fillMaxWidth(),
                    leadingImage = null,
                    label = row.label.text(),
                    valueContent = {
                        AccountValueText(
                            text = row.value ?: stringResource(MHCStrings.debug_info_unavailable),
                            modifier = Modifier,
                        )
                    },
                )
                if (index < rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacings.medium),
                    )
                }
            }
        }
    }
}

private class DebugRowsCardPreviewProvider : PreviewParameterProvider<DebugRowsCard> {
    private val base = DebugRowsCard(
        rows = listOf(
            DebugRowsCard.Row(
                label = StringResource("Version"),
                value = "1.2.0",
            ),
            DebugRowsCard.Row(
                label = StringResource("Account ID"),
                value = null,
            ),
        ),
    )

    override val values = sequenceOf(
        base,
        base.copy(rows = base.rows.take(1)),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DebugRowsCardPreviewProvider::class) content: DebugRowsCard) {
    GroveTheme { content.Content(modifier = Modifier.fillMaxWidth()) }
}
