//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import org.grovealliance.ui.StringResource

class FakeHeartScoreDetailContentMapper : HeartScoreDetailContentMapper {
    val inputs = mutableListOf<HeartScoreDetailContentInput>()
    override fun map(input: HeartScoreDetailContentInput): HeartScoreDetailContentLayout {
        inputs += input
        return HeartScoreDetailContentLayout(
            title = StringResource("Details"),
            sections = emptyList(),
        )
    }
}
