//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.FakeAccountService
import edu.stanford.myheartcounts.account.StudyVariantKey
import edu.stanford.myheartcounts.model.StudyVariant
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.FakeAccount
import org.grovealliance.account.isAnonymousUser
import org.grovealliance.study.FakeStudyManager
import org.junit.Test
import java.util.Locale

class StudyVariantSynchronizerTest {

    private val account = FakeAccount()
    private val accountService = FakeAccountService()
    private val variantProvider = FakeStudyVariantProvider()
    private val studyManager = FakeStudyManager()

    @Test
    fun `resolves study content in the content locale of the current variant`() = runTest {
        // given
        start()
        variantProvider.setContentLocale(locale = Locale.UK)

        // when
        variantProvider.select(variant = StudyVariant.Imperial)
        runCurrent()

        // then
        assertThat(studyManager.preferredLocale).isEqualTo(Locale.UK)
    }

    @Test
    fun `adopts the variant stored on the account`() = runTest {
        // given
        start()

        // when
        account.supplyUserDetails(details = details(variant = StudyVariant.Imperial))
        runCurrent()

        // then
        assertThat(variantProvider.currentVariant).isEqualTo(StudyVariant.Imperial)
        assertThat(accountService.updates).isEmpty()
    }

    @Test
    fun `restores the account's variant when another one is selected on the device`() = runTest {
        // given
        start()
        account.supplyUserDetails(details = details(variant = StudyVariant.Imperial))
        runCurrent()

        // when
        variantProvider.select(variant = StudyVariant.Stanford)
        runCurrent()

        // then
        assertThat(variantProvider.currentVariant).isEqualTo(StudyVariant.Imperial)
    }

    @Test
    fun `records the device's variant on an account without one`() = runTest {
        // given
        variantProvider.select(variant = StudyVariant.Imperial)
        start()

        // when
        account.supplyUserDetails(details = details(variant = null))
        runCurrent()

        // then
        assertThat(accountService.updates.single().modifiedDetails[StudyVariantKey::class])
            .isEqualTo(StudyVariant.Imperial)
    }

    @Test
    fun `leaves anonymous accounts untouched`() = runTest {
        // given
        start()

        // when
        account.supplyUserDetails(
            details = details(variant = StudyVariant.Imperial).apply { isAnonymousUser = true },
        )
        runCurrent()

        // then
        assertThat(variantProvider.currentVariant).isEqualTo(StudyVariant.Stanford)
        assertThat(accountService.updates).isEmpty()
    }

    private fun TestScope.start() {
        StudyVariantSynchronizer(
            account = account,
            accountService = accountService,
            variantProvider = variantProvider,
            studyManager = studyManager,
            scope = backgroundScope,
        ).configure()
        runCurrent()
    }

    private fun details(variant: StudyVariant?) = AccountDetails().apply {
        variant?.let { this[StudyVariantKey::class] = it }
    }
}
