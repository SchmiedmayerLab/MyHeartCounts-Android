//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.foundation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ObjectIdentifierTest {
    private data class TestClass(val name: String)

    @Test
    fun `it should handle same object equality correctly`() {
        // given
        val groveInstance = TestClass("Grove")
        val otherGroveInstance = TestClass("Grove")
        val apodiniInstance = TestClass("Apodini")
        val otherApodiniInstance = TestClass("Apodini")
        val grove = ObjectIdentifier(groveInstance)
        val otherGrove = ObjectIdentifier(otherGroveInstance)
        val apodini = ObjectIdentifier(apodiniInstance)
        val otherApodini = ObjectIdentifier(otherApodiniInstance)

        // then
        assertThat(groveInstance).isEqualTo(otherGroveInstance)
        assertThat(groveInstance).isEqualTo(grove.ref)
        assertThat(apodiniInstance).isEqualTo(otherApodiniInstance)
        assertThat(apodiniInstance).isEqualTo(apodini.ref)
        assertThat(grove).isEqualTo(ObjectIdentifier(groveInstance))
        assertThat(apodini).isEqualTo(ObjectIdentifier(apodiniInstance))
        assertThat(grove).isNotEqualTo(apodini)
        assertThat(grove).isNotEqualTo(otherGrove)
        assertThat(apodini).isNotEqualTo(otherApodini)
    }
}
