//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

import edu.stanford.myheartcounts.firebase.MHCFirebaseEmulator
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.accountConfiguration
import org.grovealliance.account.firebase.FirebaseAccountService
import org.grovealliance.account.firebase.FirebaseAuthProvider
import org.grovealliance.account.firebase.FirebaseAuthProviders
import org.grovealliance.account.firebase.FirestoreAccountStorage
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl

/**
 * Registers account management: the account service, storage, the keys required and collected at
 * sign-up, and every app-managed manual key.
 */
@GroveDsl
fun ConfigurationBuilder.account() {
    accountConfiguration(
        // Email and password is the base `AccountService` path and needs no provider entry.
        // Anonymous sign-in exists only so participants in a region the study has not launched in
        // yet can join the waitlist, matching iOS.
        service = MHCAccountService.wrapping(
            wrapped = FirebaseAccountService(
                providers = FirebaseAuthProviders(FirebaseAuthProvider.Anonymous),
                // Null unless the build targets a local emulator suite; see MHCFirebaseEmulator.
                emulatorSettings = MHCFirebaseEmulator.authSettings(),
            ),
        ),
        storageProvider = FirestoreAccountStorage(collectionPath = USERS_COLLECTION),
        configuration = {
            requires(key = AccountKeys.accountId)
            collects(key = AccountKeys.name)
            collects(key = AccountKeys.email)
            collects(key = AccountKeys.password)
            collects(key = AccountKeys.dateOfBirth)
            supports(key = AccountKeys.genderIdentity)
            manual(key = AccountKeys.userId)

            // My Heart Counts specific keys — all app-managed (manual).
            // Study & Enrollment
            manual(key = AccountKeys.hasWithdrawnFromStudy)
            manual(key = AccountKeys.dateOfEnrollment)
            manual(key = AccountKeys.lastSignedConsentVersion)
            manual(key = AccountKeys.lastSignedConsentDate)
            manual(key = AccountKeys.didOptInToTrial)
            manual(key = AccountKeys.lastActiveDate)
            manual(key = AccountKeys.preferredWorkoutTypes)
            manual(key = AccountKeys.preferredNudgeNotificationTime)
            // App-Specific
            manual(key = AccountKeys.fcmToken)
            manual(key = AccountKeys.enableDebugMode)
            manual(key = AccountKeys.timeZone)
            manual(key = AccountKeys.language)
            manual(key = AccountKeys.preferredMeasurementSystem)
            manual(key = AccountKeys.postTrialNudgesOptIn)
            manual(key = AccountKeys.mostRecentOnboardingStep)
            // Demographics
            manual(key = AccountKeys.usZipCodePrefix)
            manual(key = AccountKeys.ukPostcodePrefix)
            manual(key = AccountKeys.heightInCm)
            manual(key = AccountKeys.weightInKg)
            manual(key = AccountKeys.futureStudies)
            manual(key = AccountKeys.mhcGenderIdentity)
            manual(key = AccountKeys.usRegion)
            manual(key = AccountKeys.householdIncomeUS)
            manual(key = AccountKeys.householdIncomeUK)
            manual(key = AccountKeys.educationUS)
            manual(key = AccountKeys.educationUK)
            manual(key = AccountKeys.latinoStatus)
            manual(key = AccountKeys.biologicalSexAtBirth)
            manual(key = AccountKeys.bloodType)
            manual(key = AccountKeys.stageOfChange)
            manual(key = AccountKeys.ukRegion)
            manual(key = AccountKeys.raceEthnicity)
            manual(key = AccountKeys.comorbidities)
            manual(key = AccountKeys.nhsNumber)
            manual(key = AccountKeys.referralSource)
        },
    )
}

/**
 * The Firestore collection holding one account document per participant, shared with the iOS app
 * and the `MyHeartCounts-Firebase` backend.
 */
const val USERS_COLLECTION = "users"
