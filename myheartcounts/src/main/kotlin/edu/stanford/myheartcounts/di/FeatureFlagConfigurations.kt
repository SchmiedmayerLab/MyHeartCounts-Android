//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.featureflag.AppEnvironment
import edu.stanford.myheartcounts.featureflag.DefaultFeatureFlags
import edu.stanford.myheartcounts.featureflag.FeatureFlags
import edu.stanford.myheartcounts.featureflag.StoredFeatureFlags
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.storage.local.KeyValueStorageFactory
import org.grovealliance.storage.local.KeyValueStorageType

/**
 * Registers the app's [FeatureFlags] for the app's [AppEnvironment]: fixed defaults in production,
 * values stored on the device in testing.
 */
fun ConfigurationBuilder.featureFlagConfigurations() {
    singleton<FeatureFlags> {
        when (dependency<AppEnvironment>()) {
            AppEnvironment.Production -> DefaultFeatureFlags()
            AppEnvironment.Testing -> StoredFeatureFlags(
                storage = dependency<KeyValueStorageFactory>().create(
                    fileName = FEATURE_FLAGS_STORAGE,
                    type = KeyValueStorageType.UNENCRYPTED,
                ),
            )
        }
    }
}

private const val FEATURE_FLAGS_STORAGE = "edu.stanford.myheartcounts.FEATURE_FLAGS"
