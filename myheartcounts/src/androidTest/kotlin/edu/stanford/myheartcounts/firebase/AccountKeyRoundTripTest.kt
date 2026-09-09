//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.BiologicalSexAtBirthKey
import edu.stanford.myheartcounts.account.ComorbiditiesKey
import edu.stanford.myheartcounts.account.DateOfEnrollmentKey
import edu.stanford.myheartcounts.account.HeightInCmKey
import edu.stanford.myheartcounts.account.NhsNumberKey
import edu.stanford.myheartcounts.account.PreferredNudgeNotificationTimeKey
import edu.stanford.myheartcounts.account.PreferredWorkoutTypesKey
import edu.stanford.myheartcounts.account.RaceEthnicityKey
import edu.stanford.myheartcounts.account.ReferralSourceKey
import edu.stanford.myheartcounts.account.UkPostcodePrefixKey
import edu.stanford.myheartcounts.account.biologicalSexAtBirth
import edu.stanford.myheartcounts.account.comorbidities
import edu.stanford.myheartcounts.account.dateOfEnrollment
import edu.stanford.myheartcounts.account.heightInCm
import edu.stanford.myheartcounts.account.nhsNumber
import edu.stanford.myheartcounts.account.preferredNudgeNotificationTime
import edu.stanford.myheartcounts.account.preferredWorkoutTypes
import edu.stanford.myheartcounts.account.raceEthnicity
import edu.stanford.myheartcounts.account.referralSource
import edu.stanford.myheartcounts.account.ukPostcodePrefix
import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.NHSNumber
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import edu.stanford.myheartcounts.model.workout.NotificationTime
import edu.stanford.myheartcounts.model.workout.WorkoutType
import edu.stanford.myheartcounts.model.workout.WorkoutTypes
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Verifies that the account keys with non-trivial serializers survive a trip through Firestore, and
 * that they land under the wire identifiers iOS reads.
 *
 * This is the check the plan singled out as most likely to fail silently: "login works" passes while
 * an individual key quietly fails to encode, and nothing surfaces it until the study tries to analyze
 * a column that is empty for every Android participant.
 */
@RunWith(AndroidJUnit4::class)
class AccountKeyRoundTripTest {

    private val enrolledAt: Instant = Instant.parse("2026-02-03T09:30:00Z")

    @Before
    fun signIn() = runTest {
        assumeTrue("Needs -Pmhc.firebaseEmulatorHost", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
        FirebaseTestSession.signUpFreshParticipant()
    }

    @After
    fun signOut() = runTest {
        FirebaseTestSession.signOut()
    }

    @Test
    fun writesEveryAwkwardKeyToFirestoreUnderItsSharedIdentifier() = runTest {
        writeKeys()

        val document = FirebaseTestSession.rawUserDocument()

        // The identifiers are the cross-platform contract, so they are asserted literally rather
        // than through the key objects that produced them.
        assertThat(document.get("preferredWorkoutTypes")).isNotNull()
        assertThat(document.get("preferredNotificationTime")).isNotNull()
        assertThat(document.get("dateOfEnrollment")).isNotNull()
        assertThat(document.get("biologicalSexAtBirth")).isNotNull()
        assertThat(document.get("raceEthnicity")).isNotNull()
        assertThat(document.get("comorbidities")).isNotNull()
        assertThat(document.get("nhsNumber")).isNotNull()
        assertThat(document.get("referralSource")).isNotNull()
        assertThat(document.getDouble("heightInCM")).isEqualTo(HEIGHT_CM)

        // Settled against iOS this session: the identifier is `ukPostcode`, not `ukPostcodePrefix`.
        assertThat(document.get("ukPostcode")).isEqualTo(UK_POSTCODE)
        assertThat(document.contains("ukPostcodePrefix")).isFalse()

        // An Instant has to be a native Firestore timestamp, which is what the backend's converters
        // and iOS both expect — a number here would decode as garbage on the other platform.
        assertThat(document.getTimestamp("dateOfEnrollment")?.toInstant()).isEqualTo(enrolledAt)
    }

    @Test
    fun readsEveryAwkwardKeyBackWithItsValueIntact() = runTest {
        writeKeys()

        val details = FirebaseTestSession.account.details.value
        assertThat(details).isNotNull()
        requireNotNull(details)

        assertThat(details.preferredWorkoutTypes?.elements).containsExactlyElementsIn(WORKOUTS.elements)
        assertThat(details.preferredNudgeNotificationTime).isEqualTo(NOTIFICATION_TIME)
        assertThat(details.dateOfEnrollment).isEqualTo(enrolledAt)
        assertThat(details.biologicalSexAtBirth).isEqualTo(BiologicalSex.FEMALE)
        assertThat(details.raceEthnicity).isEqualTo(RACE_ETHNICITY)
        assertThat(details.comorbidities).isEqualTo(COMORBIDITIES)
        assertThat(details.nhsNumber).isEqualTo(NHS_NUMBER)
        assertThat(details.heightInCm).isEqualTo(HEIGHT_CM)
        assertThat(details.ukPostcodePrefix).isEqualTo(UK_POSTCODE)
        assertThat(details.referralSource).isEqualTo(REFERRAL_SOURCE)
    }

    private suspend fun writeKeys() {
        val modified = AccountDetails()
        modified[PreferredWorkoutTypesKey::class] = WORKOUTS
        modified[PreferredNudgeNotificationTimeKey::class] = NOTIFICATION_TIME
        modified[DateOfEnrollmentKey::class] = enrolledAt
        modified[BiologicalSexAtBirthKey::class] = BiologicalSex.FEMALE
        modified[RaceEthnicityKey::class] = RACE_ETHNICITY
        modified[ComorbiditiesKey::class] = COMORBIDITIES
        modified[NhsNumberKey::class] = NHS_NUMBER
        modified[HeightInCmKey::class] = HEIGHT_CM
        modified[UkPostcodePrefixKey::class] = UK_POSTCODE
        modified[ReferralSourceKey::class] = REFERRAL_SOURCE

        AccountModifications(modifiedDetails = modified)
            .mapCatching { FirebaseTestSession.accountService.updateAccountDetails(it).getOrThrow() }
            .getOrThrow()
    }

    private companion object {
        val WORKOUTS = WorkoutTypes(setOf(WorkoutType.entries.first(), WorkoutType.entries.last()))
        val NOTIFICATION_TIME = NotificationTime(hour = 19, minute = 30)
        val RACE_ETHNICITY = RaceEthnicity.NONE
        val COMORBIDITIES = Comorbidities(emptyMap())
        val NHS_NUMBER = NHSNumber("9434765919")
        const val HEIGHT_CM = 172.5
        const val UK_POSTCODE = "SW1A"
        const val REFERRAL_SOURCE = "3"
    }
}
