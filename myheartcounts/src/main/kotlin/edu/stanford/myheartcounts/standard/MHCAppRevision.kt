//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType

/**
 * Identifies the build of the app that produced an uploaded resource.
 *
 * Every uploaded resource carries it as the [URL] extension, whose child extensions `URL/version`,
 * `URL/build`, `URL/bundleIdentifier` and `URL/osVersion` hold the respective properties.
 *
 * @param version The app's version name, e.g. `4.2.1`.
 * @param build The app's version code.
 * @param bundleIdentifier The app's application id.
 * @param osVersion The operating system release the app runs on, as the system reports it.
 */
data class MHCAppRevision(
    val version: String,
    val build: Int,
    val bundleIdentifier: String,
    val osVersion: String,
) {

    /**
     * This revision as the [URL] extension.
     */
    fun toExtension(): Extension = Extension(URL).apply {
        addExtension(Extension("$URL/version", StringType(version)))
        addExtension(Extension("$URL/build", IntegerType(build)))
        addExtension(Extension("$URL/bundleIdentifier", StringType(bundleIdentifier)))
        addExtension(Extension("$URL/osVersion", StringType(osVersion)))
    }

    companion object {

        /**
         * The URL of the extension identifying the app build.
         */
        const val URL = "https://myheartcounts.stanford.edu/fhir/core/mhcAppRevision"
    }
}
