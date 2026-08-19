//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.questionnaire

sealed interface CancelBehavior {
    data object Disabled : CancelBehavior
    data object ShouldConfirmCancel : CancelBehavior
    data object Cancel : CancelBehavior
}
