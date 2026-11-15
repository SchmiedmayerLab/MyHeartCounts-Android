//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import edu.stanford.myheartcounts.ui.ToggleRow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveSection

internal fun DebugSheet.section(key: String) = sections.value.sections.single { it.key == key }

internal suspend fun DebugSheet.loadedSection(key: String) = sections
    .map { list -> list.sections.find { it.key == key } }
    .filterNotNull()
    .first()

internal fun GroveSection.cardRows(key: String) =
    (elements.single { it.key == key } as GroveSection.Element.Card).rows

internal fun GroveSection.infoCards() = elements.filterIsInstance<GroveSection.Element.Card>().map { card ->
    card.rows.map { it.content as DebugInfoRow }
}

internal fun GroveSection.toggleRows() = elements.filterIsInstance<GroveSection.Element.Card>().flatMap { card ->
    card.rows.map { it.content as ToggleRow }
}

internal fun GroveSection.restartButton() =
    (elements.single { it.key == "restart" } as GroveSection.Element.Content).content as AsyncTextButton

internal fun GroveSection.textRow(key: String) = cardRows(key = key).single().content as DebugTextRow
