//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import com.google.common.truth.Truth.assertThat
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.junit.Test

class PromptedActionRejectionsTest {

    private val storage = InMemoryKeyValueStorage()

    @Test
    fun `rejects nothing at first`() {
        // when
        val rejected = rejections().rejected.value

        // then
        assertThat(rejected).isEmpty()
    }

    @Test
    fun `remembers a rejection across instances`() {
        // given
        rejections().reject(type = PromptedActionType.VerifyAccountEmail)

        // when
        val rejected = rejections().rejected.value

        // then
        assertThat(rejected).containsExactly(PromptedActionType.VerifyAccountEmail)
    }

    @Test
    fun `stores the rejected actions by their identifiers`() {
        // when
        rejections().apply {
            reject(type = PromptedActionType.CompleteDemographics)
            reject(type = PromptedActionType.VerifyAccountEmail)
        }

        // then
        assertThat(storage.getString(key = "rejected_prompted_actions")).isEqualTo("demographics,email-verification")
    }

    @Test
    fun `forgets every rejection when cleared`() {
        // given
        val rejections = rejections().apply { reject(type = PromptedActionType.CompleteDemographics) }

        // when
        rejections.clear()

        // then
        assertThat(rejections.rejected.value).isEmpty()
        assertThat(rejections().rejected.value).isEmpty()
    }

    private fun rejections() = PromptedActionRejectionsImpl(storage = storage)
}
