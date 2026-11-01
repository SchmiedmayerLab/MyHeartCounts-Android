//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.ItemDisplayRowComposable
import org.grovealliance.ui.account.AccountValueText
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The account overview section about the app: its name and [version], and a link to the privacy
 * policy.
 */
data class AppInfoSection(
    val version: String,
    val onPrivacyPolicySelected: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier) {
            ItemDisplayRowComposable(
                modifier = Modifier.fillMaxWidth(),
                leadingImage = ImageResource(image = Icons.Default.Info),
                label = stringResource(MHCStrings.app_name),
                valueContent = {
                    AccountValueText(
                        text = version,
                        modifier = Modifier,
                    )
                },
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacings.medium),
            )
            ItemDisplayRowComposable(
                modifier = Modifier.fillMaxWidth(),
                leadingImage = ImageResource(image = Icons.Default.Shield),
                label = stringResource(MHCStrings.account_privacy_policy),
                valueContent = null,
                showArrow = true,
                onClick = onPrivacyPolicySelected,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        AppInfoSection(
            version = "1.2.0",
            onPrivacyPolicySelected = {},
        ).Content(modifier = Modifier.padding(Spacings.medium))
    }
}
