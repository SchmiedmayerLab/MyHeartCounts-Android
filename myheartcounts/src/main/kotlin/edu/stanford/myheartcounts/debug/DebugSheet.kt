//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.ToggleRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSectionList
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A bottom sheet for development and support showing its [sections] beneath [appBar], following
 * them while the sheet is shown. [onDismiss] is called once the sheet is swiped or tapped away.
 */
data class DebugSheet(
    val appBar: GroveAppBar,
    val sections: StateFlow<GroveSectionList>,
    override val onDismiss: () -> Unit,
) : BottomSheetComposableContent {

    override val skipPartiallyExpanded: Boolean = true

    @Composable
    override fun Content(modifier: Modifier) {
        val sections by sections.collectAsStateWithLifecycle()

        StaticGroveScaffold(appBar = appBar) {
            sections.Content(modifier = modifier)
        }
    }
}

private class DebugSheetPreviewProvider : PreviewParameterProvider<DebugSheet> {
    private val base = DebugSheet(
        appBar = groveAppBar { title(StringResource("Debug")) },
        sections = MutableStateFlow(
            groveSectionList {
                section(key = "shake") {
                    title("Debug Sheet")
                    card(key = "toggle") {
                        row(
                            key = "shake-to-open",
                            content = ToggleRow(
                                title = StringResource("Open on Shake"),
                                subtitle = StringResource("Shaking the phone opens this sheet."),
                                isOn = MutableStateFlow(true),
                                onToggle = {},
                            ),
                        )
                    }
                }
                section(key = "app-info") {
                    title("App Info")
                    card(key = "app") {
                        row(
                            key = "version",
                            content = DebugInfoRow(
                                label = StringResource("Version"),
                                value = StringResource("1.2.0"),
                            ),
                        )
                    }
                }
            },
        ),
        onDismiss = {},
    )

    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DebugSheetPreviewProvider::class) content: DebugSheet) {
    GroveTheme { content.Content(modifier = Modifier) }
}
