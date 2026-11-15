//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding.comprehension

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.BooleanOptionRow
import edu.stanford.myheartcounts.ui.TextRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSectionList
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The consent survey step layout: the introduction, the review questions and the continue button in
 * [sections], following their changes.
 */
data class ConsentSurveyLayout(
    val sections: StateFlow<GroveSectionList>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val sections by sections.collectAsStateWithLifecycle()
        sections.Content(modifier = modifier.fillMaxSize())
    }
}

private class ConsentSurveyLayoutPreviewProvider : PreviewParameterProvider<ConsentSurveyLayout> {
    private val questions = listOf(
        MHCStrings.consent_survey_question_seek_help to true,
        MHCStrings.consent_survey_question_voluntary to null,
        MHCStrings.consent_survey_question_withdraw to false,
    )
    private val base = ConsentSurveyLayout(
        sections = MutableStateFlow(
            groveSectionList {
                section(key = "header") {
                    card(key = "header") {
                        row(
                            key = "header",
                            content = TextRow(text = StringResource(MHCStrings.consent_survey_header)),
                        )
                    }
                }
                questions.forEach { (text, value) ->
                    section(key = "$text") {
                        card(key = "question") {
                            row(
                                key = "question",
                                content = BooleanOptionRow(
                                    text = StringResource(text),
                                    subtext = null,
                                    value = MutableStateFlow(value),
                                    onChange = {},
                                    style = BooleanOptionRow.Style.Label,
                                    confirmLabel = StringResource(MHCStrings.answer_true),
                                    declineLabel = StringResource(MHCStrings.answer_false),
                                ),
                            )
                        }
                    }
                }
                section(key = "continue") {
                    content(
                        key = "continue",
                        content = AsyncTextButton(
                            title = StringResource(MHCStrings.onboarding_continue),
                            action = {},
                        ),
                    )
                }
            },
        ),
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(ConsentSurveyLayoutPreviewProvider::class) layout: ConsentSurveyLayout) {
    GroveTheme { layout.Content() }
}
