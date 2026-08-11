//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding.comprehension

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.BooleanOptionRow
import edu.stanford.myheartcounts.ui.MHCAppTheme
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.SpeziCard
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Spacings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The consent survey step layout: an introductory [header], a list of review [questions], and a
 * [primaryButton] whose enabled state tracks whether every question has been answered correctly.
 */
data class ConsentSurveyLayout(
    val header: StringResource,
    val questions: List<BooleanOptionRow>,
    val primaryButton: Flow<AsyncTextButton>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            SpeziCard {
                Text(
                    modifier = Modifier.padding(Spacings.medium),
                    text = header.text(),
                    color = Colors.onSurfaceVariant,
                )
            }
            questions.forEach { question ->
                SpeziCard {
                    question.Content(modifier = Modifier.fillMaxWidth())
                }
            }
            PrimaryButton()
        }
    }

    @Composable
    private fun PrimaryButton() {
        val primaryButton by primaryButton.collectAsStateWithLifecycle(initialValue = null)
        primaryButton?.Content(modifier = Modifier.fillMaxWidth())
    }
}

@Preview
@Composable
private fun ConsentSurveyPreview() {
    MHCAppTheme {
        ConsentSurveyLayout(
            header = StringResource(MHCStrings.consent_survey_header),
            questions = listOf(
                BooleanOptionRow(
                    text = StringResource(MHCStrings.consent_survey_question_seek_help),
                    subtext = null,
                    value = flowOf(true),
                    onChange = { },
                    style = BooleanOptionRow.Style.Label,
                    confirmLabel = StringResource(MHCStrings.answer_true),
                    declineLabel = StringResource(MHCStrings.answer_false),
                ),
                BooleanOptionRow(
                    text = StringResource(MHCStrings.consent_survey_question_voluntary),
                    subtext = null,
                    value = flowOf(null),
                    onChange = { },
                    style = BooleanOptionRow.Style.Label,
                    confirmLabel = StringResource(MHCStrings.answer_true),
                    declineLabel = StringResource(MHCStrings.answer_false),
                ),
                BooleanOptionRow(
                    text = StringResource(MHCStrings.consent_survey_question_withdraw),
                    subtext = null,
                    value = flowOf(false),
                    onChange = { },
                    style = BooleanOptionRow.Style.Label,
                    confirmLabel = StringResource(MHCStrings.answer_true),
                    declineLabel = StringResource(MHCStrings.answer_false),
                ),
            ),
            primaryButton = flowOf(
                AsyncTextButton(
                    title = StringResource(MHCStrings.onboarding_continue),
                    action = { },
                )
            ),
        ).Content(modifier = Modifier)
    }
}
