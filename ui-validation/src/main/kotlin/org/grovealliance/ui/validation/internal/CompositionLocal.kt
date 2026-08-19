//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.validation.internal

import androidx.compose.runtime.compositionLocalOf
import org.grovealliance.ui.validation.CapturedValidationState

internal val LocalCapturedValidationStates =
    compositionLocalOf { mutableListOf<CapturedValidationState>() }
