//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.internal

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The on-device store backing the scheduler's tasks and outcomes.
 */
@Database(
    entities = [TaskEntity::class, OutcomeEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(SchedulerTypeConverters::class)
internal abstract class SchedulerDatabase : RoomDatabase() {
    abstract fun dao(): SchedulerDao

    companion object {
        const val NAME = "spezi_scheduler.db"
    }
}
