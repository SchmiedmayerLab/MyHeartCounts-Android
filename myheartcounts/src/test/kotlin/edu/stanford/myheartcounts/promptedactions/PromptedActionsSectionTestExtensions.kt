//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import org.grovealliance.ui.GroveSection
import org.grovealliance.ui.scheduler.PromptedActionRow

internal fun GroveSection.actionRows(): List<PromptedActionRow> = elements
    .filterIsInstance<GroveSection.Element.Card>()
    .flatMap { it.rows }
    .map { it.content as PromptedActionRow }
