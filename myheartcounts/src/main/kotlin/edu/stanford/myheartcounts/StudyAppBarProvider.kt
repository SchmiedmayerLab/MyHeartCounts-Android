//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.SpeziAppBar
import edu.stanford.spezi.ui.SpeziAppBarBuilderScope
import edu.stanford.spezi.ui.speziAppBar

/**
 * Builds the app bar shared by the study tabs, pre-configured as leading-aligned with a trailing
 * action that opens the account overview. [scope] is applied last, so callers set the title and may
 * override the defaults.
 */
interface StudyAppBarProvider {
    fun create(scope: SpeziAppBarBuilderScope.() -> Unit): SpeziAppBar
}

/**
 * Default [StudyAppBarProvider] implementation.
 */
class StudyAppBarProviderImpl(
    private val navigator: Navigator,
) : StudyAppBarProvider {
    override fun create(scope: SpeziAppBarBuilderScope.() -> Unit): SpeziAppBar = speziAppBar {
        centerAlign(value = false)
        action(imageResource = ImageResource(Icons.Default.AccountCircle)) {
            navigator.push(event = NavigationEvent.NavigateTo(MHCRoute.AccountOverview))
        }
        scope()
    }
}
