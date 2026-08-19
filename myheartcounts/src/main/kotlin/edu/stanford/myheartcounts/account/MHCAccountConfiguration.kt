//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

import org.grovealliance.account.AccountKeys
import org.grovealliance.account.InMemoryAccountService
import org.grovealliance.account.InMemoryAccountStorageProvider
import org.grovealliance.account.accountConfiguration
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl

/**
 * Registers account management: the account service, storage, the keys required and collected at
 * sign-up, and every app-managed manual key.
 */
@GroveDsl
fun ConfigurationBuilder.account() {
    accountConfiguration(
        service = InMemoryAccountService(),
        storageProvider = InMemoryAccountStorageProvider(),
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
        },
    )
}
