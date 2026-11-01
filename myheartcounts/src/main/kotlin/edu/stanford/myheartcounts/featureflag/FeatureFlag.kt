//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.featureflag

/**
 * A switch for app behaviour. [id] identifies the flag wherever its value is stored, and
 * [defaultValue] applies unless the flag was changed, which only the testing environment allows.
 */
enum class FeatureFlag(
    val id: String,
    val defaultValue: Boolean,
) {
    /**
     * Shortens onboarding to login, Health Connect access and enrollment, to reach the study
     * quickly when testing what comes after onboarding.
     */
    SimplifyOnboarding(
        id = "simplifyOnboarding",
        defaultValue = false,
    ),

    /**
     * Lets participants in the United Kingdom take part in the Imperial study variant, stored on the
     * default region's backend, instead of joining the launch waitlist.
     */
    UKStudyTesting(
        id = "ukStudyTesting",
        defaultValue = false,
    ),
}
