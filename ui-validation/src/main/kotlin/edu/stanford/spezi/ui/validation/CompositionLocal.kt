//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.validation

import androidx.compose.runtime.compositionLocalOf

val LocalValidationEngine =
    compositionLocalOf<ValidationEngine?> { null }

val LocalValidationEngineConfiguration =
    compositionLocalOf { ValidationEngineConfiguration.noneOf(ValidationEngine.ConfigurationOption::class.java) }
