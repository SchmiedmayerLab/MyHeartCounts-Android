//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import org.grovealliance.resources.Strings

val LocalDefaultErrorTitle = compositionLocalOf { StringResource(Strings.viewstate_default_error_title) }
val LocalDefaultErrorMessage = compositionLocalOf { StringResource(Strings.viewstate_default_error_message) }

sealed interface ViewState {
    data object Idle : ViewState
    data object Processing : ViewState
    data class Error(val throwable: Throwable?) : ViewState {
        val errorTitle: String
            @Composable @ReadOnlyComposable get() = LocalDefaultErrorTitle.current.text()

        val errorMessage: String
            @Composable @ReadOnlyComposable get() = throwable?.localizedMessage ?: LocalDefaultErrorMessage.current.text()
    }
}
