//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.ExternalLink
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.ItemDisplayRow
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSection
import org.grovealliance.ui.theme.Spacings

/**
 * Builds the account overview's app info section; by default for version 1.2.0.
 */
object AppInfoSectionFixtures {
    fun create(
        version: String = "1.2.0",
    ) = groveSection(key = "app-info") {
        card(key = "app-info") {
            rows(
                listOf(
                    version(version = version),
                    privacyPolicy,
                    licenseInformation,
                ),
            )
        }
    }

    private fun version(version: String) = CardRow(
        key = "version",
        content = ItemDisplayRow(
            leadingImage = ImageResource(image = Icons.Default.Info),
            label = StringResource(MHCStrings.app_name),
            value = StringResource(version),
        ),
    )

    private val privacyPolicy = CardRow(
        key = "privacy-policy",
        content = ExternalLink(
            text = StringResource(MHCStrings.account_privacy_policy),
            icon = ImageResource(image = Icons.Default.Shield),
            padding = { Spacings.medium },
            onClicked = {},
        ),
    )

    private val licenseInformation = CardRow(
        key = "license-information",
        content = ItemDisplayRow(
            leadingImage = ImageResource(image = Icons.Default.AccountBalance),
            label = StringResource(MHCStrings.license_information_title),
            value = null,
            showArrow = true,
            onClick = {},
        ),
    )
}
