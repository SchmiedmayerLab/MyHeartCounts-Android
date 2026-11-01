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
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.ItemDisplayRowComposable
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The account overview section about the participant's part in the study, opening their
 * participation stats and achievements through [onParticipationStatsSelected].
 */
data class StudyParticipationSection(
    val onParticipationStatsSelected: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier) {
            ItemDisplayRowComposable(
                modifier = Modifier.fillMaxWidth(),
                leadingImage = ImageResource(image = Icons.Default.MilitaryTech),
                label = stringResource(MHCStrings.account_view_participation_stats),
                valueContent = null,
                showArrow = true,
                onClick = onParticipationStatsSelected,
            )
        }
    }
}

private class StudyParticipationSectionPreviewProvider : PreviewParameterProvider<StudyParticipationSection> {
    private val base = StudyParticipationSection(onParticipationStatsSelected = {})

    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(StudyParticipationSectionPreviewProvider::class) content: StudyParticipationSection,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
