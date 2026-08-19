//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler

import java.time.Instant

/**
 * A single occurrence of a [Task], together with its [outcome] if it has been completed.
 *
 * Events are snapshots: completing an event through the [Scheduler] does not retroactively update
 * other event instances referring to the same occurrence.
 */
data class Event(
    val task: Task,
    val occurrence: Occurrence,
    val outcome: Outcome?,
) {
    /**
     * Whether the event has been completed, i.e. has an [outcome].
     */
    val isCompleted: Boolean get() = outcome != null

    /**
     * A stable identifier for the occurrence this event represents.
     */
    val id: Id get() = Id(
        taskId = task.id,
        occurrenceStart = occurrence.start,
    )

    /**
     * Identifies an event by its task and occurrence start.
     */
    data class Id(val taskId: String, val occurrenceStart: Instant)
}
