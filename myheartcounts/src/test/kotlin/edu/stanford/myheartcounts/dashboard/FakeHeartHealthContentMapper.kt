//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

class FakeHeartHealthContentMapper : HeartHealthContentMapper {
    private val receivedInputs = mutableListOf<HeartHealthContentInput>()
    val inputs: List<HeartHealthContentInput> get() = receivedInputs.toList()

    override fun map(input: HeartHealthContentInput): HeartHealthContentLayout {
        receivedInputs += input
        return HeartHealthContentLayout(sections = emptyList())
    }
}
