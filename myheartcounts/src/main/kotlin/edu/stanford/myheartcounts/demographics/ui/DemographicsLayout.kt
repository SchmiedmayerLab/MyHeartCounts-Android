//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.demographics.DemographicsField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The demographics questions: the [healthImport] card above the question [sections], each shown as
 * its own card, with the [datePicker] open on one of the questions, if any.
 *
 * @param healthImport the card offering to fill in answers from health data
 * @param sections the question sections, in display order
 * @param datePicker the date picker currently open
 * @param scaffoldState carries the sheets shown above the layout
 */
data class DemographicsLayout(
    val healthImport: HealthImportCard,
    val sections: List<Section>,
    val datePicker: StateFlow<DemographicsDatePicker?>,
    val scaffoldState: GroveScaffoldState,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val datePicker by datePicker.collectAsStateWithLifecycle()

        GroveScaffold(state = scaffoldState) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(Colors.surfaceContainer),
                contentPadding = PaddingValues(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.large),
            ) {
                item {
                    healthImport.Content(modifier = Modifier.fillMaxWidth())
                }
                items(sections) { section ->
                    section.Content(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        datePicker?.Content()
    }

    /**
     * A card of related question [rows], separated by hairline dividers.
     */
    data class Section(
        val rows: List<Row>,
    ) : ComposableContent {

        @Composable
        override fun Content(modifier: Modifier) {
            GroveCard(
                modifier = modifier,
                shape = GroveShapes.large,
            ) {
                Column {
                    rows.forEachIndexed { index, row ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                        }
                        row.Content(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    /**
     * The question for [field], titled [label], with its current [value].
     *
     * @param field the question
     * @param label the question's title
     * @param value the answer as it currently stands
     * @param menu the menu of answers open on this row, if any
     * @param accessory the trailing indicator of how the answer is edited
     * @param onClick called when the row is tapped
     */
    data class Row(
        val field: DemographicsField,
        val label: StringResource,
        val value: StateFlow<Value>,
        val menu: StateFlow<DemographicsMenu?>,
        val accessory: Accessory,
        val onClick: () -> Unit,
    ) : ComposableContent {

        @Composable
        override fun Content(modifier: Modifier) {
            val value by value.collectAsStateWithLifecycle()
            val menu by menu.collectAsStateWithLifecycle()

            BoxWithConstraints(
                modifier = modifier
                    .clickable(onClick = onClick)
                    .padding(Spacings.medium),
            ) {
                val valueMaxWidth = maxWidth * VALUE_MAX_WIDTH_FRACTION
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacings.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacings.tiny),
                    ) {
                        Text(
                            text = label.text(),
                            style = TextStyles.bodyLarge,
                            color = Colors.onSurface,
                        )
                        if (value.isOptional) {
                            Text(
                                text = stringResource(MHCStrings.demographics_optional),
                                style = TextStyles.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = Colors.onSurfaceVariant,
                            )
                        }
                    }
                    Box(modifier = Modifier.widthIn(max = valueMaxWidth)) {
                        Text(
                            text = value.text.text(),
                            style = TextStyles.bodyLarge,
                            color = Colors.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = VALUE_MAX_LINES,
                            overflow = TextOverflow.Ellipsis,
                        )
                        menu?.Content()
                    }
                    when (accessory) {
                        Accessory.None -> Unit
                        Accessory.Menu -> Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            modifier = Modifier.size(Sizes.Icon.extraSmall),
                            tint = Colors.primary,
                            contentDescription = null,
                        )
                        Accessory.Disclosure -> Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            modifier = Modifier.size(Sizes.Icon.extraSmall),
                            tint = Colors.onSurfaceVariant,
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        private companion object {
            const val VALUE_MAX_LINES = 2
            const val VALUE_MAX_WIDTH_FRACTION = 0.5f
        }
    }

    /**
     * A [Row]'s answer; an optional question is marked as such below its title.
     *
     * @param text the answer, or what stands in for a missing one
     * @param isOptional whether the form is complete without an answer
     */
    data class Value(
        val text: StringResource,
        val isOptional: Boolean,
    )

    /**
     * How a [Row]'s answer is edited.
     */
    enum class Accessory {
        /**
         * Without a trailing indicator, in place or in a dialog.
         */
        None,

        /**
         * From a menu of options anchored to the row.
         */
        Menu,

        /**
         * On a screen of its own.
         */
        Disclosure,
    }
}

private class DemographicsLayoutPreviewProvider : PreviewParameterProvider<DemographicsLayout> {
    private val row = DemographicsLayout.Row(
        field = DemographicsField.GenderIdentity,
        label = StringResource("Gender Identity"),
        value = MutableStateFlow(
            DemographicsLayout.Value(
                text = StringResource("—"),
                isOptional = false,
            ),
        ),
        menu = MutableStateFlow(null),
        accessory = DemographicsLayout.Accessory.Menu,
        onClick = {},
    )
    private val base = DemographicsLayout(
        healthImport = HealthImportCard(
            title = StringResource("Read from Health Connect"),
            description = StringResource("Use this option to fill in your answers from Health Connect."),
            onClick = {},
        ),
        sections = listOf(
            DemographicsLayout.Section(
                rows = listOf(
                    row,
                    row.copy(
                        field = DemographicsField.BloodType,
                        label = StringResource("Blood Type"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("A+"),
                                isOptional = true,
                            ),
                        ),
                    ),
                ),
            ),
            DemographicsLayout.Section(
                rows = listOf(
                    row.copy(
                        field = DemographicsField.Comorbidities,
                        label = StringResource("Comorbidities"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("Not Set"),
                                isOptional = true,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Disclosure,
                    ),
                ),
            ),
        ),
        datePicker = MutableStateFlow(null),
        scaffoldState = GroveScaffoldState(),
    )
    override val values = sequenceOf(
        base,
        base.copy(
            sections = listOf(
                DemographicsLayout.Section(
                    rows = listOf(
                        row.copy(
                            menu = MutableStateFlow(
                                DemographicsMenu(
                                    field = DemographicsField.GenderIdentity,
                                    options = listOf(
                                        DemographicsMenu.Option(
                                            index = 0,
                                            label = StringResource("Female"),
                                            isSelected = true,
                                            isFollowedByDivider = false,
                                        ),
                                        DemographicsMenu.Option(
                                            index = 1,
                                            label = StringResource("Male"),
                                            isSelected = false,
                                            isFollowedByDivider = false,
                                        ),
                                    ),
                                    onSelect = {},
                                    onDismiss = {},
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DemographicsLayoutPreviewProvider::class) content: DemographicsLayout) {
    GroveTheme { content.Content() }
}
