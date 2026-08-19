//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.validation

import org.grovealliance.ui.StringResource
import java.util.UUID

data class FailedValidationResult(
    val id: UUID,
    val message: StringResource,
) {
    // Constructors

    constructor(rule: ValidationRule) : this(
        id = rule.id,
        message = rule.message
    )

    // Overrides

    override fun equals(other: Any?) =
        (other as? FailedValidationResult)?.id == id

    override fun hashCode() =
        id.hashCode()
}
