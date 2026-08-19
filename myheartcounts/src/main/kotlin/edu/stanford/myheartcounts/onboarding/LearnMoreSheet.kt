//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings

/**
 * A bottom sheet showing additional explanatory [description] text beneath the given [appBar].
 */
data class LearnMoreSheet(
    val appBar: GroveAppBar,
    val description: StringResource,
) : BottomSheetComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        StaticGroveScaffold(appBar = appBar) {
            Text(
                modifier = Modifier.padding(Spacings.medium),
                text = description.text()
            )
        }
    }
}
