//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FirebaseSchemaTest {

    @Test
    fun `collection names match the backend`() {
        // then
        assertThat(FirebaseSchema.Collection.USERS).isEqualTo("users")
        assertThat(FirebaseSchema.Collection.QUESTIONNAIRE_RESPONSES).isEqualTo("questionnaireResponses")
        assertThat(FirebaseSchema.Collection.NOTIFICATION_TRACKING).isEqualTo("notificationTracking")
        assertThat(FirebaseSchema.Collection.NOTIFICATION_HISTORY).isEqualTo("notificationHistory")
        assertThat(FirebaseSchema.Collection.STATS).isEqualTo("stats")
        assertThat(FirebaseSchema.Collection.STATS_MONTHS).isEqualTo("months")
        assertThat(FirebaseSchema.Collection.HEALTH_OBSERVATIONS_PREFIX).isEqualTo("HealthObservations_")
    }

    @Test
    fun `user names match the backend`() {
        // then
        assertThat(FirebaseSchema.User.HAS_WITHDRAWN_FROM_STUDY).isEqualTo("hasWithdrawnFromStudy")
        assertThat(FirebaseSchema.User.DATE_OF_ENROLLMENT).isEqualTo("dateOfEnrollment")
        assertThat(FirebaseSchema.User.LAST_SIGNED_CONSENT_VERSION).isEqualTo("lastSignedConsentVersion")
        assertThat(FirebaseSchema.User.LAST_SIGNED_CONSENT_DATE).isEqualTo("lastSignedConsentDate")
        assertThat(FirebaseSchema.User.DID_OPT_IN_TO_TRIAL).isEqualTo("didOptInToTrial")
        assertThat(FirebaseSchema.User.LAST_ACTIVE_DATE).isEqualTo("lastActiveDate")
        assertThat(FirebaseSchema.User.PREFERRED_WORKOUT_TYPES).isEqualTo("preferredWorkoutTypes")
        assertThat(FirebaseSchema.User.PREFERRED_NOTIFICATION_TIME).isEqualTo("preferredNotificationTime")
        assertThat(FirebaseSchema.User.FCM_TOKEN).isEqualTo("fcmToken")
        assertThat(FirebaseSchema.User.ENABLE_APP_DEBUG_MODE).isEqualTo("enableAppDebugMode")
        assertThat(FirebaseSchema.User.TIME_ZONE).isEqualTo("timeZone")
        assertThat(FirebaseSchema.User.LANGUAGE).isEqualTo("language")
        assertThat(FirebaseSchema.User.PREFERRED_MEASUREMENT_SYSTEM).isEqualTo("preferredMeasurementSystem")
        assertThat(FirebaseSchema.User.EXTENDED_ACTIVITY_NUDGES_OPT_IN).isEqualTo("extendedActivityNudgesOptIn")
        assertThat(FirebaseSchema.User.MOST_RECENT_ONBOARDING_STEP).isEqualTo("mostRecentOnboardingStep")
        assertThat(FirebaseSchema.User.US_ZIP_CODE_PREFIX).isEqualTo("usZipCodePrefix")
        assertThat(FirebaseSchema.User.UK_POSTCODE).isEqualTo("ukPostcode")
        assertThat(FirebaseSchema.User.HEIGHT_IN_CM).isEqualTo("heightInCM")
        assertThat(FirebaseSchema.User.WEIGHT_IN_KG).isEqualTo("weightInKG")
        assertThat(FirebaseSchema.User.FUTURE_STUDIES).isEqualTo("futureStudies")
        assertThat(FirebaseSchema.User.MHC_GENDER_IDENTITY).isEqualTo("mhcGenderIdentity")
        assertThat(FirebaseSchema.User.US_REGION).isEqualTo("usRegion")
        assertThat(FirebaseSchema.User.HOUSEHOLD_INCOME_US).isEqualTo("householdIncomeUS")
        assertThat(FirebaseSchema.User.HOUSEHOLD_INCOME_UK).isEqualTo("householdIncomeUK")
        assertThat(FirebaseSchema.User.EDUCATION_US).isEqualTo("educationUS")
        assertThat(FirebaseSchema.User.EDUCATION_UK).isEqualTo("educationUK")
        assertThat(FirebaseSchema.User.LATINO_STATUS).isEqualTo("latinoStatus")
        assertThat(FirebaseSchema.User.BIOLOGICAL_SEX_AT_BIRTH).isEqualTo("biologicalSexAtBirth")
        assertThat(FirebaseSchema.User.BLOOD_TYPE).isEqualTo("bloodType")
        assertThat(FirebaseSchema.User.STAGE_OF_CHANGE).isEqualTo("stageOfChange")
        assertThat(FirebaseSchema.User.UK_REGION).isEqualTo("ukRegion")
        assertThat(FirebaseSchema.User.RACE_ETHNICITY).isEqualTo("raceEthnicity")
        assertThat(FirebaseSchema.User.COMORBIDITIES).isEqualTo("comorbidities")
        assertThat(FirebaseSchema.User.NHS_NUMBER).isEqualTo("nhsNumber")
        assertThat(FirebaseSchema.User.REFERRAL_SOURCE).isEqualTo("referralSource")
    }

    @Test
    fun `measurement system values match the backend`() {
        // then
        assertThat(FirebaseSchema.MeasurementSystem.US).isEqualTo("ussystem")
        assertThat(FirebaseSchema.MeasurementSystem.UK).isEqualTo("uksystem")
        assertThat(FirebaseSchema.MeasurementSystem.METRIC).isEqualTo("metric")
    }

    @Test
    fun `notification tracking names match the backend`() {
        // then
        assertThat(FirebaseSchema.NotificationTracking.TIMESTAMP).isEqualTo("timestamp")
        assertThat(FirebaseSchema.NotificationTracking.TIME_ZONE).isEqualTo("timeZone")
        assertThat(FirebaseSchema.NotificationTracking.EVENT).isEqualTo("event")
        assertThat(FirebaseSchema.NotificationTracking.NOTIFICATION_ID).isEqualTo("notificationId")
        assertThat(FirebaseSchema.NotificationTracking.ADDITIONAL_STUFF).isEqualTo("additionalStuff")
        assertThat(FirebaseSchema.NotificationTracking.EVENT_OPENED).isEqualTo("opened")
    }

    @Test
    fun `notification history names match the backend`() {
        // then
        assertThat(FirebaseSchema.NotificationHistory.TITLE).isEqualTo("title")
        assertThat(FirebaseSchema.NotificationHistory.BODY).isEqualTo("body")
        assertThat(FirebaseSchema.NotificationHistory.ORIGINAL_TIMESTAMP).isEqualTo("originalTimestamp")
    }

    @Test
    fun `stats document names match the backend`() {
        // then
        assertThat(FirebaseSchema.StatsDocument.VERSION).isEqualTo("version")
        assertThat(FirebaseSchema.StatsDocument.METRIC).isEqualTo("metric")
        assertThat(FirebaseSchema.StatsDocument.HOURLY).isEqualTo("hourly")
        assertThat(FirebaseSchema.StatsDocument.DAILY).isEqualTo("daily")
        assertThat(FirebaseSchema.StatsDocument.SESSIONS).isEqualTo("sessions")
        assertThat(FirebaseSchema.StatsDocument.SAMPLES).isEqualTo("samples")
    }

    @Test
    fun `stats entry names match the backend`() {
        // then
        assertThat(FirebaseSchema.StatsEntry.START).isEqualTo("start")
        assertThat(FirebaseSchema.StatsEntry.END).isEqualTo("end")
        assertThat(FirebaseSchema.StatsEntry.UNIT).isEqualTo("unit")
        assertThat(FirebaseSchema.StatsEntry.SUM).isEqualTo("sum")
        assertThat(FirebaseSchema.StatsEntry.MIN).isEqualTo("min")
        assertThat(FirebaseSchema.StatsEntry.MAX).isEqualTo("max")
        assertThat(FirebaseSchema.StatsEntry.AVG).isEqualTo("avg")
        assertThat(FirebaseSchema.StatsEntry.AVERAGE).isEqualTo("average")
        assertThat(FirebaseSchema.StatsEntry.NUMERATOR).isEqualTo("numerator")
        assertThat(FirebaseSchema.StatsEntry.DENOMINATOR).isEqualTo("denominator")
        assertThat(FirebaseSchema.StatsEntry.WEIGHTING).isEqualTo("weighting")
        assertThat(FirebaseSchema.StatsEntry.DATE).isEqualTo("date")
        assertThat(FirebaseSchema.StatsEntry.VALUE).isEqualTo("value")
        assertThat(FirebaseSchema.StatsEntry.SYSTOLIC).isEqualTo("systolic")
        assertThat(FirebaseSchema.StatsEntry.DIASTOLIC).isEqualTo("diastolic")
        assertThat(FirebaseSchema.StatsEntry.ID).isEqualTo("id")
        assertThat(FirebaseSchema.StatsEntry.END_DATE).isEqualTo("endDate")
        assertThat(FirebaseSchema.StatsEntry.DURATION).isEqualTo("duration")
        assertThat(FirebaseSchema.StatsEntry.ACTIVITY_TYPE).isEqualTo("activityType")
        assertThat(FirebaseSchema.StatsEntry.PROVENANCE).isEqualTo("provenance")
        assertThat(FirebaseSchema.StatsEntry.OBSERVATION_ID).isEqualTo("observationID")
    }

    @Test
    fun `stats metric identifiers match the backend`() {
        // then
        assertThat(FirebaseSchema.StatsMetric.STEPS).isEqualTo("steps")
        assertThat(FirebaseSchema.StatsMetric.EXERCISE_TIME).isEqualTo("exercise-time")
        assertThat(FirebaseSchema.StatsMetric.ACTIVE_ENERGY).isEqualTo("active-energy")
        assertThat(FirebaseSchema.StatsMetric.WALKING_RUNNING_DISTANCE).isEqualTo("walking-running-distance")
        assertThat(FirebaseSchema.StatsMetric.FLIGHTS_CLIMBED).isEqualTo("flights-climbed")
        assertThat(FirebaseSchema.StatsMetric.HEART_RATE).isEqualTo("heart-rate")
        assertThat(FirebaseSchema.StatsMetric.RESTING_HEART_RATE).isEqualTo("resting-heart-rate")
        assertThat(FirebaseSchema.StatsMetric.WEIGHT).isEqualTo("weight")
        assertThat(FirebaseSchema.StatsMetric.HEIGHT).isEqualTo("height")
        assertThat(FirebaseSchema.StatsMetric.BMI).isEqualTo("bmi")
        assertThat(FirebaseSchema.StatsMetric.SLEEP).isEqualTo("sleep")
        assertThat(FirebaseSchema.StatsMetric.BLOOD_PRESSURE).isEqualTo("blood-pressure")
        assertThat(FirebaseSchema.StatsMetric.WORKOUTS).isEqualTo("workouts")
        assertThat(FirebaseSchema.StatsMetric.ELECTROCARDIOGRAMS).isEqualTo("electrocardiograms")
    }

    @Test
    fun `stats source identifiers match the backend`() {
        // then
        assertThat(FirebaseSchema.StatsSource.HEALTH_KIT).isEqualTo("com.apple.HealthKit")
        assertThat(FirebaseSchema.StatsSource.HEALTH_CONNECT).isEqualTo("androidx.health.connect")
        assertThat(FirebaseSchema.StatsSource.MANUAL_ENTRY).isEqualTo("edu.stanford.myheartcounts.manual")
    }

    @Test
    fun `storage names match the backend`() {
        // then
        assertThat(FirebaseSchema.Storage.USERS).isEqualTo("users")
        assertThat(FirebaseSchema.Storage.CONSENT).isEqualTo("consent")
        assertThat(FirebaseSchema.Storage.LIVE_HEALTH_SAMPLES).isEqualTo("liveHealthSamples")
        assertThat(FirebaseSchema.Storage.HISTORICAL_HEALTH_SAMPLES).isEqualTo("historicalHealthSamples")
        assertThat(FirebaseSchema.Storage.HEALTH_DELETIONS).isEqualTo("healthDeletions")
    }

    @Test
    fun `consent metadata names match the backend`() {
        // then
        assertThat(FirebaseSchema.ConsentMetadata.CONSENT_FORM_METADATA).isEqualTo("consentFormMetadata")
        assertThat(FirebaseSchema.ConsentMetadata.RESPONSES).isEqualTo("responses")
        assertThat(FirebaseSchema.ConsentMetadata.DATE).isEqualTo("date")
        assertThat(FirebaseSchema.ConsentMetadata.VERSION).isEqualTo("version")
    }

    @Test
    fun `function names match the backend`() {
        // then
        assertThat(FirebaseSchema.Function.JOIN_WAITLIST).isEqualTo("joinWaitlist")
        assertThat(FirebaseSchema.Function.MARK_ACCOUNT_FOR_STUDY_WITHDRAWAL).isEqualTo("markAccountForStudyWithdrawal")
        assertThat(FirebaseSchema.Function.MARK_ACCOUNT_FOR_STUDY_REENROLLMENT).isEqualTo("markAccountForStudyReenrollment")
        assertThat(FirebaseSchema.Function.PARAMETER_REGION).isEqualTo("region")
        assertThat(FirebaseSchema.Function.PARAMETER_EMAIL).isEqualTo("email")
    }
}
