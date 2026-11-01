//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.featureflag

/**
 * [FeatureFlags] answering each flag's default value until it is set through [setEnabled].
 */
class FakeFeatureFlags : FeatureFlags {
    private val values = mutableMapOf<FeatureFlag, Boolean>()

    override fun isEnabled(flag: FeatureFlag): Boolean = values[flag] ?: flag.defaultValue

    /**
     * Sets whether [flag] is [enabled].
     */
    fun setEnabled(
        flag: FeatureFlag,
        enabled: Boolean,
    ) {
        values[flag] = enabled
    }
}
