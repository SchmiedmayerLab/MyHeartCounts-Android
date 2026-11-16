//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.navigation.ExternalAppIntentMapper
import edu.stanford.myheartcounts.navigation.ExternalAppIntentMapperImpl
import edu.stanford.myheartcounts.navigation.ExternalAppSource
import edu.stanford.myheartcounts.navigation.ExternalAppSourceImpl
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.navigation.NavigatorImpl
import org.grovealliance.core.ConfigurationBuilder

/**
 * Registers navigation: the navigator, and opening other apps along with telling which of them
 * are installed.
 */
fun ConfigurationBuilder.navigationConfigurations() {
    singleton<Navigator> { NavigatorImpl() }
    singleton<ExternalAppIntentMapper> {
        ExternalAppIntentMapperImpl(
            context = appContext(),
            studyVariants = dependency(),
        )
    }
    singleton<ExternalAppSource> {
        ExternalAppSourceImpl(
            context = appContext(),
            intentMapper = dependency(),
        )
    }
}
