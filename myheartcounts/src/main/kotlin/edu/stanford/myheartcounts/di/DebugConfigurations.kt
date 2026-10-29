//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.debug.DebugAppInfoSource
import edu.stanford.myheartcounts.debug.DebugAppInfoSourceImpl
import edu.stanford.myheartcounts.debug.DebugHealthInfoSource
import edu.stanford.myheartcounts.debug.DebugHealthInfoSourceImpl
import edu.stanford.myheartcounts.debug.DebugMapper
import edu.stanford.myheartcounts.debug.DebugMapperImpl
import edu.stanford.myheartcounts.debug.DebugViewModel
import edu.stanford.myheartcounts.debug.ShakeDetector
import edu.stanford.myheartcounts.debug.ShakeDetectorImpl
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.viewmodel.viewModel

/**
 * Registers the debug sheet that the testing environment opens on a shake: the app details it shows and the
 * shake detection that opens it.
 */
fun ConfigurationBuilder.debugConfigurations() {
    singleton<ShakeDetector> {
        ShakeDetectorImpl(
            context = appContext(),
            appLifecycle = dependency(),
            timeProvider = dependency(),
        )
    }
    factory<DebugAppInfoSource> {
        DebugAppInfoSourceImpl(
            appRevision = dependency(),
            account = dependency(),
            studyManager = dependency(),
            studyBundleProvider = dependency(),
            firebaseAppConfiguration = dependency(),
        )
    }
    factory<DebugHealthInfoSource> { DebugHealthInfoSourceImpl(health = dependency()) }
    factory<DebugMapper> { DebugMapperImpl() }
    viewModel {
        DebugViewModel(
            featureFlags = dependency(),
            appInfoSource = dependency(),
            healthInfoSource = dependency(),
            navigator = dependency(),
            environment = dependency(),
            shakeDetector = dependency(),
            mapper = dependency(),
        )
    }
}
