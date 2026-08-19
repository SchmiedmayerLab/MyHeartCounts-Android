//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier

@Composable
fun <State : OperationState> OperationStateAlert(
    state: MutableState<State>,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
) {
    val viewState = mapOperationStateToViewState(state.value)
    ViewStateAlert(
        state = viewState.value,
        modifier = modifier,
        onClose = {
            viewState.value = ViewState.Idle
            onClose()
        }
    )
}

@Composable
fun <State : OperationState> OperationStateAlert(
    state: State,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    ViewStateAlert(
        state = state.representation,
        modifier = modifier,
        onClose = onClose
    )
}
