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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.BooleanOptionRow
import edu.stanford.myheartcounts.ui.MHCAppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.GroveSearchFieldComposable
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.rememberGroveAppBar
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.medium

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
 * The eligibility step layout: an introductory [description], a list of question [sections], and a
 * [primaryButton] whose enabled state tracks whether the answers are complete.
 */
data class EligibilityLayout(
    val description: StringResource,
    val sections: List<Section>,
    val primaryButton: Flow<AsyncTextButton>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            GroveCard {
                Text(
                    modifier = Modifier.padding(Spacings.medium),
                    text = description.text(),
                    color = Colors.onSurfaceVariant,
                )
            }
            sections.forEach { it.Content(modifier = Modifier.fillMaxWidth()) }
            PrimaryButton()
        }
    }

    @Composable
    private fun PrimaryButton() {
        val primaryButton by primaryButton.collectAsStateWithLifecycle(initialValue = null)
        primaryButton?.Content(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * A titled card grouping a single piece of [content] under a [title].
 */
data class Section(
    val title: StringResource,
    val content: ComposableContent,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(modifier = modifier) {
            Text(
                modifier = Modifier.padding(vertical = Spacings.small),
                text = title.text(),
                style = LocalTextStyle.current.medium(),
                color = Colors.onSurfaceVariant,
            )

            GroveCard {
                content.Content(modifier = Modifier.fillMaxWidth())
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

@Preview
@Composable
private fun EligibilityPreview() {
    MHCAppTheme {
        EligibilityLayout(
            description = StringResource(MHCStrings.eligibility_subtitle),
            sections = listOf(
                Section(
                    title = StringResource(MHCStrings.eligibility_age_title),
                    content = BooleanOptionRow(
                        text = StringResource(MHCStrings.eligibility_age_question),
                        subtext = null,
                        value = flowOf(true),
                        onChange = { },
                        style = BooleanOptionRow.Style.Switch,
                        confirmLabel = StringResource(MHCStrings.answer_yes),
                        declineLabel = StringResource(MHCStrings.answer_no),
                    ),
                ),
                Section(
                    title = StringResource(MHCStrings.eligibility_region_title),
                    content = LinkRow(
                        text = StringResource(MHCStrings.eligibility_region_question),
                        valueLabel = flowOf(StringResource("Germany")),
                        onClicked = { },
                    ),
                ),
                Section(
                    title = StringResource(MHCStrings.eligibility_language_title),
                    content = BooleanOptionRow(
                        text = StringResource(MHCStrings.eligibility_language_question),
                        subtext = null,
                        value = flowOf(true),
                        onChange = { },
                        style = BooleanOptionRow.Style.Label,
                        confirmLabel = StringResource(MHCStrings.answer_yes),
                        declineLabel = StringResource(MHCStrings.answer_no),
                    ),
                ),
                Section(
                    title = StringResource(MHCStrings.eligibility_account_sharing_title),
                    content = BooleanOptionRow(
                        text = StringResource(MHCStrings.eligibility_account_sharing_question),
                        subtext = StringResource(MHCStrings.eligibility_account_sharing_explanation),
                        value = flowOf(false),
                        onChange = { },
                        style = BooleanOptionRow.Style.Label,
                        confirmLabel = StringResource(MHCStrings.answer_yes),
                        declineLabel = StringResource(MHCStrings.answer_no),
                    ),
                ),
            ),
            primaryButton = flowOf(
                AsyncTextButton(
                    title = StringResource(MHCStrings.eligibility_save),
                    action = { },
                )
            ),
        ).Content(modifier = Modifier)
    }
}
