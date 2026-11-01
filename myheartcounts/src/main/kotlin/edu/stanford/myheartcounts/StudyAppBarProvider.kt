//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MilitaryTech
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveAppBarBuilderScope
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar

/**
 * Builds the app bar shared by the study tabs, pre-configured as leading-aligned with a trailing
 * action that opens the account overview. [scope] configures the title, alignment and additional
 * actions, which appear before the account action.
 */
interface StudyAppBarProvider {

    /**
     * The app bar configured by [scope]. With [showsParticipationStats], an action opening the
     * participation stats and achievements precedes the account action.
     */
    fun create(
        showsParticipationStats: Boolean = false,
        scope: GroveAppBarBuilderScope.() -> Unit,
    ): GroveAppBar
}

/**
 * Default [StudyAppBarProvider] implementation.
 */
class StudyAppBarProviderImpl(
    private val navigator: Navigator,
) : StudyAppBarProvider {
    override fun create(
        showsParticipationStats: Boolean,
        scope: GroveAppBarBuilderScope.() -> Unit,
    ): GroveAppBar = groveAppBar {
        centerAlign(value = false)
        scope()
        if (showsParticipationStats) {
            action(
                imageResource = ImageResource(
                    image = Icons.Default.MilitaryTech,
                    contentDescription = StringResource(MHCStrings.participation_stats_title),
                ),
            ) {
                navigator.push(event = NavigationEvent.NavigateTo(MHCRoute.ParticipationStats))
            }
        }
        action(imageResource = ImageResource(Icons.Default.AccountCircle)) {
            navigator.push(event = NavigationEvent.NavigateTo(MHCRoute.AccountOverview))
        }
    }
}
