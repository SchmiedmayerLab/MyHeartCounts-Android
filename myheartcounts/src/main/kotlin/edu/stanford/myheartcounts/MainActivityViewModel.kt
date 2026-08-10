//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.spezi.account.Account
import edu.stanford.spezi.account.observeSignOutEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Drives the app's top-level route: it determines the initial root and switches back to onboarding
 * whenever the account signs out.
 */
class MainActivityViewModel(
    private val navigator: Navigator,
    private val account: Account,
) : ViewModel() {

    private val _state = MutableStateFlow(MainActivityState(initialRoot = MHCRoute.Root.Splash))
    val state = _state.asStateFlow()

    init {
        // Dummy splash for now, will be replaced by actual logic to determine
        // the initial screen based on onboarding completion and account status.
        viewModelScope.launch {
            delay(2.seconds)
            navigator.push(NavigationEvent.SwitchRoot(MHCRoute.Root.Onboarding))
        }

        viewModelScope.launch {
            account.observeSignOutEvents().collect {
                navigator.push(NavigationEvent.SwitchRoot(MHCRoute.Root.Onboarding))
            }
        }
    }
}

/**
 * The root route the back stack is initialised with.
 */
data class MainActivityState(
    val initialRoot: MHCRoute.Root,
)
