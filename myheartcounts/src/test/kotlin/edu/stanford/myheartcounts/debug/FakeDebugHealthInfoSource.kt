//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.health.HealthConnectAvailability

/**
 * A [DebugHealthInfoSource] whose current info is set through [setInfo].
 */
class FakeDebugHealthInfoSource : DebugHealthInfoSource {
    private val state = MutableStateFlow(
        DebugHealthInfo(
            availability = HealthConnectAvailability.Available,
            isFullyAuthorized = false,
            historyAccess = null,
            backgroundAccess = null,
            read = emptyList(),
            write = emptyList(),
        ),
    )

    override val info: Flow<DebugHealthInfo> = state

    /**
     * Sets the info [info] emits from now on.
     */
    fun setInfo(info: DebugHealthInfo) {
        state.value = info
    }
}
