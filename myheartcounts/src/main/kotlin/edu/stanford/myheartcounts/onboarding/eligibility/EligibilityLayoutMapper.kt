//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding.eligibility

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.onboarding.OnboardingAction
import edu.stanford.myheartcounts.onboarding.OnboardingAnswers
import edu.stanford.myheartcounts.onboarding.OnboardingStep
import edu.stanford.myheartcounts.onboarding.OnboardingStepInput
import edu.stanford.myheartcounts.ui.BooleanOptionRow
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.StringResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        val onChange: (OnboardingAnswers) -> Unit = {
            input.push(OnboardingAction.OnboardingAnswersChanged(it))
        }
        return EligibilityLayout(
            description = StringResource(MHCStrings.eligibility_subtitle),
            sections = listOf(
                Section(
                    title = StringResource(MHCStrings.eligibility_age_title),
                    content = yesNoRow(
                        text = StringResource(MHCStrings.eligibility_age_question),
                        subtext = null,
                        value = answers.map { it.isOfAge },
                        onChange = { onChange(answers.value.copy(isOfAge = it)) },
                        style = BooleanOptionRow.Style.Switch,
                    ),
                ),
                Section(
                    title = StringResource(MHCStrings.eligibility_region_title),
                    content = LinkRow(
                        text = StringResource(MHCStrings.eligibility_region_question),
                        valueLabel = answers.map { state -> state.country?.let { StringResource(it.name) } },
                        onClicked = { input.push(OnboardingAction.ShowCountrySelectionSheet) },
                    ),
                ),
                Section(
                    title = StringResource(MHCStrings.eligibility_language_title),
                    content = yesNoRow(
                        text = StringResource(MHCStrings.eligibility_language_question),
                        subtext = null,
                        value = answers.map { it.understandsEnglish },
                        onChange = { onChange(answers.value.copy(understandsEnglish = it)) },
                        style = BooleanOptionRow.Style.Label,
                    ),
                ),
                Section(
                    title = StringResource(MHCStrings.eligibility_account_sharing_title),
                    content = yesNoRow(
                        text = StringResource(MHCStrings.eligibility_account_sharing_question),
                        subtext = StringResource(MHCStrings.eligibility_account_sharing_explanation),
                        value = answers.map { it.usesSharedAccount },
                        onChange = { onChange(answers.value.copy(usesSharedAccount = it)) },
                        style = BooleanOptionRow.Style.Label,
                    ),
                ),
            ),
            primaryButton = answers.map {
                AsyncTextButton(
                    title = StringResource(MHCStrings.eligibility_save),
                    coroutineScope = { input.scope },
                    action = { input.advance(OnboardingStep.Eligibility) },
                    enabled = isComplete(it),
                )
            },
        )
    }

    private fun yesNoRow(
        text: StringResource,
        subtext: StringResource?,
        value: Flow<Boolean?>,
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

    private fun isComplete(answers: OnboardingAnswers): Boolean =
        answers.country != null && answers.understandsEnglish != null && answers.usesSharedAccount != null
}
