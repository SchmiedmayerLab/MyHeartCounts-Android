//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.markdown.MarkdownDocumentView
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.DisplayedEffect
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.OnActionVoid
import org.grovealliance.ui.StaticGroveScaffold

/**
 * A bottom sheet rendering [document] beneath the given [appBar].
 *
 * [onDisplayed] is invoked each time the sheet becomes visible, so that presenting the document can
 * be recorded.
 */
data class MarkdownSheet(
    val appBar: GroveAppBar,
    val document: MarkdownDocument,
    val onDisplayed: OnActionVoid = {},
) : BottomSheetComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        DisplayedEffect(onDisplayed = onDisplayed)
        StaticGroveScaffold(appBar = appBar) {
            MarkdownDocumentView(
                document = document,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
