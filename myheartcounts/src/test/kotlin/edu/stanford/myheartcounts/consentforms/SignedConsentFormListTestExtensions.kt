//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import edu.stanford.myheartcounts.consentforms.ui.SignedConsentFormListLayout
import edu.stanford.myheartcounts.consentforms.ui.SignedConsentFormRow
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.GroveSection

@Suppress("UNCHECKED_CAST")
internal fun SignedConsentFormListLayout.rows(): List<CardRow<SignedConsentFormRow>> = section.value.elements
    .filterIsInstance<GroveSection.Element.Card>()
    .flatMap { it.rows } as List<CardRow<SignedConsentFormRow>>
