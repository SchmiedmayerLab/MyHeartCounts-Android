//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import org.grovealliance.scheduler.Event
import org.grovealliance.ui.MutableGroveScaffoldState

/**
 * An [EventPerformer] that records every request instead of performing it.
 */
class FakeEventPerformer : EventPerformer {
    private val _requests = mutableListOf<Request>()

    /**
     * Every request passed to [perform], in order.
     */
    val requests: List<Request> get() = _requests

    override suspend fun perform(
        event: Event,
        completesEvent: Boolean,
        scaffoldState: MutableGroveScaffoldState,
    ) {
        _requests += Request(
            event = event,
            completesEvent = completesEvent,
        )
    }

    /**
     * A recorded call of [perform].
     */
    data class Request(
        val event: Event,
        val completesEvent: Boolean,
    )
}
