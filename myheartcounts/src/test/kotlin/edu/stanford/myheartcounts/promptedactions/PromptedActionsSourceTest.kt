//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.demographics.FakeDemographicsEditor
import edu.stanford.myheartcounts.study.StudyActivationImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.FakeAccount
import org.grovealliance.account.isIncomplete
import org.grovealliance.account.isVerified
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.grovealliance.study.FakeStudyManager
import org.grovealliance.study.fixtures.StudyEnrollmentFixtures
import org.junit.Test

class PromptedActionsSourceTest {

    private val account = FakeAccount()
    private val demographicsEditor = FakeDemographicsEditor()
    private val studyManager = FakeStudyManager()
    private val studyActivation = StudyActivationImpl(
        storage = InMemoryKeyValueStorage(),
        timeProvider = FakeTimeProvider(),
    )
    private val rejections = FakePromptedActionRejections()
    private val source = PromptedActionsSourceImpl(
        account = account,
        demographicsEditor = demographicsEditor,
        studyManager = studyManager,
        studyActivation = studyActivation,
        rejections = rejections,
    )

    @Test
    fun `offers nothing before the study was activated on this device`() = runTest {
        // given
        account.supplyUserDetails(details = details())
        studyManager.setEnrollments(enrollments = listOf(StudyEnrollmentFixtures.create()))

        // when
        val states = source.actions.first().map { it.state }

        // then
        assertThat(states).containsExactly(PromptedActionState.Unavailable, PromptedActionState.Unavailable)
    }

    @Test
    fun `offers completing the demographics and verifying the email once active`() = runTest {
        // given
        activate()
        account.supplyUserDetails(details = details())

        // when
        val actions = source.actions.first()

        // then
        assertThat(actions).containsExactly(
            PromptedActionFixtures.create(type = PromptedActionType.CompleteDemographics),
            PromptedActionFixtures.create(type = PromptedActionType.VerifyAccountEmail),
        ).inOrder()
    }

    @Test
    fun `reports completed demographics and a verified email as completed`() = runTest {
        // given
        activate()
        account.supplyUserDetails(details = details(isVerified = true))
        demographicsEditor.setComplete(isComplete = true)

        // when
        val states = source.actions.first().map { it.state }

        // then
        assertThat(states).containsExactly(PromptedActionState.Completed, PromptedActionState.Completed)
    }

    @Test
    fun `offers no demographics until the account details are fully read`() = runTest {
        // given
        activate()
        account.supplyUserDetails(details = details().apply { isIncomplete = true })

        // when
        val demographics = source.actions.first().first()

        // then
        assertThat(demographics.state).isEqualTo(PromptedActionState.Unavailable)
    }

    @Test
    fun `marks rejected actions`() = runTest {
        // given
        activate()
        account.supplyUserDetails(details = details())

        // when
        rejections.reject(type = PromptedActionType.VerifyAccountEmail)

        // then
        assertThat(source.actions.first().map { it.isRejected }).containsExactly(false, true).inOrder()
    }

    @Test
    fun `follows the study being activated`() = runTest {
        // given
        studyManager.setEnrollments(enrollments = listOf(StudyEnrollmentFixtures.create()))
        account.supplyUserDetails(details = details())

        // when
        studyActivation.markActivated()

        // then
        assertThat(source.actions.first().map { it.state }).containsExactly(PromptedActionState.Pending, PromptedActionState.Pending)
    }

    private fun activate() {
        studyManager.setEnrollments(enrollments = listOf(StudyEnrollmentFixtures.create()))
        studyActivation.markActivated()
    }

    private fun details(isVerified: Boolean = false) = AccountDetails().apply {
        this[AccountKeys.accountId::class] = "account-id"
        this.isVerified = isVerified
    }
}
