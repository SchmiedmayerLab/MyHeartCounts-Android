//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.model.StudyVariant
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.grovealliance.storage.local.KeyValueStorage
import org.grovealliance.storage.local.KeyValueStorageFactory
import org.grovealliance.storage.local.KeyValueStorageType
import org.junit.After
import org.junit.Test
import java.util.Locale

class StudyVariantProviderTest {

    private val defaultLocale: Locale = Locale.getDefault()
    private val storage = InMemoryKeyValueStorage()
    private val storageFactory = object : KeyValueStorageFactory {
        override fun create(
            fileName: String,
            type: KeyValueStorageType,
        ): KeyValueStorage = storage
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `applies the Stanford variant until one is selected`() {
        // when
        val variant = provider().currentVariant

        // then
        assertThat(variant).isEqualTo(StudyVariant.Stanford)
    }

    @Test
    fun `remembers the selected variant across instances`() {
        // given
        provider().select(variant = StudyVariant.Imperial)

        // when
        val variant = provider().currentVariant

        // then
        assertThat(variant).isEqualTo(StudyVariant.Imperial)
    }

    @Test
    fun `falls back to the Stanford variant once cleared`() {
        // given
        val provider = provider()
        provider.select(variant = StudyVariant.Imperial)

        // when
        provider.clear()

        // then
        assertThat(provider.currentVariant).isEqualTo(StudyVariant.Stanford)
        assertThat(provider().currentVariant).isEqualTo(StudyVariant.Stanford)
    }

    @Test
    fun `resolves content in the device language and the variant's region`() {
        // given
        Locale.setDefault(Locale.forLanguageTag("es-US"))
        val provider = provider()
        provider.select(variant = StudyVariant.Imperial)

        // when
        val locale = provider.contentLocale()

        // then
        assertThat(locale).isEqualTo(Locale.forLanguageTag("es-GB"))
    }

    @Test
    fun `resolves content in English when the study offers none in the device language`() {
        // given
        Locale.setDefault(Locale.GERMANY)

        // when
        val locale = provider().contentLocale()

        // then
        assertThat(locale).isEqualTo(Locale.US)
    }

    private fun provider() = StudyVariantProviderImpl(storageFactory = storageFactory)
}
