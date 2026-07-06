package edu.stanford.myheartcounts.account

import edu.stanford.spezi.account.AccountKeys
import edu.stanford.spezi.account.InMemoryAccountService
import edu.stanford.spezi.account.InMemoryAccountStorageProvider
import edu.stanford.spezi.account.accountConfiguration
import edu.stanford.spezi.core.ConfigurationBuilder
import edu.stanford.spezi.core.SpeziDsl

/**
 * Registers account management: the account service, storage, the keys required and collected at
 * sign-up, and every app-managed manual key.
 */
@SpeziDsl
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
