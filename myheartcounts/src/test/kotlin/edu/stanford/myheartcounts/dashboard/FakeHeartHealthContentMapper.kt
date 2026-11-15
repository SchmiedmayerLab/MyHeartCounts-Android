//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.groveSectionList

class FakeHeartHealthContentMapper : HeartHealthContentMapper {
    private val receivedInputs = mutableListOf<HeartHealthContentInput>()
    val inputs: List<HeartHealthContentInput> get() = receivedInputs.toList()

    override fun map(input: HeartHealthContentInput): GroveSectionList {
        receivedInputs += input
        return groveSectionList {}
    }
}
