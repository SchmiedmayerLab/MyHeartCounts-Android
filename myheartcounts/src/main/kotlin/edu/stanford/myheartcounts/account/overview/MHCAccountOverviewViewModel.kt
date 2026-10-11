//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.account.StudyWithdrawal
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudySection.WithdrawalState
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ActionSource

/**
 * Coordinates the account overview: builds it once and carries out what it offers.
 */
class MHCAccountOverviewViewModel(
    private val navigator: Navigator,
    private val studyWithdrawal: StudyWithdrawal,
    mapper: MHCAccountOverviewMapper,
) : ViewModel() {

    private val logger by groveLogger()
    private val actionSource = ActionSource(::onAction)
    private val withdrawalState = MutableStateFlow(WithdrawalState.Idle)

    /**
     * The account overview to render.
     */
    val screen: MHCAccountOverview = mapper.map(
        input = MHCAccountOverviewInput(
            withdrawalState = withdrawalState.asStateFlow(),
            actionSink = actionSource.sink(),
        ),
    )

    private fun onAction(action: MHCAccountOverviewAction) {
        when (action) {
            MHCAccountOverviewAction.WithdrawSelected -> withdrawalState.value = WithdrawalState.Confirming
            MHCAccountOverviewAction.WithdrawConfirmed -> withdraw()
            MHCAccountOverviewAction.WithdrawDialogDismissed -> withdrawalState.value = WithdrawalState.Idle
            MHCAccountOverviewAction.Dismissed -> navigator.push(event = NavigationEvent.Close(route = MHCRoute.AccountOverview))
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
}

/**
 * The data the account overview is built from.
 */
data class MHCAccountOverviewInput(
    val withdrawalState: StateFlow<WithdrawalState>,
    val actionSink: ActionSink<MHCAccountOverviewAction>,
)

/**
 * An intent raised by the account overview.
 */
sealed interface MHCAccountOverviewAction {

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
     * The account overview was dismissed.
     */
    data object Dismissed : MHCAccountOverviewAction
}
