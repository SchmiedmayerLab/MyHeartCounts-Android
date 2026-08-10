//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study

import androidx.room.Room
import edu.stanford.spezi.core.ConfigurationBuilder
import edu.stanford.spezi.core.SpeziDsl
import edu.stanford.spezi.core.coroutines.Concurrency
import edu.stanford.spezi.study.internal.BundleStore
import edu.stanford.spezi.study.internal.ScheduleCompiler
import edu.stanford.spezi.study.internal.StudyDatabase
import edu.stanford.spezi.study.internal.StudyEnrollmentMapper
import edu.stanford.spezi.study.internal.StudyEnvironment
import edu.stanford.spezi.study.internal.StudyManagerImpl
import java.io.File
import java.util.Locale

/**
 * Registers the [StudyManager] module in the Spezi configuration.
 *
 * Requires a `Scheduler` (register via `scheduler()`), a `TimeProvider`, and a [Concurrency] module
 * to be part of the graph. Resolve the registered module elsewhere via `dependency<StudyManager>()`.
 *
 * @param preferredLocale the locale used to resolve localized bundle resources such as task titles.
 */
@SpeziDsl
fun ConfigurationBuilder.studyManager(preferredLocale: Locale = Locale.getDefault()) {
    singleton {
        Room.databaseBuilder(
            appContext(),
            StudyDatabase::class.java,
            StudyDatabase.NAME,
        ).build()
    }
    singleton {
        BundleStore(File(appContext().filesDir, STUDY_BUNDLES_DIRECTORY))
    }
    factory { StudyEnrollmentMapper() }
    factory {
        ScheduleCompiler(
            calculator = dependency(),
            timeProvider = dependency()
        )
    }
    module<StudyManager> {
        val database = dependency<StudyDatabase>()
        StudyManagerImpl(
            enrollmentDao = database.dao(),
            scheduler = dependency(),
            scheduleCompiler = dependency(),
            timeProvider = dependency(),
            bundleStore = dependency(),
            mapper = dependency(),
            scope = dependency<Concurrency>().ioCoroutineScope(),
            environment = StudyEnvironment(preferredLocale = preferredLocale),
        )
    }
}

private const val STUDY_BUNDLES_DIRECTORY = "edu.stanford.spezi.study/StudyBundles"
