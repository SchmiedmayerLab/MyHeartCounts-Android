//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

/** UTF-16 validation shared by every byte-sensitive Grove identity algorithm. */
internal object GroveUnicode {
    /**
     * Returns [value] when it is a sequence of Unicode scalar values.
     *
     * Kotlin strings are UTF-16. A supplementary scalar therefore occupies a *paired* high and
     * low surrogate and is valid input. Only an unpaired code unit is rejected.
     */
    fun requireScalarText(value: String, field: String): String {
        var index = 0
        while (index < value.length) {
            val current = value[index]
            when {
                Character.isHighSurrogate(current) -> {
                    require(index + 1 < value.length && Character.isLowSurrogate(value[index + 1])) {
                        "$field contains an unpaired high surrogate at UTF-16 index $index."
                    }
                    index += 2
                }

                Character.isLowSurrogate(current) -> {
                    throw IllegalArgumentException(
                        "$field contains an unpaired low surrogate at UTF-16 index $index.",
                    )
                }

                else -> index += 1
            }
        }
        return value
    }
}
