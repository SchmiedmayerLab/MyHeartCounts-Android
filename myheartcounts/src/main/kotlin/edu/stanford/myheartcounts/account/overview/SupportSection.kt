//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.ItemDisplayRowComposable
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The account overview section where the participant can reach the study team, by sending
 * feedback through [onSendFeedbackSelected].
 */
data class SupportSection(
    val onSendFeedbackSelected: () -> Unit,
    val onDemographicsSelected: () -> Unit = {},
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier) {
            Column {
                ItemDisplayRowComposable(
                    modifier = Modifier.fillMaxWidth(),
                    leadingImage = ImageResource(image = Icons.Outlined.Person),
                    label = stringResource(MHCStrings.demographics_title),
                    valueContent = null,
                    showArrow = true,
                    onClick = onDemographicsSelected,
                )
                ItemDisplayRowComposable(
                    modifier = Modifier.fillMaxWidth(),
                    leadingImage = ImageResource(image = Icons.Outlined.ChatBubbleOutline),
                    label = stringResource(MHCStrings.account_send_feedback),
                    valueContent = null,
                    showArrow = true,
                    onClick = onSendFeedbackSelected,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        SupportSection(onSendFeedbackSelected = {}).Content(modifier = Modifier.padding(Spacings.medium))
    }
}
