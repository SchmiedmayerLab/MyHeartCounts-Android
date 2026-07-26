//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.consent.internal

/**
 * A single option within a consent select element.
 */
internal data class SelectionOption(val id: String, val title: String)

/**
 * The selection a consent select element requires for completion.
 */
internal sealed interface ExpectedSelection {
    /**
     * The option identified by [id] is the only accepted selection.
     */
    data class Option(val id: String) : ExpectedSelection

    /**
     * Any selection is accepted; [allowEmptySelection] controls whether an empty selection satisfies completion.
     */
    data class Anything(val allowEmptySelection: Boolean) : ExpectedSelection
}
