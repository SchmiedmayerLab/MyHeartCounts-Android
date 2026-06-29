package edu.stanford.spezi.sample.app

import androidx.navigation3.runtime.NavKey
import edu.stanford.spezi.ui.EventSink
import kotlinx.serialization.Serializable

class Navigator {
    private val eventsSink = EventSink<NavigationEvent>()

    val events = eventsSink.source()

    fun navigateTo(event: NavigationEvent) {
        eventsSink.push(event)
    }
}

sealed interface NavigationEvent {
    data object Health : NavigationEvent
    data object AccountLogin : NavigationEvent
    data object AccountOverview : NavigationEvent
    data object PopBackStack : NavigationEvent
    data object NavigateUp : NavigationEvent
}

@Serializable
sealed interface Routes : NavKey {
    @Serializable
    data object Home : Routes

    @Serializable
    data object Health : Routes

    @Serializable
    data object AccountLogin : Routes

    @Serializable
    data object AccountOverview : Routes
}
