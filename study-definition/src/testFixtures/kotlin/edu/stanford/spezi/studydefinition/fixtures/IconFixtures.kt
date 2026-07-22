//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.Metadata

/**
 * Fixtures for [Metadata.Icon]. [create] returns a [Metadata.Icon.SystemSymbol].
 */
object IconFixtures {
    fun create(): Metadata.Icon = createSystemSymbol()

    fun createSystemSymbol(name: String = ""): Metadata.Icon.SystemSymbol = Metadata.Icon.SystemSymbol(name)

    fun createCustom(url: String = ""): Metadata.Icon.Custom = Metadata.Icon.Custom(url)
}
