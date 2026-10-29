//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

/**
 * The names the app shares with the backend: Firestore collections and fields, statistics identifiers,
 * Cloud Storage paths and callable functions. Changing a name changes where data is stored or looked up.
 */
object FirebaseSchema {

    /**
     * Firestore collections, and the prefix of the per-type health observation collections.
     */
    object Collection {
        const val USERS = "users"
        const val QUESTIONNAIRE_RESPONSES = "questionnaireResponses"
        const val NOTIFICATION_TRACKING = "notificationTracking"
        const val NOTIFICATION_HISTORY = "notificationHistory"
        const val STATS = "stats"
        const val STATS_MONTHS = "months"
        const val HEALTH_OBSERVATIONS_PREFIX = "HealthObservations_"
        const val FEEDBACK = "feedback"
    }

    /**
     * Fields of a participant's document in [Collection.USERS].
     */
    object User {
        const val STUDY_VARIANT = "studyVariant"
        const val HAS_WITHDRAWN_FROM_STUDY = "hasWithdrawnFromStudy"
        const val DATE_OF_ENROLLMENT = "dateOfEnrollment"
        const val LAST_SIGNED_CONSENT_VERSION = "lastSignedConsentVersion"
        const val LAST_SIGNED_CONSENT_DATE = "lastSignedConsentDate"
        const val DID_OPT_IN_TO_TRIAL = "didOptInToTrial"
        const val LAST_ACTIVE_DATE = "lastActiveDate"
        const val PREFERRED_WORKOUT_TYPES = "preferredWorkoutTypes"
        const val PREFERRED_NOTIFICATION_TIME = "preferredNotificationTime"
        const val FCM_TOKEN = "fcmToken"
        const val ENABLE_APP_DEBUG_MODE = "enableAppDebugMode"
        const val TIME_ZONE = "timeZone"
        const val LANGUAGE = "language"
        const val PREFERRED_MEASUREMENT_SYSTEM = "preferredMeasurementSystem"
        const val EXTENDED_ACTIVITY_NUDGES_OPT_IN = "extendedActivityNudgesOptIn"
        const val MOST_RECENT_ONBOARDING_STEP = "mostRecentOnboardingStep"
        const val US_ZIP_CODE_PREFIX = "usZipCodePrefix"
        const val UK_POSTCODE = "ukPostcode"
        const val HEIGHT_IN_CM = "heightInCM"
        const val WEIGHT_IN_KG = "weightInKG"
        const val FUTURE_STUDIES = "futureStudies"
        const val MHC_GENDER_IDENTITY = "mhcGenderIdentity"
        const val US_REGION = "usRegion"
        const val HOUSEHOLD_INCOME_US = "householdIncomeUS"
        const val HOUSEHOLD_INCOME_UK = "householdIncomeUK"
        const val EDUCATION_US = "educationUS"
        const val EDUCATION_UK = "educationUK"
        const val LATINO_STATUS = "latinoStatus"
        const val BIOLOGICAL_SEX_AT_BIRTH = "biologicalSexAtBirth"
        const val BLOOD_TYPE = "bloodType"
        const val STAGE_OF_CHANGE = "stageOfChange"
        const val UK_REGION = "ukRegion"
        const val RACE_ETHNICITY = "raceEthnicity"
        const val COMORBIDITIES = "comorbidities"
        const val NHS_NUMBER = "nhsNumber"
        const val REFERRAL_SOURCE = "referralSource"
    }

    /**
     * Values of [User.PREFERRED_MEASUREMENT_SYSTEM].
     */
    object MeasurementSystem {
        const val US = "ussystem"
        const val UK = "uksystem"
        const val METRIC = "metric"
    }

    /**
     * Fields of a document in [Collection.NOTIFICATION_TRACKING], and the recorded event values.
     */
    object NotificationTracking {
        const val TIMESTAMP = "timestamp"
        const val TIME_ZONE = "timeZone"
        const val EVENT = "event"
        const val NOTIFICATION_ID = "notificationId"
        const val ADDITIONAL_STUFF = "additionalStuff"
        const val EVENT_OPENED = "opened"
    }

    /**
     * Fields of a document in [Collection.FEEDBACK].
     */
    object Feedback {
        const val VERSION = "version"
        const val ACCOUNT_ID = "accountId"
        const val MESSAGE = "message"
        const val DATE = "date"
        const val TIME_ZONE = "timeZone"
        const val APP_VERSION = "appVersion"
        const val APP_BUILD_NUMBER = "appBuildNumber"
        const val DEVICE_INFO = "deviceInfo"
        const val OS_VERSION = "osVersion"
        const val MODEL = "model"
        const val TYPE = "type"
    }

    /**
     * Fields of a document in [Collection.NOTIFICATION_HISTORY].
     */
    object NotificationHistory {
        const val TITLE = "title"
        const val BODY = "body"
        const val ORIGINAL_TIMESTAMP = "originalTimestamp"
    }

    /**
     * Top-level fields of a monthly statistics document in [Collection.STATS_MONTHS]. Exactly one of
     * [HOURLY], [DAILY], [SESSIONS] and [SAMPLES] holds the entries, keyed by source.
     */
    object StatsDocument {
        const val VERSION = "version"
        const val METRIC = "metric"
        const val HOURLY = "hourly"
        const val DAILY = "daily"
        const val SESSIONS = "sessions"
        const val SAMPLES = "samples"
    }

    /**
     * Fields of an entry in a monthly statistics document.
     */
    object StatsEntry {
        const val START = "start"
        const val END = "end"
        const val UNIT = "unit"
        const val SUM = "sum"
        const val MIN = "min"
        const val MAX = "max"
        const val AVG = "avg"
        const val AVERAGE = "average"
        const val NUMERATOR = "numerator"
        const val DENOMINATOR = "denominator"
        const val WEIGHTING = "weighting"
        const val DATE = "date"
        const val VALUE = "value"
        const val SYSTOLIC = "systolic"
        const val DIASTOLIC = "diastolic"
        const val ID = "id"
        const val END_DATE = "endDate"
        const val DURATION = "duration"
        const val ACTIVITY_TYPE = "activityType"
        const val PROVENANCE = "provenance"
        const val OBSERVATION_ID = "observationID"
    }

    /**
     * Identifiers of the statistics metrics, each naming its document in [Collection.STATS].
     */
    object StatsMetric {
        const val STEPS = "steps"
        const val EXERCISE_TIME = "exercise-time"
        const val ACTIVE_ENERGY = "active-energy"
        const val WALKING_RUNNING_DISTANCE = "walking-running-distance"
        const val FLIGHTS_CLIMBED = "flights-climbed"
        const val HEART_RATE = "heart-rate"
        const val RESTING_HEART_RATE = "resting-heart-rate"
        const val WEIGHT = "weight"
        const val HEIGHT = "height"
        const val BMI = "bmi"
        const val SLEEP = "sleep"
        const val BLOOD_PRESSURE = "blood-pressure"
        const val WORKOUTS = "workouts"
        const val ELECTROCARDIOGRAMS = "electrocardiograms"
    }

    /**
     * Source identifiers keying the contributions in a statistics document.
     */
    object StatsSource {
        const val HEALTH_KIT = "com.apple.HealthKit"
        const val HEALTH_CONNECT = "androidx.health.connect"
        const val MANUAL_ENTRY = "edu.stanford.myheartcounts.manual"
    }

    /**
     * Cloud Storage folders. Every upload lives under `USERS/{accountId}/`.
     */
    object Storage {
        const val USERS = "users"
        const val CONSENT = "consent"
        const val LIVE_HEALTH_SAMPLES = "liveHealthSamples"
        const val HISTORICAL_HEALTH_SAMPLES = "historicalHealthSamples"
        const val HEALTH_DELETIONS = "healthDeletions"
    }

    /**
     * Custom metadata keys of an uploaded consent document.
     */
    object ConsentMetadata {
        const val CONSENT_FORM_METADATA = "consentFormMetadata"
        const val RESPONSES = "responses"
        const val DATE = "date"
        const val VERSION = "version"
    }

    /**
     * Callable Cloud Functions and their parameters.
     */
    object Function {
        const val JOIN_WAITLIST = "joinWaitlist"
        const val MARK_ACCOUNT_FOR_STUDY_WITHDRAWAL = "markAccountForStudyWithdrawal"
        const val MARK_ACCOUNT_FOR_STUDY_REENROLLMENT = "markAccountForStudyReenrollment"
        const val PARAMETER_REGION = "region"
        const val PARAMETER_EMAIL = "email"
    }
}
