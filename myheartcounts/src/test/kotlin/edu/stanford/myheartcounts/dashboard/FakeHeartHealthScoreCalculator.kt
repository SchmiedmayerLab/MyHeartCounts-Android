//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

class FakeHeartHealthScoreCalculator : HeartHealthScoreCalculator {
    private val receivedInputs = mutableListOf<HeartHealthScoreInput>()
    val inputs: List<HeartHealthScoreInput> get() = receivedInputs.toList()

    override fun calculate(input: HeartHealthScoreInput): HeartHealthScores {
        receivedInputs += input
        return HeartHealthFixtures.scores(
            overall = null,
            scored = emptySet(),
            score = 0.0,
            measuredAt = HeartHealthScoreFixtures.NOW,
        )
    }
}
