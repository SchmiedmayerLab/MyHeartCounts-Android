//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.ItemDisplayRow
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSection

/**
 * Builds the account overview's support section; by default offering a language change.
 */
object SupportSectionFixtures {
    fun create(
        offersLanguageChange: Boolean = true,
    ) = groveSection(key = "support") {
        card(key = "support") {
            if (offersLanguageChange) row(row = changeLanguage)
            row(row = sendFeedback)
        }
    }

    private val changeLanguage = CardRow(
        key = "change-language",
        content = ItemDisplayRow(
            leadingImage = ImageResource(image = Icons.Default.Language),
            label = StringResource(MHCStrings.account_change_language),
            value = null,
            showArrow = true,
            onClick = {},
        ),
    )

    private val sendFeedback = CardRow(
        key = "send-feedback",
        content = ItemDisplayRow(
            leadingImage = ImageResource(image = Icons.Outlined.ChatBubbleOutline),
            label = StringResource(MHCStrings.account_send_feedback),
            value = null,
            showArrow = true,
            onClick = {},
        ),
    )
}
