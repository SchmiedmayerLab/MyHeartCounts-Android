//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

/**
 * A step outside the scheduled study tasks that the participant is encouraged to take, such as
 * completing their demographics, in its current [state]; [isRejected] once the participant asked
 * not to be suggested it again.
 */
data class PromptedAction(
    val type: PromptedActionType,
    val state: PromptedActionState,
    val isRejected: Boolean,
)

/**
 * The steps a participant can be prompted to take, each identified by its stable [id].
 */
enum class PromptedActionType(val id: String) {
    CompleteDemographics(id = "demographics"),
    VerifyAccountEmail(id = "email-verification"),
}

/**
 * Where a participant stands with a [PromptedAction].
 */
enum class PromptedActionState {

    /**
     * The step can be taken and has not been yet.
     */
    Pending,

    /**
     * The step has been taken.
     */
    Completed,

    /**
     * The step can't be taken at the moment, such as before the study was activated on this device.
     */
    Unavailable,
}
