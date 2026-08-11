//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.validation

data class CapturedValidationState(
    internal val engine: ValidationEngine,
    private val input: String,
) {
    // Overrides

    override fun hashCode() =
        super.hashCode()

    override fun equals(other: Any?) =
        (other as? CapturedValidationState)?.let {
            it.engine === engine && it.input == input
        } == true

    // Methods

    fun runValidation() {
        engine.runValidation(input)
    }

    internal fun requestFocus() {
        engine.requestFocus()
    }
}
