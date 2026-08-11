//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.navigation

import androidx.navigation3.runtime.NavKey
import edu.stanford.spezi.ui.EventSink
import edu.stanford.spezi.ui.EventSourceFlow
import kotlinx.serialization.Serializable

/**
 * App-wide navigation dispatcher. Producers call [push]; consumers collect [events] and apply
 * each one to the back stack.
 */
interface Navigator {

    /**
     * The stream of pending navigation requests.
     */
    val events: EventSourceFlow<NavigationEvent>

    /**
     * Enqueues [event] for consumers to apply.
     */
    fun push(event: NavigationEvent)
}

/**
 * Default [Navigator] implementation.
 */
class NavigatorImpl : Navigator {
    private val eventSink = EventSink<NavigationEvent>()

    override val events = eventSink.source()

    override fun push(event: NavigationEvent) {
        eventSink.push(event)
    }
}

/**
 * A navigation request to be applied to the back stack.
 */
sealed interface NavigationEvent {

    /**
     * Removes the top destination from the back stack.
     */
    data object Pop : NavigationEvent

    /**
     * Replaces the entire back stack with [root].
     */
    data class SwitchRoot(val root: MHCRoute.Root) : NavigationEvent

    /**
     * Launches system app settings with [action].
     */
    data class LaunchAppSettings(val action: String) : NavigationEvent

    /**
     * Opens [url] in an external browser.
     */
    data class OpenUrl(val url: String) : NavigationEvent

    /**
     * Navigates to a specific route by pushing it onto the back stack
     */
    data class NavigateTo(val route: MHCRoute) : NavigationEvent
}

/**
 * Top-level destinations of the app. The active route is derived from app state rather than from a
 * user-manipulated back stack — these are mutually exclusive phases.
 */
@Serializable
sealed interface MHCRoute : NavKey {

    /**
     * The mutually exclusive root phases of the app. Exactly one is active at a time.
     */
    @Serializable
    sealed interface Root : MHCRoute {

        /**
         * The launch screen shown while the initial route is being determined.
         */
        @Serializable
        data object Splash : Root

        /**
         * The onboarding flow, shown until onboarding is complete.
         */
        @Serializable
        data object Onboarding : Root

        /**
         * The post-onboarding study experience with its tab navigation.
         */
        @Serializable
        data object Study : Root
    }

    /**
     * The account overview, shown over the active root phase.
     */
    @Serializable
    data object AccountOverview : MHCRoute
}
