//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.foundation

/**
 * A semantic version of the form `major.minor.patch` with an optional pre-release suffix.
 *
 * Ordering compares `major`, then `minor`, then `patch` numerically. [preRelease] is not part of the
 * ordering (so `1.0.0-beta` and `1.0.0` compare equal, though they are not [equals]).
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int = 0,
    val patch: Int = 0,
    val preRelease: String? = null,
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int = when {
        major != other.major -> major.compareTo(other.major)
        minor != other.minor -> minor.compareTo(other.minor)
        else -> patch.compareTo(other.patch)
    }

    override fun toString(): String {
        val core = "$major.$minor.$patch"
        return if (preRelease != null) "$core-$preRelease" else core
    }

    companion object {
        private val PATTERN = Regex("""(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-(.+))?""")

        /**
         * Parses a version string of the form `major[.minor[.patch]][-preRelease]`.
         *
         * @return the parsed [SemanticVersion], or `null` if [versionString] is not a valid version string.
         */
        operator fun invoke(versionString: String): SemanticVersion? {
            val (major, minor, patch, preRelease) = PATTERN.matchEntire(versionString.trim())?.destructured ?: return null
            return SemanticVersion(
                major = major.toIntOrNull() ?: return null,
                minor = minor.toIntOrNull() ?: 0,
                patch = patch.toIntOrNull() ?: 0,
                preRelease = preRelease.ifEmpty { null },
            )
        }
    }
}
