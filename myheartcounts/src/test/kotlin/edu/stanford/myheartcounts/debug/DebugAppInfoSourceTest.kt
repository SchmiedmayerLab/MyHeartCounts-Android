//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.LastSignedConsentVersionKey
import edu.stanford.myheartcounts.standard.MHCAppRevisionFixtures
import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.FakeAccount
import org.grovealliance.firebase.FakeFirebaseAppConfiguration
import org.grovealliance.study.FakeStudyManager
import org.grovealliance.studydefinition.StudyBundle
import org.junit.Test

class DebugAppInfoSourceTest {

    private val account = FakeAccount()
    private val source = DebugAppInfoSourceImpl(
        appRevision = MHCAppRevisionFixtures.create(
            version = "1.2.0",
            build = 42,
        ),
        account = account,
        studyManager = FakeStudyManager(),
        studyBundleProvider = object : MHCStudyBundleProvider {
            override suspend fun get(): Result<StudyBundle> = Result.failure(IllegalStateException("not bundled"))
        },
        firebaseAppConfiguration = FakeFirebaseAppConfiguration(),
    )

    @Test
    fun `reports the app version and build`() = runTest {
        // when
        val info = source.info.first()

        // then
        assertThat(info.version).isEqualTo("1.2.0")
        assertThat(info.build).isEqualTo("42")
    }

    @Test
    fun `reports no account while nobody is signed in`() = runTest {
        // when
        val info = source.info.first()

        // then
        assertThat(info.accountId).isNull()
        assertThat(info.lastSignedConsentVersion).isNull()
    }

    @Test
    fun `reports the signed-in account once somebody signs in`() = runTest {
        // given
        source.info.first()

        // when
        account.supplyUserDetails(
            details = AccountDetails().apply {
                this[AccountKeys.accountId::class] = "account-id"
                this[LastSignedConsentVersionKey::class] = "0.1.0"
            },
        )

        // then
        val info = source.info.first()
        assertThat(info.accountId).isEqualTo("account-id")
        assertThat(info.lastSignedConsentVersion).isEqualTo("0.1.0")
    }

    @Test
    fun `reports no Firebase project until Firebase is set up`() = runTest {
        // when
        val info = source.info.first()

        // then
        assertThat(info.firebaseProjectId).isNull()
    }
}
