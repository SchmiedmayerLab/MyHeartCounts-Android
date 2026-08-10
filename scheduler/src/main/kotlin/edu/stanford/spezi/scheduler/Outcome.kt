//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

import java.time.Instant
import java.util.UUID

/**
 * The record of an [Event] having been completed.
 *
 * There is at most one outcome per occurrence of a task. Additional information can be stored in
 * the outcome's [context].
 */
data class Outcome(
    val id: UUID,
    val taskId: String,
    val occurrenceStartDate: Instant,
    val completionDate: Instant,
    val context: OutcomeContext,
)
