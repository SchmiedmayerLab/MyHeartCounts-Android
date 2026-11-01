//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ShakeDetector] whose shakes are triggered through [shake], and reported only while enabled.
 */
class FakeShakeDetector : ShakeDetector {
    private val _shakes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val enabled = MutableStateFlow(true)

    override val shakes: Flow<Unit> = _shakes

    override val isEnabled: StateFlow<Boolean> = enabled.asStateFlow()

    override fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    /**
     * Whether anything currently observes [shakes].
     */
    val isObserved: Boolean get() = _shakes.subscriptionCount.value > 0

    /**
     * Reports a shake to collectors.
     */
    fun shake() {
        if (enabled.value) _shakes.tryEmit(Unit)
    }
}
