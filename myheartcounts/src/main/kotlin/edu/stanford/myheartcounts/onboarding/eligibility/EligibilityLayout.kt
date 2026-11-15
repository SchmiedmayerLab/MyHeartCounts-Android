//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding.eligibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.BooleanOptionRow
import edu.stanford.myheartcounts.ui.MHCAppTheme
import edu.stanford.myheartcounts.ui.TextRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.GroveSearchFieldComposable
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSectionList
import org.grovealliance.ui.rememberGroveAppBar
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.medium

/**
 * The eligibility step layout: the introduction, the eligibility questions and the save button in
 * [sections], following their changes.
 */
data class EligibilityLayout(
    val sections: StateFlow<GroveSectionList>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val sections by sections.collectAsStateWithLifecycle()
        sections.Content(modifier = modifier.fillMaxSize())
    }
}

/**
 * A searchable bottom-sheet country picker presenting the given [rows]; [onClose] dismisses it
 * without changing the selection.
 */
data class CountrySelectionSheet(
    val rows: List<CountryRow>,
    val onClose: () -> Unit,
) : BottomSheetComposableContent {

    /**
     * A country row: its display [label] (flag, name, and code), whether it is the currently
     * [selected] country, an [onSelect] callback, and a [matches] predicate driving the search filter.
     */
    data class CountryRow(
        val label: String,
        val selected: Boolean,
        val onSelect: () -> Unit,
        val matches: (query: String) -> Boolean,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(query, rows) {
            rows.filter { it.matches(query) }
        }
        StaticGroveScaffold(
            appBar = rememberGroveAppBar {
                title(MHCStrings.eligibility_country_selection_title)
                close { onClose() }
            },
        ) {
            Column(modifier = modifier.padding(Spacings.medium)) {
                GroveSearchFieldComposable(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacings.medium),
                    value = query,
                    placeholder = stringResource(MHCStrings.eligibility_country_selection_search_placeholder),
                    onValueChanged = { query = it },
                )
                GroveCard {
                    LazyColumn {
                        items(filtered.size) { position ->
                            val row = filtered[position]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { row.onSelect() }
                                    .padding(horizontal = Spacings.medium)
                                    .padding(vertical = Spacings.medium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = row.label)

                                Icon(
                                    modifier = Modifier.alpha(if (row.selected) 1f else 0f),
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Colors.primary,
                                )
                            }

                            if (position < filtered.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A tappable row showing [text] with an optional trailing [valueLabel] and a chevron; tapping it
 * invokes [onClicked].
 */
data class LinkRow(
    val text: StringResource,
    val valueLabel: Flow<StringResource?>,
    val onClicked: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val label by valueLabel.collectAsStateWithLifecycle(initialValue = null)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClicked() }
                .padding(Spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = text.text(),
                style = LocalTextStyle.current.medium(),
            )

            label?.let {
                Text(
                    modifier = Modifier.padding(start = Spacings.small),
                    text = it.text(),
                    color = Colors.onSurfaceVariant,
                )
            }

            Icon(
                modifier = Modifier.size(Sizes.Icon.extraSmall),
                imageVector = Icons.AutoMirrored.Default.ArrowForwardIos,
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    MHCAppTheme {
        CountrySelectionSheet(
            rows = listOf(
                CountrySelectionSheet.CountryRow(
                    label = "🇺🇸  United States",
                    selected = true,
                    onSelect = { },
                    matches = { true },
                ),
                CountrySelectionSheet.CountryRow(
                    label = "🇮🇹  Italy",
                    selected = false,
                    onSelect = { },
                    matches = { true },
                ),
            ),
            onClose = { },
        ).Content(modifier = Modifier)
    }
}

private class EligibilityLayoutPreviewProvider : PreviewParameterProvider<EligibilityLayout> {
    private val base = EligibilityLayout(
        sections = MutableStateFlow(
            groveSectionList {
                section(key = "description") {
                    card(key = "description") {
                        row(
                            key = "description",
                            content = TextRow(text = StringResource(MHCStrings.eligibility_subtitle)),
                        )
                    }
                }
                section(key = "age") {
                    title(MHCStrings.eligibility_age_title)
                    card(key = "age") {
                        row(
                            key = "age",
                            content = BooleanOptionRow(
                                text = StringResource(MHCStrings.eligibility_age_question),
                                subtext = null,
                                value = MutableStateFlow(true),
                                onChange = {},
                                style = BooleanOptionRow.Style.Switch,
                                confirmLabel = StringResource(MHCStrings.answer_yes),
                                declineLabel = StringResource(MHCStrings.answer_no),
                            ),
                        )
                    }
                }
                section(key = "region") {
                    title(MHCStrings.eligibility_region_title)
                    card(key = "region") {
                        row(
                            key = "region",
                            content = LinkRow(
                                text = StringResource(MHCStrings.eligibility_region_question),
                                valueLabel = MutableStateFlow(StringResource("Germany")),
                                onClicked = {},
                            ),
                        )
                    }
                }
                section(key = "save") {
                    content(
                        key = "save",
                        content = AsyncTextButton(
                            title = StringResource(MHCStrings.eligibility_save),
                            action = {},
                        ),
                    )
                }
            },
        ),
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun EligibilityPreview(@PreviewParameter(EligibilityLayoutPreviewProvider::class) layout: EligibilityLayout) {
    GroveTheme { layout.Content() }
}
