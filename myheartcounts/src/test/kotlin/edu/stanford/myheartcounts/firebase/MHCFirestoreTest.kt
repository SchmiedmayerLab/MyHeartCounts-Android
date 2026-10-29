//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.FakeAccount
import org.grovealliance.firebase.FakeFirebaseAppConfiguration
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MHCFirestoreTest {
    private val account = FakeAccount()
    private val configuration = FakeFirebaseAppConfiguration()
    private val firestore = MHCFirestore(
        account = account,
        firebaseAppConfiguration = configuration,
    )

    @Test
    fun `account access waits internally for initialization`() = runTest {
        // given
        account.supplyUserDetails(details = HeartHealthScoreFixtures.account(id = "participant"))
        val id = async { firestore.accountId() }
        runCurrent()
        assertThat(id.isCompleted).isFalse()

        // when
        configuration.setConfigured()
        runCurrent()

        // then
        assertThat(id.await()).isEqualTo("participant")
    }

    @Test
    fun `all document accessors wait before touching Firebase and can be cancelled`() = runTest {
        // given
        val accessors: List<suspend () -> Any> = listOf(
            { firestore.usersCollection() },
            { firestore.userDocument() },
            { firestore.questionnaireResponses() },
            { firestore.notificationTracking() },
            { firestore.notificationHistory() },
            { firestore.healthObservations(sampleTypeIdentifier = "Steps") },
            { firestore.healthStatsMonths(metricId = "steps") },
        )

        // when / then
        accessors.forEach { accessor ->
            val reference = async { accessor() }
            runCurrent()
            assertThat(reference.isCompleted).isFalse()
            reference.cancelAndJoin()
        }
    }
}
