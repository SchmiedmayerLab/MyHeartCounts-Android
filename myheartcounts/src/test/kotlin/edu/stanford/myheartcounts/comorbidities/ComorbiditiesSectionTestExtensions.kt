//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import org.grovealliance.ui.GroveSection
import org.grovealliance.ui.GroveSectionList

internal fun GroveSectionList.choiceRows(): List<ChoiceRow> = sections
    .flatMap { it.elements }
    .filterIsInstance<GroveSection.Element.Card>()
    .flatMap { it.rows }
    .map { it.content as ChoiceRow }
