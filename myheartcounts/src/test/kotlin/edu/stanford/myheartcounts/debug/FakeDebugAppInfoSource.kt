//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [DebugAppInfoSource] whose current info is set through [setInfo].
 */
class FakeDebugAppInfoSource : DebugAppInfoSource {
    private val state = MutableStateFlow(
        DebugAppInfo(
            version = "1.0.0",
            build = "1",
            firebaseProjectId = null,
            accountId = null,
            enrolledStudyRevision = null,
            bundledStudyRevision = null,
            lastSignedConsentVersion = null,
        ),
    )

    override val info: Flow<DebugAppInfo> = state

    /**
     * Sets the info [info] emits from now on.
     */
    fun setInfo(info: DebugAppInfo) {
        state.value = info
    }
}
