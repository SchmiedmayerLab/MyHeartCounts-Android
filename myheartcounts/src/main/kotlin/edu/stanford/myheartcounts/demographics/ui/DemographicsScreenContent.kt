//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.demographics.DemographicsField
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The demographics [layout], beneath [appBar] when there is one.
 */
data class DemographicsScreenContent(
    val appBar: GroveAppBar?,
    val layout: DemographicsLayout,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        if (appBar == null) {
            layout.Content(modifier = modifier)
        } else {
            StaticGroveScaffold(appBar = appBar) {
                layout.Content(modifier = modifier)
            }
        }
    }
}

private class DemographicsScreenContentPreviewProvider : PreviewParameterProvider<DemographicsScreenContent> {
    private val base = DemographicsScreenContent(
        appBar = groveAppBar {
            back {}
            title(StringResource("Demographics"))
        },
        layout = DemographicsLayout(
            healthImport = HealthImportCard(
                title = StringResource("Read from Health Connect"),
                description = StringResource("Use this option to fill in your answers from Health Connect."),
                onClick = {},
            ),
            sections = listOf(
                DemographicsLayout.Section(
                    rows = listOf(
                        DemographicsLayout.Row(
                            field = DemographicsField.DateOfBirth,
                            label = StringResource("Date of Birth"),
                            value = MutableStateFlow(
                                DemographicsLayout.Value(
                                    text = StringResource("Jun 15, 1990"),
                                    isOptional = false,
                                ),
                            ),
                            menu = MutableStateFlow(null),
                            accessory = DemographicsLayout.Accessory.None,
                            onClick = {},
                        ),
                    ),
                ),
            ),
            datePicker = MutableStateFlow(null),
            scaffoldState = GroveScaffoldState(),
        ),
    )
    override val values = sequenceOf(
        base,
        base.copy(appBar = null),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(DemographicsScreenContentPreviewProvider::class) content: DemographicsScreenContent,
) {
    GroveTheme { content.Content() }
}
