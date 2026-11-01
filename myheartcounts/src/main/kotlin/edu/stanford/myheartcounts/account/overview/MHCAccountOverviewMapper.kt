//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.standard.MHCAppRevision
import edu.stanford.myheartcounts.ui.ToggleSection
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.Colors

/**
 * Builds the account overview.
 */
interface MHCAccountOverviewMapper {

    /**
     * Maps [input] into the overview to render. Its sections observe the flows in [input] and every
     * interaction is reported through its action sink, so the result stays valid as the state changes.
     */
    fun map(input: MHCAccountOverviewInput): MHCAccountOverview

    /**
     * Maps the feedback [message] into the sheet for writing it, beneath [appBar]; edits are reported
     * through [onMessageChanged], and the sheet can be swiped away only while [isDismissible].
     */
    fun mapFeedback(
        appBar: StateFlow<GroveAppBar>,
        message: StateFlow<String>,
        onMessageChanged: (String) -> Unit,
        isDismissible: () -> Boolean,
    ): FeedbackSheet

    /**
     * The feedback sheet's app bar, closing through [onClose] and offering [onSend] while [canSend].
     */
    fun mapFeedbackAppBar(
        canSend: Boolean,
        onClose: () -> Unit,
        onSend: suspend () -> Unit,
    ): GroveAppBar
}

/**
 * Default [MHCAccountOverviewMapper] implementation.
 */
class MHCAccountOverviewMapperImpl(
    private val appRevision: MHCAppRevision,
) : MHCAccountOverviewMapper {

    override fun map(input: MHCAccountOverviewInput) = MHCAccountOverview(
        sections = MHCAccountOverviewSections(
            items = listOf(
                StudyParticipationSection(
                    onParticipationStatsSelected = { input.actionSink.push(MHCAccountOverviewAction.ParticipationStatsSelected) },
                ),
                postTrialNudges(input = input),
                support(input = input),
                appInfo(input = input),
                withdrawFromStudy(input = input),
            ),
        ),
        scaffoldState = input.scaffoldState,
        onDismiss = { input.actionSink.push(MHCAccountOverviewAction.Dismissed) },
    )

    override fun mapFeedback(
        appBar: StateFlow<GroveAppBar>,
        message: StateFlow<String>,
        onMessageChanged: (String) -> Unit,
        isDismissible: () -> Boolean,
    ) = FeedbackSheet(
        appBar = appBar,
        message = message,
        onMessageChanged = onMessageChanged,
        isDismissible = isDismissible,
    )

    override fun mapFeedbackAppBar(
        canSend: Boolean,
        onClose: () -> Unit,
        onSend: suspend () -> Unit,
    ) = groveAppBar {
        title(MHCStrings.account_feedback_title)
        close { onClose() }
        action(
            content = AsyncTextButton(
                title = StringResource(MHCStrings.account_feedback_send),
                enabled = canSend,
                containerColor = { Colors.transparent },
                textColor = { if (canSend) Colors.primary else Colors.onSurfaceVariant },
                action = onSend,
            ),
        )
    }

    private fun postTrialNudges(input: MHCAccountOverviewInput) = ToggleSection(
        title = StringResource(MHCStrings.account_post_trial_nudges_title),
        subtitle = StringResource(MHCStrings.account_post_trial_nudges_subtitle),
        isOn = input.postTrialNudgesOptIn,
        onToggle = { input.actionSink.push(MHCAccountOverviewAction.PostTrialNudgesChanged(isOptedIn = it)) },
    )

    private fun support(input: MHCAccountOverviewInput) = SupportSection(
        onSendFeedbackSelected = { input.actionSink.push(MHCAccountOverviewAction.SendFeedbackSelected) },
        onDemographicsSelected = { input.actionSink.push(MHCAccountOverviewAction.DemographicsSelected) },
    )

    private fun appInfo(input: MHCAccountOverviewInput) = AppInfoSection(
        version = appRevision.version,
        onPrivacyPolicySelected = { input.actionSink.push(MHCAccountOverviewAction.PrivacyPolicySelected) },
    )

    private fun withdrawFromStudy(input: MHCAccountOverviewInput) = WithdrawFromStudySection(
        state = input.withdrawalState,
        onWithdrawSelected = { input.actionSink.push(MHCAccountOverviewAction.WithdrawSelected) },
        onWithdrawConfirmed = { input.actionSink.push(MHCAccountOverviewAction.WithdrawConfirmed) },
        onDismissed = { input.actionSink.push(MHCAccountOverviewAction.WithdrawDialogDismissed) },
    )
}
