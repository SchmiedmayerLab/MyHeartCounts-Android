//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.foundation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SemanticVersionTest {

    @Test
    fun `it should parse a version with all components and a pre-release`() {
        // given
        val raw = "1.0.0-beta.1"

        // when
        val version = SemanticVersion(raw)

        // then
        assertThat(version).isEqualTo(SemanticVersion(major = 1, minor = 0, patch = 0, preRelease = "beta.1"))
    }

    @Test
    fun `it should default omitted components to zero`() {
        // given
        val raw = "2"

        // when
        val version = SemanticVersion(raw)

        // then
        assertThat(version).isEqualTo(SemanticVersion(major = 2, minor = 0, patch = 0))
    }

    @Test
    fun `it should reject malformed version strings`() {
        // given
        val malformed = listOf("", "x.y", "1.2.3.4")

        // when
        val results = malformed.map { SemanticVersion(it) }

        // then
        results.forEach { assertThat(it).isNull() }
    }

    @Test
    fun `it should order by major then minor then patch`() {
        // given
        val versions = listOf(
            SemanticVersion(major = 2, minor = 0, patch = 0),
            SemanticVersion(major = 1, minor = 2, patch = 0),
            SemanticVersion(major = 1, minor = 0, patch = 5),
        )

        // when
        val ordered = versions.sorted()

        // then
        assertThat(ordered).containsExactly(
            SemanticVersion(major = 1, minor = 0, patch = 5),
            SemanticVersion(major = 1, minor = 2, patch = 0),
            SemanticVersion(major = 2, minor = 0, patch = 0),
        ).inOrder()
    }
}
