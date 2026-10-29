//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.featureflag

/**
 * The environment the app runs in, which decides whether its testing tools are offered.
 */
enum class AppEnvironment {
    /**
     * Testing: feature flags can be changed, and shaking the device opens the debug sheet.
     */
    Testing,

    /**
     * Production, as distributed to participants: fixed feature flags and no debug sheet.
     */
    Production,
}
