//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.consent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.grovealliance.consent.internal.ConsentAction
import org.grovealliance.consent.internal.ConsentViewModel
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * Displays the consent document that was configured via [consent] configuration and collects the signer's name and signature.
 *
 * [onConsent] is invoked with the completed [ConsentResponses] once the user submits the form.
 * It is a suspending callback, allowing callers to perform async work (e.g. persisting the
 * responses) before the screen considers consent finalised.
 */
@Composable
fun ConsentScreen(
    onConsent: suspend (ConsentResponses) -> Unit,
) {
    val viewModel = groveViewModel<ConsentViewModel>()
    val layout = viewModel.layout
    val currentOnConsent by rememberUpdatedState(onConsent)
    layout.Content(modifier = Modifier.fillMaxSize())

    LaunchedEffect(onConsent) {
        layout.actionSink.push(ConsentAction.OnConsentGivenCallbackUpdated(currentOnConsent))
    }
}
