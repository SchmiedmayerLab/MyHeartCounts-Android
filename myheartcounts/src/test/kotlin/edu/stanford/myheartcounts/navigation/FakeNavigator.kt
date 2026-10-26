//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.navigation

import org.grovealliance.ui.EventSink
import org.grovealliance.ui.EventSourceFlow

/**
 * A [Navigator] that records every pushed event instead of delivering it.
 */
class FakeNavigator : Navigator {
    private val eventSink = EventSink<NavigationEvent>()
    private val _pushedEvents = mutableListOf<NavigationEvent>()

    /**
     * Every event pushed so far, in order.
     */
    val pushedEvents: List<NavigationEvent> get() = _pushedEvents

    override val events: EventSourceFlow<NavigationEvent> = eventSink.source()

    override fun push(event: NavigationEvent) {
        _pushedEvents += event
    }
}
