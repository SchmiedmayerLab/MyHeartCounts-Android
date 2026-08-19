//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler

import java.time.Instant

/**
 * A single occurrence of a [Schedule].
 *
 * Think of it as one calendar entry of a potentially repeating schedule, spanning [start] until [end].
 */
data class Occurrence(
    val start: Instant,
    val end: Instant,
    val schedule: Schedule,
) : Comparable<Occurrence> {
    override fun compareTo(other: Occurrence): Int = start.compareTo(other.start)
}
