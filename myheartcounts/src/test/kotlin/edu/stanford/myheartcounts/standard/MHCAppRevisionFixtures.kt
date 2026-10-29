//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

/**
 * App revisions for tests, with placeholder values for every property a test leaves out.
 */
object MHCAppRevisionFixtures {

    /**
     * An [MHCAppRevision] with the given properties.
     */
    fun create(
        version: String = "1.0.0",
        build: Int = 1,
        bundleIdentifier: String = "edu.stanford.myheartcounts",
        osVersion: String = "16",
        deviceModel: String = "Pixel 8",
        deviceType: String = "shiba",
    ): MHCAppRevision = MHCAppRevision(
        version = version,
        build = build,
        bundleIdentifier = bundleIdentifier,
        osVersion = osVersion,
        deviceModel = deviceModel,
        deviceType = deviceType,
    )
}
