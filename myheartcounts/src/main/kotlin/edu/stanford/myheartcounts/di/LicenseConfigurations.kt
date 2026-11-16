//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.standard.MHCAppRevision
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.license.LicenseProject
import org.grovealliance.license.license
import org.grovealliance.ui.StringResource

/**
 * Registers the app's third-party software and licenses, listed under the app itself.
 */
fun ConfigurationBuilder.licenseConfigurations() {
    license {
        project {
            LicenseProject(
                name = StringResource(MHCStrings.app_name),
                version = dependency<MHCAppRevision>().version,
                licenseIdentifier = "MIT",
                url = "https://github.com/SchmiedmayerLab/MyHeartCounts-Android",
            )
        }
    }
}
