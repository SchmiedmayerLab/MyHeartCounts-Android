//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.demographics.ui.DemographicsChoiceSheet
import edu.stanford.myheartcounts.demographics.ui.DemographicsLayout
import edu.stanford.myheartcounts.demographics.ui.HealthImportCard
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import org.grovealliance.ui.GroveSection
import org.grovealliance.ui.GroveSectionList

internal fun DemographicsLayout.questionSections(): List<List<DemographicsLayout.Row>> = sections.sections
    .map { section -> section.cardContents().filterIsInstance<DemographicsLayout.Row>() }
    .filter { it.isNotEmpty() }

internal fun DemographicsLayout.healthImport() = sections.sections
    .single { it.key == "health-import" }
    .elements
    .filterIsInstance<GroveSection.Element.Content>()
    .single()
    .content as HealthImportCard

internal fun DemographicsChoiceSheet.options(): List<ChoiceRow> =
    sections.value.cardContents().filterIsInstance<ChoiceRow>()

private fun GroveSectionList.cardContents() = sections.flatMap { it.cardContents() }

private fun GroveSection.cardContents() = elements
    .filterIsInstance<GroveSection.Element.Card>()
    .flatMap { it.rows }
    .map { it.content }
