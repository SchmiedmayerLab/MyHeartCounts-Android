//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.stats

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import edu.stanford.myheartcounts.dashboard.ui.DashboardSectionList
import edu.stanford.myheartcounts.dashboard.ui.LabeledValue
import edu.stanford.myheartcounts.dashboard.ui.TextBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.horizontalSlideBackward
import org.grovealliance.ui.horizontalSlideForward
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The participation stats sheet, showing the content of the current [page] and sliding between
 * pages. Going back from a page other than [ParticipationStatsPage.Stats] raises
 * [ParticipationStatsAction.Back].
 *
 * @param scaffoldState the sheet's scaffold, app bar included
 * @param page the page on screen
 * @param stats the stats themselves
 * @param explainer what the stats cover
 * @param achievements every achievement
 * @param actionSink receives the sheet's intents
 */
data class ParticipationStatsScreenContent(
    val scaffoldState: GroveScaffoldState,
    val page: StateFlow<ParticipationStatsPage>,
    val stats: DashboardSectionList,
    val explainer: ParticipationStatsExplainer,
    val achievements: DashboardSectionList,
    val actionSink: ActionSink<ParticipationStatsAction>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveScaffold(state = scaffoldState) {
            val page by page.collectAsStateWithLifecycle()
            BackHandler(enabled = page != ParticipationStatsPage.Stats) {
                actionSink.push(ParticipationStatsAction.Back)
            }
            Column(modifier = modifier.fillMaxSize()) {
                DUMMY_DATA_BADGE.Content(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = Spacings.small),
                )
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) horizontalSlideForward else horizontalSlideBackward
                    },
                    label = "participationStatsPage",
                    modifier = Modifier.weight(1f),
                ) { shown ->
                    when (shown) {
                        ParticipationStatsPage.Stats -> stats
                        ParticipationStatsPage.Explainer -> explainer
                        ParticipationStatsPage.Achievements -> achievements
                    }.Content()
                }
            }
        }
    }

    private companion object {
        val DUMMY_DATA_BADGE = TextBadge(
            text = StringResource("DUMMY DATA"),
            color = DashboardColorPalette.orange,
        )
    }
}

private class ParticipationStatsScreenContentPreviewProvider : PreviewParameterProvider<ParticipationStatsScreenContent> {
    private val base = ParticipationStatsScreenContent(
        scaffoldState = GroveScaffoldState(),
        page = MutableStateFlow(ParticipationStatsPage.Stats),
        stats = DashboardSectionList(
            sections = listOf(
                DashboardSection(
                    title = null,
                    content = LabeledValue(
                        label = StringResource("Enrolled since"),
                        value = StringResource("January 1, 2026"),
                    ),
                    footnote = null,
                ),
            ),
        ),
        explainer = ParticipationStatsExplainer(
            text = StringResource("Participation Stats cover all activity recorded since you enrolled."),
        ),
        achievements = DashboardSectionList(sections = emptyList()),
        actionSink = ActionSource<ParticipationStatsAction> {}.sink(),
    )

    override val values = sequenceOf(
        base,
        base.copy(page = MutableStateFlow(ParticipationStatsPage.Explainer)),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ParticipationStatsScreenContentPreviewProvider::class) content: ParticipationStatsScreenContent,
) {
    GroveTheme {
        content.Content()
    }
}
