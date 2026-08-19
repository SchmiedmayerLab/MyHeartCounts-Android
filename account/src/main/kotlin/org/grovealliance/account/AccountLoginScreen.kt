//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.grovealliance.account.internal.screen.AccountLoginAction
import org.grovealliance.account.internal.screen.AccountLoginEvent
import org.grovealliance.account.internal.screen.AccountLoginViewModel
import org.grovealliance.core.viewmodel.groveViewModel
import org.grovealliance.ui.ConsumeEvents
import org.grovealliance.ui.DismissStyle
import org.grovealliance.ui.GroveScaffold

/**
 * Renders the login screen, including sign-in and sign-up flows.
 *
 * ```kotlin
 *
 * if (!isLoggedIn) {
 *     AccountLoginScreen(
 *         onSuccess = { /* navigate to home */ },
 *         onDismiss = { /* handle dismissal */ },
 *     )
 * }
 * ```
 *
 * @param dismissStyle The style to use when dismissing the screen. Defaults to [DismissStyle.CLOSE].
 * @param onSuccess Called when the user successfully signs in or signs up.
 * @param onDismiss Called when the user dismisses the screen without signing in.
 */
@Composable
fun AccountLoginScreen(
    dismissStyle: DismissStyle = DismissStyle.CLOSE,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel = groveViewModel<AccountLoginViewModel>()

    ConsumeEvents(eventFlow = viewModel.events) { event ->
        when (event) {
            AccountLoginEvent.Dismissed -> onDismiss()
            AccountLoginEvent.Success -> onSuccess()
        }
    }
    val screen = viewModel.screen

    LaunchedEffect(dismissStyle) {
        screen.actionSink.push(action = AccountLoginAction.SetDismissStyle(dismissStyle))
    }

    GroveScaffold(state = screen.scaffoldState) {
        val layout by screen.layout.collectAsStateWithLifecycle()
        layout.Content(modifier = Modifier.fillMaxSize())
    }
}
