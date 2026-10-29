//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType
import org.junit.Test

class MHCAppRevisionTest {

    private val revision = MHCAppRevision(
        version = "4.2.1",
        build = 123,
        bundleIdentifier = "edu.stanford.myheartcounts",
        osVersion = "14",
    )

    @Test
    fun `identifies the build under the app revision url`() {
        // when
        val extension = revision.toExtension()

        // then
        assertThat(extension.url).isEqualTo("https://myheartcounts.stanford.edu/fhir/core/mhcAppRevision")
        assertThat(extension.value).isNull()
    }

    @Test
    fun `records each property as a child extension in a stable order`() {
        // when
        val children = revision.toExtension().extension

        // then
        assertThat(children.map { it.url })
            .containsExactly(
                "${MHCAppRevision.URL}/version",
                "${MHCAppRevision.URL}/build",
                "${MHCAppRevision.URL}/bundleIdentifier",
                "${MHCAppRevision.URL}/osVersion",
            )
            .inOrder()
        assertThat((children[0].value as StringType).value).isEqualTo("4.2.1")
        assertThat((children[1].value as IntegerType).value).isEqualTo(123)
        assertThat((children[2].value as StringType).value).isEqualTo("edu.stanford.myheartcounts")
        assertThat((children[3].value as StringType).value).isEqualTo("14")
    }
}
