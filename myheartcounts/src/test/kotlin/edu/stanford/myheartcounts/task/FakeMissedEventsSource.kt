//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.InstantRange

/**
 * A [MissedEventsSource] emitting configured events for every window.
 */
class FakeMissedEventsSource : MissedEventsSource {
    private val events = MutableStateFlow<List<Event>>(emptyList())

    /**
     * Sets the events [missedEvents] emits.
     */
    fun setEvents(events: List<Event>) {
        this.events.value = events
    }

    override fun missedEvents(today: InstantRange): Flow<List<Event>> = events
}
