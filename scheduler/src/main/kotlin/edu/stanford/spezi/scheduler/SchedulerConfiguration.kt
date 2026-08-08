//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

import androidx.room.Room
import edu.stanford.spezi.core.ConfigurationBuilder
import edu.stanford.spezi.core.SpeziDsl
import edu.stanford.spezi.core.coroutines.Concurrency
import edu.stanford.spezi.scheduler.internal.CompletionPolicyEvaluator
import edu.stanford.spezi.scheduler.internal.NotificationPlanner
import edu.stanford.spezi.scheduler.internal.NotificationScheduler
import edu.stanford.spezi.scheduler.internal.NotificationSchedulerImpl
import edu.stanford.spezi.scheduler.internal.OccurrenceGenerator
import edu.stanford.spezi.scheduler.internal.ScheduleCalculatorImpl
import edu.stanford.spezi.scheduler.internal.SchedulerDatabase
import edu.stanford.spezi.scheduler.internal.SchedulerImpl
import edu.stanford.spezi.scheduler.internal.SchedulerMapper
import edu.stanford.spezi.scheduler.internal.SchedulerNotificationsImpl

/**
 * Registers the [Scheduler] module in the Spezi configuration.
 *
 * Resolve the registered module elsewhere via `dependency<Scheduler>()`.
 *
 * @param notifications when non-`null`, also registers [SchedulerNotifications] to deliver local
 *   notifications for tasks that opt in via [Task.scheduleNotifications]. This requires a [Concurrency]
 *   module in the graph; leaving it `null` keeps the scheduler free of that dependency.
 */
@SpeziDsl
fun ConfigurationBuilder.scheduler(
    notifications: SchedulerNotificationsConfiguration? = null,
) {
    singleton {
        Room.databaseBuilder(
            appContext(),
            SchedulerDatabase::class.java,
            SchedulerDatabase.NAME,
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
    factory { SchedulerMapper() }
    factory { OccurrenceGenerator(timeProvider = dependency()) }
    factory { CompletionPolicyEvaluator(timeProvider = dependency()) }
    singleton<ScheduleCalculator> {
        ScheduleCalculatorImpl(
            generator = dependency(),
            evaluator = dependency(),
        )
    }
    module<Scheduler> {
        SchedulerImpl(
            database = dependency(),
            timeProvider = dependency(),
            mapper = dependency(),
            scheduleCalculator = dependency(),
        )
    }
    notifications?.let { registerSchedulerNotifications(it) }
}

private fun ConfigurationBuilder.registerSchedulerNotifications(
    configuration: SchedulerNotificationsConfiguration,
) {
    singleton<NotificationScheduler> {
        NotificationSchedulerImpl(
            context = appContext(),
            timeProvider = dependency(),
            storageFactory = dependency(),
        )
    }
    factory {
        NotificationPlanner(
            timeProvider = dependency(),
            configuration = configuration,
        )
    }
    module<SchedulerNotifications> {
        SchedulerNotificationsImpl(
            scheduler = dependency(),
            timeProvider = dependency(),
            scope = dependency<Concurrency>().ioCoroutineScope(),
            notificationScheduler = dependency(),
            planner = dependency(),
            configuration = configuration,
        )
    }
}
