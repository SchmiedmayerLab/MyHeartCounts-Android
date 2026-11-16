//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.ExternalLink
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold
import org.grovealliance.ui.theme.medium

/**
 * The row describing the study the participant is enrolled in: its [title], its [shortExplanation]
 * where available, [enrolledSince], saying when the participant enrolled, and a [website] link.
 */
data class EnrolledStudyRow(
    val title: String,
    val shortExplanation: String?,
    val enrolledSince: StringResource,
    val website: ExternalLink,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier.padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.tiny),
        ) {
            Text(
                text = title,
                style = TextStyles.titleMedium.bold(),
            )
            shortExplanation?.let {
                Text(
                    text = it,
                    style = TextStyles.bodySmall.medium(),
                    color = Colors.onSurfaceVariant,
                )
            }
            Text(
                text = enrolledSince.text(),
                style = TextStyles.bodySmall,
                color = Colors.onSurfaceVariant,
            )
            website.Content(modifier = Modifier.padding(top = Spacings.small))
        }
    }
}

private class EnrolledStudyRowPreviewProvider : PreviewParameterProvider<EnrolledStudyRow> {
    private val base = EnrolledStudyRow(
        title = "My Heart Counts",
        shortExplanation = "Improve your cardiovascular health",
        enrolledSince = StringResource(MHCStrings.account_enrolled_since, "Jan 1, 2026, 9:30 AM"),
        website = ExternalLink(
            text = StringResource(MHCStrings.account_study_website),
            onClicked = {},
        ),
    )

    override val values = sequenceOf(
        base,
        base.copy(shortExplanation = null),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(EnrolledStudyRowPreviewProvider::class) content: EnrolledStudyRow,
) {
    GroveTheme {
        GroveCard(modifier = Modifier.padding(Spacings.medium)) {
            content.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}
