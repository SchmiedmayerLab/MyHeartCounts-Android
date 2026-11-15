//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding.eligibility

import androidx.annotation.StringRes
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
 * Builds the [ComposableContent] for the eligibility step of the onboarding flow.
 */
interface EligibilityLayoutMapper {
    fun map(input: OnboardingStepInput): ComposableContent
}

/**
 * Default [EligibilityLayoutMapper] implementation.
 */
class EligibilityLayoutMapperImpl : EligibilityLayoutMapper {

    override fun map(input: OnboardingStepInput): ComposableContent {
        val answers = input.answers
        val questions = questions(input = input)
        return EligibilityLayout(
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

    private fun questions(input: OnboardingStepInput): List<Question> {
        val answers = input.answers
        val onChange: (OnboardingAnswers) -> Unit = {
            input.push(OnboardingAction.OnboardingAnswersChanged(it))
        }
        return listOf(
            Question(
                title = MHCStrings.eligibility_age_title,
                row = CardRow(
                    key = AGE_KEY,
                    content = yesNoRow(
                        text = StringResource(MHCStrings.eligibility_age_question),
                        subtext = null,
                        value = input.answer { it.isOfAge },
                        onChange = { onChange(answers.value.copy(isOfAge = it)) },
                        style = BooleanOptionRow.Style.Switch,
                    ),
                ),
            ),
            Question(
                title = MHCStrings.eligibility_region_title,
                row = CardRow(
                    key = REGION_KEY,
                    content = LinkRow(
                        text = StringResource(MHCStrings.eligibility_region_question),
                        valueLabel = answers.map { state -> state.country?.let { StringResource(it.name) } },
                        onClicked = { input.push(OnboardingAction.ShowCountrySelectionSheet) },
                    ),
                ),
            ),
            Question(
                title = MHCStrings.eligibility_language_title,
                row = CardRow(
                    key = LANGUAGE_KEY,
                    content = yesNoRow(
                        text = StringResource(MHCStrings.eligibility_language_question),
                        subtext = null,
                        value = input.answer { it.understandsEnglish },
                        onChange = { onChange(answers.value.copy(understandsEnglish = it)) },
                        style = BooleanOptionRow.Style.Label,
                    ),
                ),
            ),
            Question(
                title = MHCStrings.eligibility_account_sharing_title,
                row = CardRow(
                    key = ACCOUNT_SHARING_KEY,
                    content = yesNoRow(
                        text = StringResource(MHCStrings.eligibility_account_sharing_question),
                        subtext = StringResource(MHCStrings.eligibility_account_sharing_explanation),
                        value = input.answer { it.usesSharedAccount },
                        onChange = { onChange(answers.value.copy(usesSharedAccount = it)) },
                        style = BooleanOptionRow.Style.Label,
                    ),
                ),
            ),
        )
    }

    private fun sections(
        input: OnboardingStepInput,
        questions: List<Question>,
        isComplete: Boolean,
    ): GroveSectionList = groveSectionList {
        section(key = DESCRIPTION_KEY) {
            card(key = DESCRIPTION_KEY) {
                row(
                    key = DESCRIPTION_KEY,
                    content = TextRow(text = StringResource(MHCStrings.eligibility_subtitle)),
                )
            }
        }
        questions.forEach { question ->
            section(key = question.row.key) {
                title(resId = question.title)
                card(key = question.row.key) { row(row = question.row) }
            }
        }
        section(key = SAVE_KEY) {
            content(
                key = SAVE_KEY,
                content = AsyncTextButton(
                    title = StringResource(MHCStrings.eligibility_save),
                    coroutineScope = { input.scope },
                    action = { input.advance(OnboardingStep.Eligibility) },
                    enabled = isComplete,
                ),
            )
        }
    }

    private fun yesNoRow(
        text: StringResource,
        subtext: StringResource?,
        value: StateFlow<Boolean?>,
        onChange: (Boolean) -> Unit,
        style: BooleanOptionRow.Style,
    ) = BooleanOptionRow(
        text = text,
        subtext = subtext,
        value = value,
        onChange = onChange,
        style = style,
        confirmLabel = StringResource(MHCStrings.answer_yes),
        declineLabel = StringResource(MHCStrings.answer_no),
    )

    private fun OnboardingStepInput.answer(read: (OnboardingAnswers) -> Boolean?): StateFlow<Boolean?> = answers
        .map(read)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = read(answers.value),
        )

    private fun isComplete(answers: OnboardingAnswers): Boolean =
        answers.country != null && answers.understandsEnglish != null && answers.usesSharedAccount != null

    /**
     * An eligibility question: its section [title] above its [row].
     */
    private data class Question(
        @StringRes val title: Int,
        val row: CardRow<ComposableContent>,
    )

    private companion object {
        const val DESCRIPTION_KEY = "description"
        const val AGE_KEY = "age"
        const val REGION_KEY = "region"
        const val LANGUAGE_KEY = "language"
        const val ACCOUNT_SHARING_KEY = "account-sharing"
        const val SAVE_KEY = "save"
    }
}
