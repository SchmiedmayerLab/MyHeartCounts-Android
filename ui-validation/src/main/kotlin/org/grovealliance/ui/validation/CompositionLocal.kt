//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.validation

import androidx.compose.runtime.compositionLocalOf

val LocalValidationEngine =
    compositionLocalOf<ValidationEngine?> { null }

val LocalValidationEngineConfiguration =
    compositionLocalOf { ValidationEngineConfiguration.noneOf(ValidationEngine.ConfigurationOption::class.java) }
