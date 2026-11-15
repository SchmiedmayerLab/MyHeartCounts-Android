//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveSection

/**
 * The single content a dashboard section shows.
 */
internal val GroveSection.content: ComposableContent
    get() = (elements.single() as GroveSection.Element.Content).content
