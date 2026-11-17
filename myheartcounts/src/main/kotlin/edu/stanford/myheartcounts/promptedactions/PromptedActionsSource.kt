//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import edu.stanford.myheartcounts.demographics.DemographicsEditor
import edu.stanford.myheartcounts.study.StudyActivation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.isIncomplete
import org.grovealliance.account.isVerified
import org.grovealliance.study.StudyManager

/**
 * The prompted actions and where the participant stands with each.
 */
interface PromptedActionsSource {

    /**
     * Every prompted action in display order, emitted again whenever a state or a rejection changes.
     */
    val actions: Flow<List<PromptedAction>>
}

/**
 * Default [PromptedActionsSource] implementation. Every action is unavailable until the
 * participant is enrolled and the study was activated on this device. Completing the demographics
 * follows whether the [demographicsEditor]'s form is complete, once the account's details are fully
 * read; verifying the email follows the [account]'s verified flag. Each action is marked rejected
 * while the [rejections] contain it.
 */
class PromptedActionsSourceImpl(
    private val account: Account,
    private val demographicsEditor: DemographicsEditor,
    private val studyManager: StudyManager,
    private val studyActivation: StudyActivation,
    private val rejections: PromptedActionRejections,
) : PromptedActionsSource {

    override val actions: Flow<List<PromptedAction>> = combine(
        account.details,
        demographicsEditor.isComplete,
        studyManager.enrollments,
        studyActivation.activationDate,
        rejections.rejected,
    ) { details, isDemographicsComplete, enrollments, activationDate, rejected ->
        val isActive = enrollments.isNotEmpty() && activationDate != null
        PromptedActionType.entries.map { type ->
            PromptedAction(
                type = type,
                state = if (isActive) {
                    state(
                        type = type,
                        details = details,
                        isDemographicsComplete = isDemographicsComplete,
                    )
                } else {
                    PromptedActionState.Unavailable
                },
                isRejected = type in rejected,
            )
        }
    }

    private fun state(
        type: PromptedActionType,
        details: AccountDetails?,
        isDemographicsComplete: Boolean,
    ): PromptedActionState {
        details ?: return PromptedActionState.Unavailable
        return when (type) {
            PromptedActionType.CompleteDemographics -> when {
                details.isIncomplete -> PromptedActionState.Unavailable
                isDemographicsComplete -> PromptedActionState.Completed
                else -> PromptedActionState.Pending
            }
            PromptedActionType.VerifyAccountEmail ->
                if (details.isVerified) PromptedActionState.Completed else PromptedActionState.Pending
        }
    }
}
