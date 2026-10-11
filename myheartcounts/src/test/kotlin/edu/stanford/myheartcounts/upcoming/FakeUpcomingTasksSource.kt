//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.upcoming

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.scheduler.Event

/**
 * An [UpcomingTasksSource] emitting configured events.
 */
class FakeUpcomingTasksSource : UpcomingTasksSource {
    private val events = MutableStateFlow<List<Event>>(emptyList())

    /**
     * Sets the events [upcomingEvents] emits.
     */
    fun setEvents(events: List<Event>) {
        this.events.value = events
    }

    override fun upcomingEvents(): Flow<List<Event>> = events
}
