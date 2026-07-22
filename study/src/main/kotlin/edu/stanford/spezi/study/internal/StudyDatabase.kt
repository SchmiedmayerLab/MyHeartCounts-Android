//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study.internal

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The on-device store backing study enrollments.
 */
@Database(
    entities = [StudyEnrollmentEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class StudyDatabase : RoomDatabase() {
    abstract fun dao(): StudyEnrollmentDao

    companion object {
        const val NAME = "spezi_study.db"
    }
}
