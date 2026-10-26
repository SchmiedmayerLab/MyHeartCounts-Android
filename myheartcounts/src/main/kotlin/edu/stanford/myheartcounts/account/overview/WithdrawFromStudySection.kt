//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.theme.Colors

/**
 * The account overview section offering to withdraw from the study, including its confirmation
 * and failure dialogs.
 */
data class WithdrawFromStudySection(
    val state: StateFlow<WithdrawalState>,
    val onWithdrawSelected: () -> Unit,
    val onWithdrawConfirmed: () -> Unit,
    val onDismissed: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val state by state.collectAsStateWithLifecycle()

        AsyncTextButton(
            modifier = modifier,
            text = stringResource(MHCStrings.account_withdraw_button),
            isLoading = state == WithdrawalState.Withdrawing,
            containerColor = Colors.surfaceContainerLowest,
            textColor = Colors.error,
            onClick = onWithdrawSelected,
        )

        when (state) {
            WithdrawalState.Confirming -> ConfirmationDialog()
            WithdrawalState.Failed -> FailureDialog()
            WithdrawalState.Idle, WithdrawalState.Withdrawing -> Unit
        }
    }

    @Composable
    private fun ConfirmationDialog() {
        AlertDialog(
            title = { Text(text = stringResource(MHCStrings.account_withdraw_confirm_title)) },
            text = { Text(text = stringResource(MHCStrings.account_withdraw_confirm_message)) },
            onDismissRequest = onDismissed,
            confirmButton = {
                TextButton(onClick = onWithdrawConfirmed) {
                    Text(
                        text = stringResource(MHCStrings.account_withdraw_confirm_action),
                        color = Colors.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissed) {
                    Text(text = stringResource(MHCStrings.account_withdraw_cancel))
                }
            },
        )
    }

    @Composable
    private fun FailureDialog() {
        AlertDialog(
            title = { Text(text = stringResource(MHCStrings.account_withdraw_failed_title)) },
            text = { Text(text = stringResource(MHCStrings.account_withdraw_failed_message)) },
            onDismissRequest = onDismissed,
            confirmButton = {
                TextButton(onClick = onDismissed) {
                    Text(text = stringResource(MHCStrings.account_withdraw_failed_dismiss))
                }
            },
        )
    }

    /**
     * Where a withdrawal from the study stands.
     */
    enum class WithdrawalState {
        /**
         * Nothing has been requested.
         */
        Idle,

        /**
         * The participant is asked to confirm the withdrawal.
         */
        Confirming,

        /**
         * The withdrawal is being carried out.
         */
        Withdrawing,

        /**
         * The last withdrawal attempt failed; the participant is still signed in.
         */
        Failed,
    }
}
