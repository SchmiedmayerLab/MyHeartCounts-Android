//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.validation.internal

import androidx.compose.runtime.compositionLocalOf
import edu.stanford.spezi.ui.validation.CapturedValidationState

internal val LocalCapturedValidationStates =
    compositionLocalOf { mutableListOf<CapturedValidationState>() }
