//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.Component

/**
 * Fixture for [Component.CustomActiveTask.ActiveTask].
 */
object ActiveTaskFixtures {
    fun create(
        identifier: String = "",
        title: String = "",
        subtitle: String? = null,
    ): Component.CustomActiveTask.ActiveTask = Component.CustomActiveTask.ActiveTask(
        identifier = identifier,
        title = title,
        subtitle = subtitle,
    )
}
