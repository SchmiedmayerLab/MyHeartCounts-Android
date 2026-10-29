//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.featureflag

import org.grovealliance.storage.local.KeyValueStorage

/**
 * Tells whether a [FeatureFlag] is enabled. Values are read when asked; a changed flag takes effect
 * once the app restarts.
 */
interface FeatureFlags {

    /**
     * Whether [flag] is enabled.
     */
    fun isEnabled(flag: FeatureFlag): Boolean
}

/**
 * [FeatureFlags] that always answers with each flag's default value, so the flags cannot be changed.
 */
class DefaultFeatureFlags : FeatureFlags {

    override fun isEnabled(flag: FeatureFlag): Boolean = flag.defaultValue
}

/**
 * [FeatureFlags] reading each flag from [storage] under its id, and its default value when none is
 * stored. Flags can be changed through [setEnabled]; a change takes effect once the app restarts.
 */
class StoredFeatureFlags(
    private val storage: KeyValueStorage,
) : FeatureFlags {

    override fun isEnabled(flag: FeatureFlag): Boolean = storage.getBoolean(key = flag.id, default = flag.defaultValue)

    /**
     * Sets whether [flag] is [enabled].
     */
    fun setEnabled(
        flag: FeatureFlag,
        enabled: Boolean,
    ) {
        storage.putBoolean(key = flag.id, value = enabled)
    }
}
