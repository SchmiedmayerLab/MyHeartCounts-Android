//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import edu.stanford.spezi.foundation.JsonSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Locale

/**
 * A handle for reading a study definition bundle from disk.
 *
 * A bundle is a directory laid out as:
 * ```
 * <name>.spezistudybundle/
 *   definition.json
 *   article/       <filename>+<locale>.md
 *   questionnaire/ <filename>+<locale>.json
 *   consent/       <filename>+<locale>.md
 * ```
 * A bundle represents an immutable resource; mutating its directory results in undefined behaviour.
 */
class StudyBundle(
    val bundleDir: File,
    val studyDefinition: StudyDefinition,
) {
    /**
     * The bundle's study identifier.
     */
    val id get() = studyDefinition.id

    /**
     * Resolves [fileRef] to the on-disk file that best matches [locale], falling back to the
     * [fallback] localization when no better match is available. Returns `null` when no candidate
     * exists.
     */
    fun resolve(
        fileRef: FileReference,
        locale: Locale,
        fallback: String? = DEFAULT_LOCALE,
    ): File? {
        val dir = File(bundleDir, fileRef.category.rawValue)
        val candidates = dir.listFiles()?.filter { it.isFile } ?: return null
        val matches = candidates.mapNotNull { file ->
            parseLocalization(file.name, fileRef)?.let { localization -> file to localization }
        }
        if (matches.isEmpty()) return null

        val language = locale.language.lowercase()
        val region = locale.country.uppercase()
        val exact = "$language-$region"

        return matches.firstOrNull { it.second.equals(exact, ignoreCase = true) }?.first
            ?: matches.firstOrNull { it.second.substringBefore('-').equals(language, ignoreCase = true) }?.first
            ?: fallback?.let { fb -> matches.firstOrNull { it.second.equals(fb, ignoreCase = true) }?.first }
            ?: matches.first().first
    }

    /**
     * The localized display title for [component], or `null` when the component has no title.
     */
    fun displayTitle(component: Component, locale: Locale): String? = when (component) {
        is Component.Informational -> articleMetadata(component.fileRef, locale)?.get("title")
        is Component.Questionnaire -> questionnaireField(component.fileRef, locale, "title")
        is Component.TimedWalkingTest -> component.test.displayTitle
        is Component.CustomActiveTask -> component.activeTask.title
        is Component.HealthDataCollection -> null
    }

    /**
     * The localized display subtitle for [component], or `null` when the component has none.
     */
    fun displaySubtitle(component: Component, locale: Locale): String? = when (component) {
        is Component.Informational -> articleMetadata(component.fileRef, locale)?.get("lede")
        is Component.Questionnaire -> questionnaireField(component.fileRef, locale, "purpose")
        is Component.CustomActiveTask -> component.activeTask.subtitle
        is Component.TimedWalkingTest, is Component.HealthDataCollection -> null
    }

    /**
     * The localized consent text referenced by the study's metadata, if any.
     */
    fun consentText(locale: Locale): String? {
        val ref = studyDefinition.metadata.consentFileRef ?: return null
        return resolve(ref, locale)?.takeIf { it.exists() }?.readText()
    }

    private fun articleMetadata(fileRef: FileReference, locale: Locale): Map<String, String>? {
        val file = resolve(fileRef, locale)?.takeIf { it.exists() } ?: return null
        return parseFrontmatter(file.readText())
    }

    private fun questionnaireField(fileRef: FileReference, locale: Locale, field: String): String? {
        val file = resolve(fileRef, locale)?.takeIf { it.exists() } ?: return null
        return runCatching {
            JsonSerializer.parseToElement(file.readText()).jsonObject[field]?.jsonPrimitive?.content
        }.getOrNull()
    }

    companion object {
        /**
         * The bundle directory's file extension.
         */
        const val FILE_EXTENSION = "spezistudybundle"

        /**
         * The default localization used as a fallback during resolution.
         */
        const val DEFAULT_LOCALE = "en-US"

        private const val DEFINITION_FILENAME = "definition.json"

        /**
         * Opens the bundle at [bundleDir], decoding its [DEFINITION_FILENAME].
         *
         * @throws IllegalArgumentException when [bundleDir] is not a valid bundle directory.
         */
        fun open(bundleDir: File): StudyBundle {
            require(bundleDir.isDirectory && bundleDir.extension == FILE_EXTENSION) {
                "Not a study bundle directory: $bundleDir"
            }
            val definitionFile = File(bundleDir, DEFINITION_FILENAME)
            require(definitionFile.exists()) { "Missing $DEFINITION_FILENAME in $bundleDir" }
            val definition = StudyDefinitionJson.decode(definitionFile.readText())
            return StudyBundle(
                bundleDir = bundleDir,
                studyDefinition = definition,
            )
        }

        /**
         * Extracts the `<locale>` from a `<base>+<locale>.<ext>` filename matching [fileRef], or
         * `null` when the name does not match.
         */
        private fun parseLocalization(filename: String, fileRef: FileReference): String? {
            val expectedExtension = if (fileRef.fileExtension.isEmpty()) "" else ".${fileRef.fileExtension}"
            if (expectedExtension.isNotEmpty() && !filename.endsWith(expectedExtension)) return null
            val withoutExtension = filename.removeSuffix(expectedExtension)
            val separatorIndex = withoutExtension.indexOf('+')
            val base = withoutExtension.substringBefore('+')
            return withoutExtension.substring(separatorIndex + 1)
                .takeIf { separatorIndex > 0 && base == fileRef.filename && it.isNotEmpty() }
        }

        /**
         * Parses a leading `---`-fenced `key: value` frontmatter block into a map.
         */
        private fun parseFrontmatter(text: String): Map<String, String> {
            val lines = text.lines()
            if (lines.firstOrNull()?.trim() != "---") return emptyMap()
            val result = mutableMapOf<String, String>()
            lines.drop(1).takeWhile { it.trim() != "---" }.forEach { line ->
                val colon = line.indexOf(':')
                if (colon > 0) {
                    val key = line.substring(0, colon).trim()
                    val value = line.substring(colon + 1).trim()
                    if (key.isNotEmpty()) result[key] = value
                }
            }
            return result
        }
    }
}
