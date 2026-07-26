//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.BottomSheetComposableContent
import edu.stanford.spezi.ui.SpeziAppBar
import edu.stanford.spezi.ui.StaticSpeziScaffold
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings

/**
 * A bottom sheet showing additional explanatory [description] text beneath the given [appBar].
 */
data class LearnMoreSheet(
    val appBar: SpeziAppBar,
    val description: StringResource,
) : BottomSheetComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        StaticSpeziScaffold(appBar = appBar) {
            Text(
                modifier = Modifier.padding(Spacings.medium),
                text = description.text()
            )
        }
    }
}
