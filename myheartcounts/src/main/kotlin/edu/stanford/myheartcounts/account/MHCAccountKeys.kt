//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.model.OnboardingStep
import edu.stanford.myheartcounts.model.StudyVariant
import edu.stanford.myheartcounts.model.workout.NotificationTime
import edu.stanford.myheartcounts.model.workout.WorkoutTypes
import kotlinx.serialization.builtins.serializer
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.InitialValue
import org.grovealliance.foundation.InstantSerializer
import java.time.Instant

/**
 * Study, enrollment and app-specific account keys. All are **manual** keys (see [ManualAccountKey]).
 * Demographics keys live in `MHCDemographicsAccountKeys.kt`.
 *
 * Accessed via extension properties on [AccountKeys], e.g. `AccountKeys.hasWithdrawnFromStudy`.
 */

// Study & Enrollment

/**
 * The study variant the user takes part in.
 */
data object StudyVariantKey : ManualAccountKey<StudyVariant>(
    identifier = FirebaseSchema.User.STUDY_VARIANT,
    serializer = StudyVariant.serializer(),
    initialValue = InitialValue.empty(StudyVariant.Stanford),
    valueType = StudyVariant::class,
)

/**
 * Whether the user has withdrawn from the study.
 */
data object HasWithdrawnFromStudyKey : ManualAccountKey<Boolean>(
    identifier = FirebaseSchema.User.HAS_WITHDRAWN_FROM_STUDY,
    serializer = Boolean.serializer(),
    initialValue = InitialValue.empty(false),
    valueType = Boolean::class,
)

/**
 * The date the user first enrolled in the study.
 */
data object DateOfEnrollmentKey : ManualAccountKey<Instant>(
    identifier = FirebaseSchema.User.DATE_OF_ENROLLMENT,
    serializer = InstantSerializer,
    initialValue = InitialValue.instant,
    valueType = Instant::class,
)

/**
 * The version of the most recently signed consent document.
 */
data object LastSignedConsentVersionKey : ManualAccountKey<String>(
    identifier = FirebaseSchema.User.LAST_SIGNED_CONSENT_VERSION,
    serializer = String.serializer(),
    initialValue = InitialValue.string,
    valueType = String::class,
)

/**
 * The date the consent document was most recently signed.
 */
data object LastSignedConsentDateKey : ManualAccountKey<Instant>(
    identifier = FirebaseSchema.User.LAST_SIGNED_CONSENT_DATE,
    serializer = InstantSerializer,
    initialValue = InitialValue.instant,
    valueType = Instant::class,
)

/**
 * Whether the user opted in to the coaching trial.
 */
data object DidOptInToTrialKey : ManualAccountKey<Boolean>(
    identifier = FirebaseSchema.User.DID_OPT_IN_TO_TRIAL,
    serializer = Boolean.serializer(),
    initialValue = InitialValue.empty(false),
    valueType = Boolean::class,
)

/**
 * The last date the user was active in the app.
 */
data object LastActiveDateKey : ManualAccountKey<Instant>(
    identifier = FirebaseSchema.User.LAST_ACTIVE_DATE,
    serializer = InstantSerializer,
    initialValue = InitialValue.instant,
    valueType = Instant::class,
)

/**
 * The user's preferred workout types.
 */
data object PreferredWorkoutTypesKey : ManualAccountKey<WorkoutTypes>(
    identifier = FirebaseSchema.User.PREFERRED_WORKOUT_TYPES,
    serializer = WorkoutTypes.serializer(),
    initialValue = InitialValue.default(WorkoutTypes(emptySet())),
    valueType = WorkoutTypes::class,
)

/**
 * The user's preferred time of day for nudge notifications.
 */
data object PreferredNudgeNotificationTimeKey : ManualAccountKey<NotificationTime>(
    identifier = FirebaseSchema.User.PREFERRED_NOTIFICATION_TIME,
    serializer = NotificationTime.serializer(),
    initialValue = InitialValue.empty(NotificationTime(hour = 0, minute = 0)),
    valueType = NotificationTime::class,
)

// App-Specific

/**
 * The Firebase Cloud Messaging token for push notifications.
 */
data object FcmTokenKey : ManualAccountKey<String>(
    identifier = FirebaseSchema.User.FCM_TOKEN,
    serializer = String.serializer(),
    initialValue = InitialValue.string,
    valueType = String::class,
)

/**
 * Whether the in-app debug mode is enabled.
 */
data object EnableDebugModeKey : ManualAccountKey<Boolean>(
    identifier = FirebaseSchema.User.ENABLE_APP_DEBUG_MODE,
    serializer = Boolean.serializer(),
    initialValue = InitialValue.empty(false),
    valueType = Boolean::class,
)

/**
 * The user's current time zone identifier.
 */
data object TimeZoneKey : ManualAccountKey<String>(
    identifier = FirebaseSchema.User.TIME_ZONE,
    serializer = String.serializer(),
    initialValue = InitialValue.string,
    valueType = String::class,
)

/**
 * The user's current language.
 */
data object LanguageKey : ManualAccountKey<String>(
    identifier = FirebaseSchema.User.LANGUAGE,
    serializer = String.serializer(),
    initialValue = InitialValue.string,
    valueType = String::class,
)

/**
 * The user's preferred measurement system (e.g. metric / imperial).
 */
data object PreferredMeasurementSystemKey : ManualAccountKey<String>(
    identifier = FirebaseSchema.User.PREFERRED_MEASUREMENT_SYSTEM,
    serializer = String.serializer(),
    initialValue = InitialValue.string,
    valueType = String::class,
)

/**
 * Whether the user opted in to post-trial activity nudges.
 */
data object PostTrialNudgesOptInKey : ManualAccountKey<Boolean>(
    identifier = FirebaseSchema.User.EXTENDED_ACTIVITY_NUDGES_OPT_IN,
    serializer = Boolean.serializer(),
    initialValue = InitialValue.empty(false),
    valueType = Boolean::class,
)

/**
 * The most recent onboarding step the user reached.
 */
data object MostRecentOnboardingStepKey : ManualAccountKey<OnboardingStep>(
    identifier = FirebaseSchema.User.MOST_RECENT_ONBOARDING_STEP,
    serializer = OnboardingStep.serializer(),
    initialValue = InitialValue.default(OnboardingStep.WELCOME),
    valueType = OnboardingStep::class,
)

// AccountKeys access

val AccountKeys.studyVariant get() = StudyVariantKey
val AccountKeys.hasWithdrawnFromStudy get() = HasWithdrawnFromStudyKey
val AccountKeys.dateOfEnrollment get() = DateOfEnrollmentKey
val AccountKeys.lastSignedConsentVersion get() = LastSignedConsentVersionKey
val AccountKeys.lastSignedConsentDate get() = LastSignedConsentDateKey
val AccountKeys.didOptInToTrial get() = DidOptInToTrialKey
val AccountKeys.lastActiveDate get() = LastActiveDateKey
val AccountKeys.fcmToken get() = FcmTokenKey
val AccountKeys.enableDebugMode get() = EnableDebugModeKey
val AccountKeys.timeZone get() = TimeZoneKey
val AccountKeys.language get() = LanguageKey
val AccountKeys.preferredMeasurementSystem get() = PreferredMeasurementSystemKey
val AccountKeys.postTrialNudgesOptIn get() = PostTrialNudgesOptInKey
val AccountKeys.preferredWorkoutTypes get() = PreferredWorkoutTypesKey
val AccountKeys.preferredNudgeNotificationTime get() = PreferredNudgeNotificationTimeKey
val AccountKeys.mostRecentOnboardingStep get() = MostRecentOnboardingStepKey

// AccountDetails access

val AccountDetails.studyVariant: StudyVariant? get() = this[StudyVariantKey::class]
val AccountDetails.hasWithdrawnFromStudy: Boolean? get() = this[HasWithdrawnFromStudyKey::class]
val AccountDetails.dateOfEnrollment: Instant? get() = this[DateOfEnrollmentKey::class]
val AccountDetails.lastSignedConsentVersion: String? get() = this[LastSignedConsentVersionKey::class]
val AccountDetails.lastSignedConsentDate: Instant? get() = this[LastSignedConsentDateKey::class]
val AccountDetails.didOptInToTrial: Boolean? get() = this[DidOptInToTrialKey::class]
val AccountDetails.lastActiveDate: Instant? get() = this[LastActiveDateKey::class]
val AccountDetails.fcmToken: String? get() = this[FcmTokenKey::class]
val AccountDetails.enableDebugMode: Boolean? get() = this[EnableDebugModeKey::class]
val AccountDetails.timeZone: String? get() = this[TimeZoneKey::class]
val AccountDetails.language: String? get() = this[LanguageKey::class]
val AccountDetails.preferredMeasurementSystem: String? get() = this[PreferredMeasurementSystemKey::class]
val AccountDetails.postTrialNudgesOptIn: Boolean? get() = this[PostTrialNudgesOptInKey::class]
val AccountDetails.preferredWorkoutTypes: WorkoutTypes? get() = this[PreferredWorkoutTypesKey::class]
val AccountDetails.preferredNudgeNotificationTime: NotificationTime? get() = this[PreferredNudgeNotificationTimeKey::class]
val AccountDetails.mostRecentOnboardingStep: OnboardingStep? get() = this[MostRecentOnboardingStepKey::class]
