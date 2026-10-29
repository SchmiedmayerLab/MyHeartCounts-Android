//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.account.PostTrialNudges
import edu.stanford.myheartcounts.account.StudyWithdrawal
import edu.stanford.myheartcounts.account.feedback.MHCFeedback
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudySection.WithdrawalState
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.mutableScaffoldState
import org.grovealliance.ui.showErrorToast
import kotlin.time.Duration.Companion.milliseconds

/**
 * Coordinates the account overview: builds it once and carries out what it offers.
 */
class MHCAccountOverviewViewModel(
    private val navigator: Navigator,
    private val studyWithdrawal: StudyWithdrawal,
    private val postTrialNudges: PostTrialNudges,
    private val feedback: MHCFeedback,
    private val mapper: MHCAccountOverviewMapper,
) : ViewModel() {

    private val logger by groveLogger()
    private val actionSource = ActionSource(::onAction)
    private val scaffoldState = mutableScaffoldState()
    private val withdrawalState = MutableStateFlow(WithdrawalState.Idle)
    private val postTrialNudgesOptIn = MutableStateFlow(false)
    private var postTrialNudgesUpdate: Job? = null
    private val feedbackMessage = MutableStateFlow("")
    private val feedbackAppBar = MutableStateFlow(feedbackAppBar(canSend = false))
    private var isSendingFeedback = false
    private var isClosingFeedback = false

    /**
     * The account overview to render.
     */
    val screen: MHCAccountOverview = mapper.map(
        input = MHCAccountOverviewInput(
            scaffoldState = scaffoldState.asScaffoldState(),
            withdrawalState = withdrawalState.asStateFlow(),
            postTrialNudgesOptIn = postTrialNudgesOptIn.asStateFlow(),
            actionSink = actionSource.sink(),
        ),
    )

    init {
        viewModelScope.launch {
            postTrialNudges.isOptedIn.collect { postTrialNudgesOptIn.value = it }
        }
    }

    private fun onAction(action: MHCAccountOverviewAction) {
        when (action) {
            MHCAccountOverviewAction.WithdrawSelected -> withdrawalState.value = WithdrawalState.Confirming
            MHCAccountOverviewAction.WithdrawConfirmed -> withdraw()
            MHCAccountOverviewAction.WithdrawDialogDismissed -> withdrawalState.value = WithdrawalState.Idle
            is MHCAccountOverviewAction.PostTrialNudgesChanged -> changePostTrialNudges(isOptedIn = action.isOptedIn)
            MHCAccountOverviewAction.PrivacyPolicySelected ->
                navigator.push(event = NavigationEvent.OpenPrivacyPolicy)
            MHCAccountOverviewAction.SendFeedbackSelected -> showFeedback()
            MHCAccountOverviewAction.DemographicsSelected -> navigator.push(
                event = NavigationEvent.NavigateTo(route = MHCRoute.Demographics),
            )
            MHCAccountOverviewAction.ParticipationStatsSelected -> navigator.push(
                event = NavigationEvent.NavigateTo(route = MHCRoute.ParticipationStats),
            )
            MHCAccountOverviewAction.Dismissed -> navigator.push(event = NavigationEvent.Close(route = MHCRoute.AccountOverview))
        }
    }

    private fun showFeedback() {
        feedbackMessage.value = ""
        isClosingFeedback = false
        feedbackAppBar.value = feedbackAppBar(canSend = false)
        val sheet = mapper.mapFeedback(
            appBar = feedbackAppBar.asStateFlow(),
            message = feedbackMessage.asStateFlow(),
            onMessageChanged = ::changeFeedbackMessage,
            isDismissible = { isClosingFeedback || (feedbackMessage.value.isBlank() && !isSendingFeedback) },
        )
        scaffoldState.showBottomSheet(sheet = sheet)
    }

    private fun changeFeedbackMessage(message: String) {
        val couldSend = feedbackMessage.value.isNotBlank()
        val canSend = message.isNotBlank()
        feedbackMessage.value = message
        if (couldSend != canSend) feedbackAppBar.value = feedbackAppBar(canSend = canSend)
    }

    private fun feedbackAppBar(canSend: Boolean) = mapper.mapFeedbackAppBar(
        canSend = canSend,
        onClose = ::closeFeedback,
        onSend = ::sendFeedback,
    )

    private fun closeFeedback() {
        if (isSendingFeedback) return
        isClosingFeedback = true
        scaffoldState.dismissBottomSheet()
    }

    private suspend fun sendFeedback() {
        val message = feedbackMessage.value.trim()
        if (message.isEmpty() || isSendingFeedback) return
        isSendingFeedback = true
        feedback.submit(message = message)
            .onSuccess {
                isClosingFeedback = true
                scaffoldState.dismissBottomSheet()
            }
            .onFailure { throwable ->
                logger.e(throwable) { "Sending feedback failed." }
                scaffoldState.showErrorToast(message = StringResource(MHCStrings.account_feedback_error))
            }
        isSendingFeedback = false
    }

    private fun changePostTrialNudges(isOptedIn: Boolean) {
        val previous = postTrialNudgesOptIn.value
        postTrialNudgesOptIn.value = isOptedIn
        postTrialNudgesUpdate?.cancel()
        postTrialNudgesUpdate = viewModelScope.launch {
            delay(duration = POST_TRIAL_NUDGES_DEBOUNCE)
            postTrialNudges.setOptedIn(optedIn = isOptedIn)
                .onFailure { throwable ->
                    logger.e(throwable) { "Updating the post-trial nudges opt-in failed." }
                    postTrialNudgesOptIn.value = previous
                    scaffoldState.showErrorToast(message = StringResource(MHCStrings.account_post_trial_nudges_error))
                }
        }
    }

    private fun withdraw() {
        withdrawalState.value = WithdrawalState.Withdrawing
        viewModelScope.launch {
            studyWithdrawal.withdraw()
                .onSuccess { withdrawalState.value = WithdrawalState.Idle }
                .onFailure { throwable ->
                    logger.e(throwable) { "Withdrawing from the study failed." }
                    withdrawalState.value = WithdrawalState.Failed
                }
        }
    }

    private companion object {
        val POST_TRIAL_NUDGES_DEBOUNCE = 250.milliseconds
    }
}

/**
 * The data the account overview is built from.
 */
data class MHCAccountOverviewInput(
    val scaffoldState: GroveScaffoldState,
    val withdrawalState: StateFlow<WithdrawalState>,
    val postTrialNudgesOptIn: StateFlow<Boolean>,
    val actionSink: ActionSink<MHCAccountOverviewAction>,
)

/**
 * An intent raised by the account overview.
 */
sealed interface MHCAccountOverviewAction {

    data object DemographicsSelected : MHCAccountOverviewAction

    /**
     * The participant opened their participation stats and achievements.
     */
    data object ParticipationStatsSelected : MHCAccountOverviewAction

    /**
     * The participant asked to withdraw from the study.
     */
    data object WithdrawSelected : MHCAccountOverviewAction

    /**
     * The participant confirmed the withdrawal.
     */
    data object WithdrawConfirmed : MHCAccountOverviewAction

    /**
     * The participant dismissed a withdrawal dialog.
     */
    data object WithdrawDialogDismissed : MHCAccountOverviewAction

    /**
     * The participant switched the post-trial nudges opt-in to [isOptedIn].
     */
    data class PostTrialNudgesChanged(val isOptedIn: Boolean) : MHCAccountOverviewAction

    /**
     * The participant asked to read the privacy policy.
     */
    data object PrivacyPolicySelected : MHCAccountOverviewAction

    /**
     * The participant asked to send feedback.
     */
    data object SendFeedbackSelected : MHCAccountOverviewAction

    /**
     * The account overview was dismissed.
     */
    data object Dismissed : MHCAccountOverviewAction
}
