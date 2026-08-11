//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study.internal

import java.util.Locale

/**
 * Locale and time-zone settings the study manager uses to resolve localized resources and to
 * compute schedule dates.
 */
internal data class StudyEnvironment(
    val preferredLocale: Locale = Locale.getDefault(),
)
