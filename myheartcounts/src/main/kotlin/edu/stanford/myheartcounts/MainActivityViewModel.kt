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
import edu.stanford.myheartcounts.onboarding.OnboardingCompletion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.grovealliance.account.Account
import org.grovealliance.account.observeSignOutEvents

/**
 * Drives the app's top-level route: the study once onboarding has been completed, onboarding
 * otherwise, and back to onboarding whenever the account signs out.
 */
class MainActivityViewModel(
    private val navigator: Navigator,
    private val account: Account,
    onboardingCompletion: OnboardingCompletion,
) : ViewModel() {

    private val _state = MutableStateFlow(
        MainActivityState(
            initialRoot = if (onboardingCompletion.isComplete) MHCRoute.Root.Study else MHCRoute.Root.Onboarding,
        )
    )
    val state = _state.asStateFlow()

    init {
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
