//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.markdown.MarkdownDocument
import edu.stanford.spezi.markdown.MarkdownDocumentView
import edu.stanford.spezi.ui.BottomSheetComposableContent
import edu.stanford.spezi.ui.DisplayedEffect
import edu.stanford.spezi.ui.OnActionVoid
import edu.stanford.spezi.ui.SpeziAppBar
import edu.stanford.spezi.ui.StaticSpeziScaffold

/**
 * A bottom sheet rendering [document] beneath the given [appBar].
 *
 * [onDisplayed] is invoked each time the sheet becomes visible, so that presenting the document can
 * be recorded.
 */
data class MarkdownSheet(
    val appBar: SpeziAppBar,
    val document: MarkdownDocument,
    val onDisplayed: OnActionVoid = {},
) : BottomSheetComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        DisplayedEffect(onDisplayed = onDisplayed)
        StaticSpeziScaffold(appBar = appBar) {
            MarkdownDocumentView(
                document = document,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
