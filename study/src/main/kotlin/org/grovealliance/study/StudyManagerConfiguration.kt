//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.study

import androidx.room.Room
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.study.internal.BundleStore
import org.grovealliance.study.internal.ScheduleCompiler
import org.grovealliance.study.internal.StudyDatabase
import org.grovealliance.study.internal.StudyEnrollmentMapper
import org.grovealliance.study.internal.StudyEnvironment
import org.grovealliance.study.internal.StudyManagerImpl
import java.io.File
import java.util.Locale

/**
 * Registers the [StudyManager] module in the Grove configuration.
 *
 * Requires a `Scheduler` (register via `scheduler()`), a `TimeProvider`, and a [Concurrency] module
 * to be part of the graph. Resolve the registered module elsewhere via `dependency<StudyManager>()`.
 *
 * @param preferredLocale the locale used to resolve localized bundle resources such as task titles.
 */
@GroveDsl
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

private const val STUDY_BUNDLES_DIRECTORY = "org.grovealliance.study/StudyBundles"
