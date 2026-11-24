//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import edu.stanford.myheartcounts.ui.DynamicSection
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.GroveSection
import org.grovealliance.ui.account.CustomAccountContent

internal fun CustomAccountContent.sectionKeys(): List<String> = currentSections().map { it.key }

internal fun CustomAccountContent.section(key: String): GroveSection = currentSections().single { it.key == key }

private fun CustomAccountContent.currentSections() = sections.map { content ->
    when (content) {
        is GroveSection -> content
        is DynamicSection -> content.section.value
        else -> error("Unexpected section content $content.")
    }
}

internal fun CustomAccountContent.dynamicSection(key: String): StateFlow<GroveSection> = sections
    .filterIsInstance<DynamicSection>()
    .single { it.section.value.key == key }
    .section

internal inline fun <reified T> GroveSection.rowOrNull(key: String): T? = elements
    .filterIsInstance<GroveSection.Element.Card>()
    .flatMap { it.rows }
    .singleOrNull { it.key == key }
    ?.content as T?

internal inline fun <reified T> GroveSection.row(key: String): T = requireNotNull(rowOrNull<T>(key = key))

internal inline fun <reified T> GroveSection.content(key: String): T =
    (elements.single { it.key == key } as GroveSection.Element.Content).content as T
