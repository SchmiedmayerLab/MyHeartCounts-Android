//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler.fixtures

import org.grovealliance.foundation.fixtures.InstantFixtures
import org.grovealliance.scheduler.AllowedCompletionPolicy
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.TaskCategory
import org.grovealliance.scheduler.TaskContext
import org.grovealliance.scheduler.TaskDraft

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
