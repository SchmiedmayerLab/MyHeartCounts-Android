//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.fixtures

import edu.stanford.spezi.foundation.fixtures.InstantFixtures
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.scheduler.Schedule
import edu.stanford.spezi.scheduler.TaskCategory
import edu.stanford.spezi.scheduler.TaskContext
import edu.stanford.spezi.scheduler.TaskDraft

/**
 * Fixture for [TaskDraft].
 */
object TaskDraftFixtures {
    fun create(
        id: String = "",
        title: String = "",
        instructions: String = "",
        schedule: Schedule = Schedule.once(at = InstantFixtures.reference),
        category: TaskCategory? = null,
        completionPolicy: AllowedCompletionPolicy = AllowedCompletionPolicy.SAME_DAY,
        tags: List<String> = emptyList(),
        context: TaskContext = TaskContext(),
    ): TaskDraft = TaskDraft(
        id = id,
        title = title,
        instructions = instructions,
        schedule = schedule,
        category = category,
        completionPolicy = completionPolicy,
        tags = tags,
        context = context,
    )
}
