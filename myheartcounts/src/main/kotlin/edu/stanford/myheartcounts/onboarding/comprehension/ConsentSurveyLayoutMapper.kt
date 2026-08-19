//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding.comprehension

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.onboarding.OnboardingAction
import edu.stanford.myheartcounts.onboarding.OnboardingAnswers
import edu.stanford.myheartcounts.onboarding.OnboardingStep
import edu.stanford.myheartcounts.onboarding.OnboardingStepInput
import edu.stanford.myheartcounts.ui.BooleanOptionRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource

/**
 * Builds the [ComposableContent] for the consent survey step of the onboarding flow.
 */
interface ConsentSurveyLayoutMapper {
    fun map(input: OnboardingStepInput): ComposableContent
}

/**
 * Default [ConsentSurveyLayoutMapper] implementation.
 */
class ConsentSurveyLayoutMapperImpl : ConsentSurveyLayoutMapper {

    override fun map(input: OnboardingStepInput): ComposableContent {
        val answers = input.answers
        val onChange: (OnboardingAnswers) -> Unit = {
            input.push(OnboardingAction.OnboardingAnswersChanged(it))
        }
        return ConsentSurveyLayout(
            header = StringResource(MHCStrings.consent_survey_header),
            questions = listOf(
                trueFalseRow(
                    text = StringResource(MHCStrings.consent_survey_question_seek_help),
                    value = answers.map { it.seeksHelpWhenUnwell },
                    onChange = { onChange(answers.value.copy(seeksHelpWhenUnwell = it)) },
                ),
                trueFalseRow(
                    text = StringResource(MHCStrings.consent_survey_question_voluntary),
                    value = answers.map { it.participationIsVoluntary },
                    onChange = { onChange(answers.value.copy(participationIsVoluntary = it)) },
                ),
                trueFalseRow(
                    text = StringResource(MHCStrings.consent_survey_question_withdraw),
                    value = answers.map { it.canWithdrawAnytime },
                    onChange = { onChange(answers.value.copy(canWithdrawAnytime = it)) },
                ),
            ),
            primaryButton = answers.map {
                AsyncTextButton(
                    title = StringResource(MHCStrings.onboarding_continue),
                    coroutineScope = { input.scope },
                    action = { input.advance(OnboardingStep.Comprehension) },
                    enabled = isComplete(it),
                )
            },
        )
    }

    private fun trueFalseRow(
        text: StringResource,
        value: Flow<Boolean?>,
        onChange: (Boolean) -> Unit,
    ) = BooleanOptionRow(
        text = text,
        subtext = null,
        value = value,
        onChange = onChange,
        style = BooleanOptionRow.Style.Label,
        confirmLabel = StringResource(MHCStrings.answer_true),
        declineLabel = StringResource(MHCStrings.answer_false),
    )

    private fun isComplete(answers: OnboardingAnswers): Boolean =
        answers.seeksHelpWhenUnwell == true &&
            answers.participationIsVoluntary == true &&
            answers.canWithdrawAnytime == true
}
