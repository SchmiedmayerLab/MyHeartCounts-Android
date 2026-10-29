//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveAppBarBuilderScope
import org.grovealliance.ui.groveAppBar

class FakeStudyAppBarProvider : StudyAppBarProvider {
    override fun create(
        showsParticipationStats: Boolean,
        scope: GroveAppBarBuilderScope.() -> Unit,
    ): GroveAppBar = groveAppBar { scope() }
}
