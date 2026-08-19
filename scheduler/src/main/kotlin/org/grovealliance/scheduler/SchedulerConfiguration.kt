//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler

import androidx.room.Room
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.scheduler.internal.CompletionPolicyEvaluator
import org.grovealliance.scheduler.internal.NotificationPlanner
import org.grovealliance.scheduler.internal.NotificationScheduler
import org.grovealliance.scheduler.internal.NotificationSchedulerImpl
import org.grovealliance.scheduler.internal.OccurrenceGenerator
import org.grovealliance.scheduler.internal.ScheduleCalculatorImpl
import org.grovealliance.scheduler.internal.SchedulerDatabase
import org.grovealliance.scheduler.internal.SchedulerImpl
import org.grovealliance.scheduler.internal.SchedulerMapper
import org.grovealliance.scheduler.internal.SchedulerNotificationsImpl

/**
 * Registers the [Scheduler] module in the Grove configuration.
 *
 * Resolve the registered module elsewhere via `dependency<Scheduler>()`.
 *
 * @param notifications when non-`null`, also registers [SchedulerNotifications] to deliver local
 *   notifications for tasks that opt in via [Task.scheduleNotifications]. This requires a [Concurrency]
 *   module in the graph; leaving it `null` keeps the scheduler free of that dependency.
 */
@GroveDsl
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
