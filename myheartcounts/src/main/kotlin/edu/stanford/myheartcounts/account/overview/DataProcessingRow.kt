//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ItemDisplayRowComposable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A row saying that data is being processed, as [text] beside a spinner.
 */
data class DataProcessingRow(
    val text: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        ItemDisplayRowComposable(
            modifier = modifier.fillMaxWidth(),
            leadingImage = null,
            label = text.text(),
            valueContent = {
                CircularProgressIndicator(modifier = Modifier.size(Sizes.Icon.small))
            },
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        DataProcessingRow(
            text = StringResource("Processing Health Data…"),
        ).Content(modifier = Modifier)
    }
}
