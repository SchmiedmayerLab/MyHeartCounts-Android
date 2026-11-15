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
import edu.stanford.myheartcounts.ui.TextRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSectionList

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
        val questions = listOf(
            CardRow(
                key = SEEK_HELP_KEY,
                content = trueFalseRow(
                    text = StringResource(MHCStrings.consent_survey_question_seek_help),
                    value = input.answer { it.seeksHelpWhenUnwell },
                    onChange = { onChange(answers.value.copy(seeksHelpWhenUnwell = it)) },
                ),
            ),
            CardRow(
                key = VOLUNTARY_KEY,
                content = trueFalseRow(
                    text = StringResource(MHCStrings.consent_survey_question_voluntary),
                    value = input.answer { it.participationIsVoluntary },
                    onChange = { onChange(answers.value.copy(participationIsVoluntary = it)) },
                ),
            ),
            CardRow(
                key = WITHDRAW_KEY,
                content = trueFalseRow(
                    text = StringResource(MHCStrings.consent_survey_question_withdraw),
                    value = input.answer { it.canWithdrawAnytime },
                    onChange = { onChange(answers.value.copy(canWithdrawAnytime = it)) },
                ),
            ),
        )
        return ConsentSurveyLayout(
            sections = answers
                .map { sections(input = input, questions = questions, isComplete = isComplete(it)) }
                .stateIn(
                    scope = input.scope,
                    started = SharingStarted.WhileSubscribed(),
                    initialValue = sections(
                        input = input,
                        questions = questions,
                        isComplete = isComplete(answers.value),
                    ),
                ),
        )
    }

    private fun sections(
        input: OnboardingStepInput,
        questions: List<CardRow<BooleanOptionRow>>,
        isComplete: Boolean,
    ): GroveSectionList = groveSectionList {
        section(key = HEADER_KEY) {
            card(key = HEADER_KEY) {
                row(
                    key = HEADER_KEY,
                    content = TextRow(text = StringResource(MHCStrings.consent_survey_header)),
                )
            }
        }
        questions.forEach { question ->
            section(key = question.key) {
                card(key = question.key) { row(row = question) }
            }
        }
        section(key = CONTINUE_KEY) {
            content(
                key = CONTINUE_KEY,
                content = AsyncTextButton(
                    title = StringResource(MHCStrings.onboarding_continue),
                    coroutineScope = { input.scope },
                    action = { input.advance(OnboardingStep.Comprehension) },
                    enabled = isComplete,
                ),
            )
        }
    }

    private fun trueFalseRow(
        text: StringResource,
        value: StateFlow<Boolean?>,
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

    private fun OnboardingStepInput.answer(read: (OnboardingAnswers) -> Boolean?): StateFlow<Boolean?> = answers
        .map(read)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = read(answers.value),
        )

    private fun isComplete(answers: OnboardingAnswers): Boolean =
        answers.seeksHelpWhenUnwell == true &&
            answers.participationIsVoluntary == true &&
            answers.canWithdrawAnytime == true

    private companion object {
        const val HEADER_KEY = "header"
        const val SEEK_HELP_KEY = "seek-help"
        const val VOLUNTARY_KEY = "voluntary"
        const val WITHDRAW_KEY = "withdraw"
        const val CONTINUE_KEY = "continue"
    }
}
