//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveAlertDialog
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The button withdrawing from the study, showing the [confirmation] while the withdrawal is
 * [WithdrawalState.Confirming] and the [failure] once it [WithdrawalState.Failed].
 */
data class WithdrawFromStudyButton(
    val state: StateFlow<WithdrawalState>,
    val onWithdrawSelected: () -> Unit,
    val confirmation: GroveAlertDialog,
    val failure: GroveAlertDialog,
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
            WithdrawalState.Confirming -> confirmation.Dialog(Modifier)
            WithdrawalState.Failed -> failure.Dialog(Modifier)
            WithdrawalState.Idle, WithdrawalState.Withdrawing -> Unit
        }
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

private class WithdrawFromStudyButtonPreviewProvider : PreviewParameterProvider<WithdrawFromStudyButton> {
    private val base = WithdrawFromStudyButton(
        state = MutableStateFlow(value = WithdrawFromStudyButton.WithdrawalState.Idle),
        onWithdrawSelected = {},
        confirmation = GroveAlertDialog(
            title = StringResource("Withdraw from Study?"),
            message = StringResource("You will no longer take part in the study."),
            confirmButton = GroveAlertDialog.Button(
                title = StringResource("Withdraw"),
                action = {},
            ),
        ),
        failure = GroveAlertDialog(
            title = StringResource("Withdrawal Failed"),
            message = StringResource("Please try again."),
            confirmButton = GroveAlertDialog.Button(
                title = StringResource("OK"),
                action = {},
            ),
        ),
    )

    override val values = sequenceOf(
        base,
        base.copy(state = MutableStateFlow(value = WithdrawFromStudyButton.WithdrawalState.Withdrawing)),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(WithdrawFromStudyButtonPreviewProvider::class) content: WithdrawFromStudyButton) {
    GroveTheme { content.Content(modifier = Modifier.fillMaxWidth().padding(Spacings.medium)) }
}
