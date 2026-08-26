//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.foundation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TypeReferenceTests {

    @Test
    fun `it should handle same type equality correctly`() {
        // given
        val stringList1 = typeReference<List<String>>()
        val stringList2 = typeReference<List<String>>()

        // then
        assertThat(stringList1).isEqualTo(stringList2)
        assertThat(setOf(stringList1, stringList2)).hasSize(1)
    }

    @Test
    fun `it should handle different type building correctly`() {
        // given
        val stringList = typeReference<List<String>>()
        val intList = typeReference<List<Int>>()
        val stringSet = typeReference<Set<String>>()
        val intSet = typeReference<Set<Int>>()
        val customType = typeReference<SomeType>()
        val allTypes = setOf(stringList, intList, stringSet, intSet, customType)

        // then
        allTypes.forEach { current ->
            val otherTypes = allTypes.filterNot { it == current }
            otherTypes.forEach { assertThat(it).isNotEqualTo(current) }
        }
        assertThat(allTypes).hasSize(5)
    }

    @Test
    fun `it should fall back to the erasure when the signature does not describe the captured type`() {
        // given - R8 merges the structurally identical anonymous typeReference subclasses into
        // their superclass, after which every instance reports the same uninformative signature.
        // Erased is the JVM-expressible stand-in: its generic superclass resolves to Object rather
        // than to the type each reference was built for.
        val string = Erased(String::class.java)
        val someType = Erased(SomeType::class.java)

        // then
        assertThat(string.type).isEqualTo(String::class.java)
        assertThat(someType.type).isEqualTo(SomeType::class.java)
        assertThat(string).isNotEqualTo(someType)
        assertThat(setOf(string, someType)).hasSize(2)
    }

    @Test
    fun `it should keep a degraded reference distinct from an unrelated one`() {
        // given
        val degraded = Erased(SomeType::class.java)

        // then
        assertThat(degraded).isNotEqualTo(typeReference<List<String>>())
        assertThat(degraded).isEqualTo(typeReference<SomeType>())
    }

    @Test
    fun `it should box primitive erasures so they match the generic signature`() {
        // given
        val int1 = typeReference<Int>()
        val int2 = typeReference<Int>()

        // then
        assertThat(int1).isEqualTo(int2)
        assertThat(int1.type).isEqualTo(java.lang.Integer::class.java)
        assertThat(int1).isNotEqualTo(typeReference<Long>())
    }

    private object SomeType

    /**
     * A subclass whose generic superclass carries no usable type argument, standing in for what R8
     * leaves behind once it merges the anonymous [typeReference] subclasses together.
     */
    private class Erased(erasure: Class<*>) : TypeReferenceImpl<Any>(erasure)
}
