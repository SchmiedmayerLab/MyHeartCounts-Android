//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [PostTrialNudges] whose stored choice is set directly and which records every write.
 */
class FakePostTrialNudges : PostTrialNudges {
    private val stored = MutableStateFlow(false)
    private var result: Result<Unit> = Result.success(Unit)
    private val _writes = mutableListOf<Boolean>()

    /**
     * The values passed to [setOptedIn], in call order.
     */
    val writes: List<Boolean> get() = _writes

    override val isOptedIn: Flow<Boolean> = stored

    /**
     * Sets the stored choice that [isOptedIn] emits.
     */
    fun setStored(optedIn: Boolean) {
        stored.value = optedIn
    }

    /**
     * Sets what [setOptedIn] returns.
     */
    fun setResult(result: Result<Unit>) {
        this.result = result
    }

    override suspend fun setOptedIn(optedIn: Boolean): Result<Unit> {
        _writes.add(optedIn)
        return result
    }
}
