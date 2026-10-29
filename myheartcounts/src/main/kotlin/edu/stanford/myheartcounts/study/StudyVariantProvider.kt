//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import edu.stanford.myheartcounts.model.StudyVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.grovealliance.storage.local.KeyValueStorageFactory
import org.grovealliance.storage.local.KeyValueStorageType
import java.util.Locale

/**
 * The [StudyVariant] the participant takes part in, remembered across launches.
 *
 * Until a variant is selected, [StudyVariant.Stanford] applies.
 */
interface StudyVariantProvider {

    /**
     * The current variant, updated as it changes.
     */
    val variant: StateFlow<StudyVariant>

    /**
     * The current variant.
     */
    val currentVariant: StudyVariant

    /**
     * The locale study content is resolved in: the device's language, where the study offers
     * content in it and English otherwise, combined with the current variant's region.
     */
    fun contentLocale(): Locale

    /**
     * Makes [variant] the current variant.
     */
    fun select(variant: StudyVariant)

    /**
     * Forgets the selected variant, so [StudyVariant.Stanford] applies again.
     */
    fun clear()
}

/**
 * Default [StudyVariantProvider] implementation.
 *
 * @param storageFactory Creates the storage the selected variant persists in.
 */
class StudyVariantProviderImpl(
    storageFactory: KeyValueStorageFactory,
) : StudyVariantProvider {

    private val storage = storageFactory.create(fileName = STORAGE_FILE, type = KeyValueStorageType.UNENCRYPTED)
    private val current = MutableStateFlow(
        value = storage.getString(key = KEY_VARIANT)
            ?.let { name -> StudyVariant.entries.firstOrNull { it.name == name } }
            ?: DEFAULT_VARIANT,
    )

    override val variant: StateFlow<StudyVariant> = current.asStateFlow()

    override val currentVariant: StudyVariant get() = current.value

    override fun contentLocale(): Locale {
        val language = Locale.getDefault().language.takeIf { it in CONTENT_LANGUAGES } ?: FALLBACK_LANGUAGE
        return Locale.Builder()
            .setLanguage(language)
            .setRegion(currentVariant.regionCode)
            .build()
    }

    override fun select(variant: StudyVariant) {
        storage.putString(key = KEY_VARIANT, value = variant.name)
        current.value = variant
    }

    override fun clear() {
        storage.delete(key = KEY_VARIANT)
        current.value = DEFAULT_VARIANT
    }

    private companion object {
        const val STORAGE_FILE = "edu.stanford.myheartcounts.STUDY_VARIANT"
        const val KEY_VARIANT = "variant"
        const val FALLBACK_LANGUAGE = "en"
        val CONTENT_LANGUAGES = setOf("en", "es")
        val DEFAULT_VARIANT = StudyVariant.Stanford
    }
}
