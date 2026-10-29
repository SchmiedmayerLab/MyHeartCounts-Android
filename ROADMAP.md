<!--

This source file is part of the My Heart Counts Android open-source project

SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)

SPDX-License-Identifier: MIT

-->

# My Heart Counts Android: Parity Roadmap

A feature inventory of the iOS app, the state of each feature on Android, and an ordered list of
tickets to close the gap.

| | |
|---|---|
| iOS baseline | `MyHeartCounts-iOS` @ `0c56672` (whole app, Watch app, `MyHeartCountsShared`, study definitions); the dashboard's data sources, participation stats and achievements re-checked against `5bd8ee3` |
| Android baseline | `MyHeartCounts-Android` @ `ec8168b` (audit); statuses updated against `bc3cf4a` ([#44](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/44): the G07 dashboard components and the Grove pin bump, on top of #42 and #40) and the Heart Health work on branch `feature/heart-health-dashboard` (not merged) |
| Grove baseline | `grove-kotlin` @ `695fad1` (audit); the app's pin is now `0d302ed` (Grove #13, the login button's loading state, on top of #10), a descendant that contains every G ticket marked done except G07, which was moved into the app (`dashboard/ui`) |
| Study definition | revision 44 (`MyHeartCounts-StudyDefinitions`) |
| Compiled | 2026-09-28 |
| Last updated | 2026-10-30 |

**Method.** Every Swift file of the iOS app target, the watchOS app and the shared package was read,
along with the study definition. It was compared against `myheartcounts/src/main` and the public API of
the Grove Kotlin modules MHC-Android builds on (section 2.0). This roadmap does **not** track
grove-swift ↔ grove-kotlin equivalence. It only lists Grove work where MHC-Android needs a capability
that Grove doesn't provide yet (the `G` tickets in section 5).

The Grove pin couldn't be read during the audit, so the audit assumed `695fad1`. The pin has since been
verified as `cc5937f`, which contains `695fad1`, and has since moved to `5f932aa`, `cb77df6` and then `0d302ed`.

The rows and tickets touched by Phase 1 work, demographics, and the G tickets were re-verified after the audit. Re-check any other row
against the code before acting on it.

## Progress

As of 2026-10-29. A ticket counts as done once it is ✅ here, including work on branches that aren't merged yet.

| | Done | Total | |
|---|---|---|---|
| App tickets (`T`) | 32 | 65 | 49% |
| Grove tickets (`G`) | 11 | 14 | 79% |
| **All tickets** | **43** | **79** | **54%** |
| Feature parity (rows of 2.1–2.13, without ➖, 💤 and the Android-only row) | 67 ✅ · 19 🟡 · 33 ❌ | 119 | 56% (64% counting 🟡 as half) |

| Phase | Done | Open |
|---|---|---|
| 1. Correctness bugs | 8 / 9 | T57 |
| 2. Data-collection foundations | 2 / 7 | T08, T09, T11, T12, T13 |
| 3. Onboarding parity | 10 / 12 | T17, T64 |
| 4. Questionnaires and tasks | 4 / 9 | T24, T27, T54, T55, T59 |
| 5. Active tasks | 0 / 4 | T29–T32 |
| 6. Account sheet and prompted actions | 2 / 9 | T33, T35, T37–T40, T58 |
| 7. Heart Health dashboard | 5 / 6 | T44 |
| 8. Consent renewal | 0 / 1 | T47 |
| 9. Localization | 0 / 1 | T48 |
| 10. Tooling and optional platform work | 1 / 7 | T49–T53, T65 |
| Grove | 11 / 14 | G03, G14, G05 (only if D4 is yes) |

Per feature group, see section 6. G03 (for the historical backfill) still blocks app work; G07's components live in the app's `dashboard/ui` package, so the dashboard UI (T43–T45, T58) needs no Grove change.

**Status legend**

| Status | Meaning |
|---|---|
| ✅ Done | Implemented with behavior equivalent to iOS |
| 🟡 Partial | Exists, but is missing behavior, is a placeholder, or diverges from iOS |
| ❌ Missing | Not implemented |
| ➖ N/A | Relies on an Apple-only platform capability. An Android analog is noted where one exists |
| 💤 iOS-inactive | Present in the iOS code but disabled or unreachable there. Do **not** port |

---

## 1. Parity bugs found during the audit

These are cases where Android already ships something, but it is wrong. They come first because they
damage study data or the participant's experience today.

| # | Problem | Evidence | Impact | Fix |
|---|---|---|---|---|
| B1 | **Consent response keys don't match the consent document.** The uploader reads `futureStudiesOptIn`, `trialOptIn` and `trialYes`, but `Consent+en-US.md` uses `future-studies`, `short-term-physical-activity-trial` and `short-term-physical-activity-trial-yes`. `ConsentUploadTest` uses the same wrong keys, so the test locks the bug in. | `standard/consent/MHCConsentUploader.kt` (companion constants); `androidTest/.../ConsentUploadTest.kt:85-86`; iOS `Onboarding/Consent+MHC.swift` | `futureStudies` and `didOptInToTrial` are **never written** for Android participants. Trial opt-in drives which demographics fields are required and whether the baseline row appears in the final enrollment step. | T01 ✅ [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40) |
| B2 | **Home completes tasks the participant never performed.** For questionnaires, the ECG and timed tests, `perform()` calls `onCompleteEvent` directly. No questionnaire is shown and no test is run. | `home/HomeContentMapper.kt` `perform()` / `actionButton()` | Scheduled surveys get marked complete with no response uploaded, so completion data in the scheduler is wrong. | T02 ✅ [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40) |
| B3 | **Launch routing is a placeholder.** After a 2-second dummy splash, the app always switches to Onboarding, so returning, enrolled participants land in onboarding on every launch. | `MainActivityViewModel.kt` (`// Dummy splash for now`) | Core flow is broken for returning users. | T03 ✅ [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40) |
| B4 | **App-revision FHIR extension diverges from iOS.** Android writes a flat string at `https://bdh.stanford.edu/fhir/defs/mhcAppRevision`. iOS (since #198) writes `https://myheartcounts.stanford.edu/fhir/core/mhcAppRevision` with `version`, `build`, `bundleIdentifier` and `osVersion` children, and adds it to *all* resources, including questionnaire responses. | `standard/health/HealthObservationProvenance.kt`, `standard/MHCQuestionnaireUploader.kt`; iOS `Utils/FHIR/MHCAppRevision.swift` | Backend and analysis code can't treat both platforms the same way. | T05 ✅ [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40) |
| B5 | **`preferredMeasurementSystem` values probably differ.** Android writes `us`, `uk` and `metric`. iOS writes `Locale.MeasurementSystem.identifier`, which is `ussystem`, `uksystem` or `metric`. *Verify against Firestore data before changing.* | `standard/MHCEnvironmentTracker.kt` `measurementSystem()`; iOS `Modules/EnvironmentTracking.swift` | The same field holds inconsistent values across platforms. | T06 ✅ [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40) |
| B6 | **Nudges aren't shown while the app is in the foreground.** `onMessageReceived` only logs, so FCM notification messages that arrive while the app is open are dropped. iOS presents them as banners in the foreground. | `notification/MHCMessagingService.kt` | Participants miss nudges. | T07 ✅ [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40) |
| B7 | **Health Connect permissions are never requested, so no health data is collected.** Grove's `health {}` module doesn't prompt on its own; the app has to call `Health.requestPermissionsIfNeeded(activity)`, which covers every configured read type plus `READ_HEALTH_DATA_HISTORY` and `READ_HEALTH_DATA_IN_BACKGROUND`. Nothing in MHC-Android calls it. The HealthAccess onboarding step is an info page with a Continue button, and Grove's own sample app is the only caller. | `onboarding/OnboardingStepLayoutMapper.kt` (`HealthAccess` → `infoStep`); Grove `health/Health.kt:84`, `sample-app/.../HealthViewModel.kt:86` | **Most severe finding.** Unless a participant grants access manually in Health Connect settings, collection runs without permissions and uploads nothing. | T10 ✅ (with G01). Participants onboarded before the fix still have no permissions (T10 follow-up) |
| B8 | **Far more Health Connect permissions are declared than MHC uses.** Grove's `health` manifest declares 92 permissions, including every `WRITE_*` and all `READ_MEDICAL_DATA_*`. They merge into the MHC manifest. | Grove `health/src/main/AndroidManifest.xml` | Play Console's Health Connect declaration requires a justification for each requested permission. Declaring unused ones (especially writes and medical data) risks rejection. | Broad declarations retained for bundle flexibility; permission pruning is not planned. |
| B9 | **Home's missed-task count was higher than on iOS.** It used Grove `queryMissedEvents`, which counts every uncompleted elapsed event in the last 2 weeks. iOS counts only the latest outstanding occurrence of a recurring task, and never one-off tasks. *Found during T26.* | `home/HomeTasksSource.kt` (before T26); iOS `Upcoming Tasks Tab/MissedEventQuery.swift` | Participants saw an inflated count, one per missed day of a daily task. | T26 ✅ [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40) |
| B10 | **Survey content doesn't render as intended in Android FHIR Data Capture.** (a) `itemControl` is coded under the ValueSet URL `http://hl7.org/fhir/ValueSet/questionnaire-item-control`; the renderer only reads the CodeSystem `http://hl7.org/fhir/questionnaire-item-control`, so pagination is lost and check-box questions appear as dropdowns. (b) Check-box items (10) lack `repeats`, which crashes the renderer on a second tick. (c) Quantity items (18) declare only `questionnaire-unit`, so no answer is ever recorded, and 8 HeartRisk items carry integer bounds on quantities, which throws on input. *Found during T23.* | `MyHeartCounts-StudyDefinitions` questionnaires; iOS ResearchKitOnFHIR treats `check-box` as multiple choice regardless of `repeats` | Surveys were unanswerable or crashed. Worked around in the app by `study/StudyQuestionnaire.prepare`. | Worked around in T23 ✅; content fix T55 |
| B11 | **Sign-up collected different account keys than iOS.** Android collected `dateOfBirth` and supported the generic `genderIdentity` key in the account UI. iOS requires only `userId` and `name`, and keeps `dateOfBirth` and `mhcGenderIdentity` as manual keys. | `account/MHCAccountConfiguration.kt`; iOS `Modules/DeferredConfigLoading.swift:249-273` | Sign-up asked Android participants for extra data, and account editing could store gender under a different key from demographics. | T56 ✅; existing remote data still needs an audit |

The iOS app replaces account deletion with *Withdraw from Study*. Grove's `AccountOverviewScreen` offers
only logout (no deletion), so there's no deletion bug. Withdraw is done (T04).

**Data written before the fixes.** Android participants who consented before T01 have neither
`futureStudies` nor `didOptInToTrial` on their account. Those who consented after T01 but before the
consent defaults were seeded may lack `futureStudies` when they left the toggle untouched. The values are
in each consent PDF's `responses` metadata (T57).

---

## 2. Feature inventory and Android status

### 2.0 What Grove Kotlin already provides

This section lists the Grove modules MHC-Android uses or will need, what each already offers, and the
capabilities MHC needs that are missing (❌, each with a `G` ticket). Modules MHC doesn't need, such as
`contact`, are left out.

| Grove module | Available today | Used by MHC-Android | Gaps MHC needs |
|---|---|---|---|
| `core`, `core-*`, `foundation` | Module/DI configuration, standard, coroutines, app lifecycle, logging, time provider, ViewModel helpers, JSON/Instant serializers | ✅ throughout | — |
| `firebase` | Firebase app configuration with late initialization; options and the initialization hook are resolved in the dependency graph | ✅ `firebase/MHCFirebaseConfiguration.kt` (`mhcFirebase()`) | — |
| `account` | `Account`, keys (manual/required/supported), `AccountDetails`/`AccountModifications`, sign-in/sign-out event flows, `AccountLoginScreen`, `AccountOverviewScreen` with a **`customSection` slot** (edit keys, name, password; logout; **no delete**), `AccountService.resetPassword` | ✅ login, overview, keys | ✅ `AccountService.sendEmailVerification()` and `refresh()` (**G08**) |
| `account-firebase` | `FirebaseAccountService` (email/password, anonymous, emulator), `FirestoreAccountStorage`, sets `isVerified` from Firebase | ✅ | ✅ Implements G08's resend and reload |
| `ui-account` | Data-entry and display composables (string, boolean, choices, instant), overview/edit layouts | 🟡 key display only | — (demographics pickers are MHC-specific) |
| `consent` | `ConsentScreen` rendering markdown consent with toggles, selects and a signature; `ConsentResponses` | ✅ onboarding consent | — (MHC has its own PDF renderer, `ConsentPdfRenderer.kt`) |
| `onboarding` | `OnboardingLayout`, title, information areas, actions, `SequentialOnboardingLayout` | ✅ all onboarding pages | — |
| `markdown` | Markdown + front-matter parsing, `MarkdownDocumentView` | ✅ articles, Heart Health explanations | ✅ Tables (**G13**) |
| `questionnaire` | `QuestionnaireComposable` built on **Android FHIR Data Capture**, with cancel behavior and a completion step; returns `QuestionnaireResponse` | ✅ survey tasks (T23) | — (MHC adapts the survey content before rendering, B10). The renderer takes the host theme's Grove palette (G11) |
| `scheduler` | Tasks and versions, events, outcomes, completion policies, `ScheduleCalculator`, `queryEvents` / `queryMissedEvents`, **local reminder notifications** (planner, boot receiver, exact alarms) | ✅ Home, Tasks tab, missed tasks, reminders (`queryMissedEvents` isn't used: its rule differs from iOS, B9) | ✅ Reminders open the app with the tapped task event (`SchedulerNotifications.openedNotification`) and take the app's icon, color and channel texts (**G06**) |
| `ui-scheduler` | `TaskTile`, `TaskSection`, `TaskListLayout`, `MissedTasksRow`, `NudgeBanner`, `PromptedActionsCard`, **`PromptedActionRow`**, `TaskActionsMenu`, `NoTasksContent` | ✅ Home, Tasks tab, missed tasks | — (covers the Tasks tab, missed tasks, prompted actions and the always-available menu) |
| `study` | `StudyManager` (enroll with date, unenroll, `informAboutStudies` revision migration, enrollments flow, lifecycle and completed-task-relative schedules, notification settings from the study definition), `ScheduledTaskAction`, task categories | ✅ | ✅ Study-declared sample types drive Health Connect collection through a runtime `Health` configuration (**G04**). ✅ `updatePreferredLocale` applies a region picked during first-run onboarding in the same session (**G09**) |
| `study-definition` | `StudyDefinition` decoding (Swift-compatible), `StudyBundle.open/unpack` (`.tar.zst`), locale-aware `resolve`, `consentText`, component titles | ✅ | — (downloading the bundle is MHC work, T08) |
| `health` | Configured read/write access, automatic background collection with change tokens and a resync callback, **`requestPermissionsIfNeeded`** (incl. history + background), per-type read/write requests, permission rationale activity, raw `query` (time range, data-origin source filter, sort, limit), anchored and continuous queries, `insert`, `oldestSampleDate` | 🟡 collection, permission request and availability (T10) | ✅ Availability / update-required handling and a suspending permission request (**G01**). ✅ Aggregate queries (`aggregate`, total or per hourly/daily/weekly/monthly/custom bucket) and `mostRecent` (**G02**). ❌ No resumable **historical bulk export** → **G03**. ❌ No **medical records (FHIR)** read API, although the permissions are declared → **G05** (only if D4 is yes). Broad declarations retained for bundle flexibility (B8) |
| `storage-local`, `storage-credential` | Key-value storage (encrypted/unencrypted), credential storage | ✅ | ✅ Encrypted values of any size (**G12**) |
| `ui`, `ui-theme`, `ui-validation` | Scaffold, app bar, cards, async buttons, toasts, error/loading layouts, date picker, choices input, validated text fields, **runtime `PermissionRequester`**, theme, validation rules | ✅ | ❌ No **chart** or **gauge** components → **G07** |
| `resources` | Shared Grove strings | ✅ | — English only by design: the app overrides Grove strings in its own locale folders (T48). **G10** ✅ made the one concatenated message translatable |

**Not required from Grove** (MHC-local on both platforms): Firebase Storage uploads and managed file
upload, FCM push handling, feedback, the timed walk test, the demographics UI, the dashboard's score
logic, and the consent PDF.

### 2.1 App shell and lifecycle

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Three root tabs: Home, Tasks (Upcoming), Heart Health | `Root View/RootView.swift` | ✅ Done | `study/StudyViewModel.kt` |
| Account button on every tab opens the account sheet | `Root View/RootViewTab.swift` | ✅ Done | `StudyAppBarProvider.kt` |
| Remembered tab selection | `rootTabSelection` preference | 🟡 Partial | `SavedStateHandle` only; not kept across process death or relaunch |
| Onboarding shown until complete; persisted `onboardingFlowComplete` | `MyHeartCounts.swift`, `OnboardingFlow.swift` | ✅ Done | T03: `onboarding/OnboardingCompletion.kt`, reset in `MHCStandard.onSignedOut`. Routing uses the flag only, as iOS does; a session lost while the app was closed still lands in Study |
| "Logging Out…" screen, then return to onboarding | `RootView.swift`, `MyHeartCountsStandard.performLogoutCleanup` | 🟡 Partial | Sign-out switches the root back to onboarding. There is no progress state (left open in T03). |
| Logout cleanup (pending uploads, staging, unenroll, clear Firestore cache next launch) | `MyHeartCountsStandard.swift` | 🟡 Partial | `MHCStandard.willSignOut()` covers push token, pending uploads, unenroll and cache. Missing: historical-export reset, rejected prompted actions, study activation date (these features don't exist yet). |
| Cleanup at launch when nobody is signed in | `handleIsLoggedOut` | ❌ Missing | |
| Per-region Firebase project (US/UK), chosen at eligibility and persisted | `Modules/DeferredConfigLoading.swift` | ✅ Done | `firebase/MHCFirebaseLoader.kt`, `MHCFirebaseRegionInitializer.kt` |
| Firebase emulator support | `FeatureFlags.useFirebaseEmulator` | ✅ Done | `firebase/MHCFirebaseEmulator.kt` |
| Firestore cache size limit and clear-after-logout | `DeferredConfigLoading.firestore`, `FirestoreCacheCleanup.swift` | ✅ Done | `firebase/MHCFirestoreSetup.kt` |
| Region-specific website and privacy-policy URLs | `MyHeartCounts+Website.swift` | ✅ Done | `model/StudyVariant` carries each variant's homepage and privacy policy (the Imperial privacy policy is still the Stanford placeholder, as on iOS); Home and the account sheet send `NavigationEvent.OpenStudyHomepage` / `OpenPrivacyPolicy`, which `MainActivity` resolves against the current variant; onboarding's in-app sheets pick the variant themselves (T63) |
| Background app refresh every 12 h to update the study definition | `Modules/AppRefresh.swift` | ❌ Missing | Depends on remote study bundle (T08) |
| Background task infrastructure | `Modules/MHCBackgroundTasks.swift` | ✅ Done | WorkManager (`HealthUploadWorker`) |
| Feature flags / launch options (disable Firebase, override config, study-bundle selector, unit overrides, prompted-action filter, force debug) | `SharedContext/FeatureFlags.swift`, `MyHeartCountsShared/Launch Options/*` | ❌ Missing | Tooling (T49/T50) |
| Localization: English, Spanish, en-GB | `Resources/Localizable.xcstrings` | 🟡 Partial | `res/values-en-rGB` overrides the 11 strings whose British spelling differs (landed with T63); Spanish is missing (T48) |
| Check for app update (App Store / TestFlight) | `Utils/CheckForUpdatesButton.swift` | ❌ Missing | Android equivalent: Play Store link / in-app updates |

### 2.2 Onboarding

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Welcome page | `Onboarding/Welcome.swift` | ✅ Done | `OnboardingStepLayoutMapper.welcome` |
| Eligibility screening on one page: age ≥ 18, country (current region first), reads the consent language, shared device account | `Onboarding/EligibilityScreening.swift`, `Screening/*` | ✅ Done | `onboarding/eligibility/*` |
| Ineligible page with study website link | `Screening/NotEligibleView.swift` | ✅ Done | |
| Region "coming soon" / "not supported": email waitlist (validation, anonymous sign-up, `joinWaitlist`, success/invalid alerts, logout on failure, localized definite article) | `Screening/RegionComingSoon.swift` | ✅ Done | Check copy parity for the "the"/Spanish-article handling |
| Study variant (Stanford / Imperial) chosen at eligibility, independent of the backend; persisted locally and as the `studyVariant` account key; sets the study locale, homepage and demographics layout; UK testing path in internal builds | `SharedContext/StudyVariant.swift`, `Modules/DeferredConfigLoading.swift`, `Screening/RegionComingSoon.swift` | ✅ Done | `model/StudyVariant`, `study/StudyVariantProvider`, `study/StudyVariantSynchronizer` (T63) |
| Load Firebase for the region, then download the study bundle | `EligibilityScreening.process` | 🟡 Partial | Firebase is loaded per region; the study bundle only comes from the app assets |
| "Unable to load study" step (network / decode errors, retry, check for update) | `Onboarding/UnableToLoadStudyDefinitionStep.swift` | ❌ Missing | Needed once the bundle is downloaded (T09) |
| Login / sign-up (email + password), "Next" when already signed in, privacy policy link | `Onboarding/AccountOnboarding.swift`, `Account/AccountSetupHeader.swift` | ✅ Done | `LoginStep` via Grove, with the header copy and a Privacy Policy footer (T22). Sign-up requires name; DOB and study gender are manual demographics keys, with no generic gender field (T56) |
| Reactivate previously withdrawn account (`markAccountForStudyReenrollment`, or logout) | `AccountOnboarding.ReactivatePreviouslyWithdrawnAccount` | ✅ Done | `OnboardingStep.ReactivateAccount` (T15); uses the Grove readiness fix from Grove #10 (pinned) |
| Skip consent flow when current consent version is already signed (minor-version compare) | `ConsentManager.shouldSign` | ✅ Done | `standard/consent/MHCConsentSigning` (T14) |
| 4 disclaimer pages with Learn More sheets | `Onboarding/ConsentDisclaimers.swift` | ✅ Done | StudyOverview / TrialComponent / DataCollection / RisksAndBenefits. Copy differs from iOS; align strings. |
| Comprehension check (3 true/false, all must be true) | `Onboarding/ComprehensionScreening.swift` | ✅ Done | `onboarding/comprehension/*` |
| Consent signing (markdown with toggles, selects, signature; name prefill) | `Onboarding/Consent.swift` | ✅ Done | Grove `ConsentStep` + `MHCConsentDocumentProvider` |
| Consent PDF export/share button in consent view | `Consent.swift` (`ConsentShareButton`) | ✅ Done | Share icon in the consent step's app bar, enabled once the form is complete (T22) |
| Consent PDF (paper size by region) uploaded with metadata; account keys updated | `MyHeartCountsStandard+Consent.swift`, `Onboarding/Consent.swift` | ✅ Done | `MHCConsentHandler.kt`. B1 fixed (T01). Untouched elements are seeded with their initial values, as iOS does, so `futureStudies` and `didOptInToTrial` are always written. Earlier data: see T57 |
| Health permissions step (baseline + all study types) | `Onboarding/HealthKitPermissions.swift` | ✅ Done | T10: Continue requests every collected read type plus history and background (`standard/health/HealthAccess.kt`), waits for the screen to close and continues whatever was granted, as iOS does. While Health Connect must be installed or updated, the step offers that, plus Skip (G01). Divergence: no write permissions (iOS also asks to write workouts, height, weight, BMI, glucose, BP; D1) |
| Health Records permissions step | `Onboarding/HealthRecordPermissions.swift` | 💤 iOS-inactive | `includeInOnboarding = false` |
| Workout preference (9 workout types, nudge time in 15-min steps), also reusable as a sheet | `Onboarding/WorkoutPreferenceSetting*.swift` | ❌ Missing | Models and account keys exist (`model/workout/*`) |
| Notification permission (skipped if already granted) | `Onboarding/NotificationPermissions.swift` | ✅ Done | `NotificationPermissionHandler.kt` |
| Demographics step (Continue gated on required fields) | `Onboarding/DemographicsStep.swift` | ✅ Done | `DemographicsStep`; completion and successful-save gate before final enrollment (T21) |
| Final enrollment page (trial row only if opted in) | `Onboarding/FinalEnrollmentStep.swift` | ✅ Done | Trial row only when the consent's trial question was answered yes (T22) |
| Enroll, reusing the account's `dateOfEnrollment` | `MyHeartCountsStandard.enroll` | ✅ Done | `study/StudyEnroller.kt` |
| Record local `studyActivationDate`; start historical export after enrollment | `MyHeartCountsStandard.enroll` | ❌ Missing | |
| Track `mostRecentOnboardingStep` on the account per step | `Onboarding/ProgressTracking.swift` | ✅ Done | `onboarding/OnboardingProgressTracker` (T16); `demographics` is recorded by T21; `workoutPreference` follows with T17 |

### 2.3 Consent lifecycle

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Consent renewal: when a newer consent (minor bump) arrives with a study bundle, show an explainer (last-signed date), then re-sign | `Modules/ConsentManager.swift`, `Onboarding/ConsentRenewal/ConsentRenewalFlow.swift` | ❌ Missing | Needs remote bundle (T08) |
| Review signed consent forms (list Storage PDFs newest first, version/title/date from metadata, open PDF, pull to refresh) | `Account/SignedConsentForms.swift` | ❌ Missing | |

### 2.4 Account and settings (account sheet)

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Account overview (name, email, password etc.) | `Account/AccountSheet.swift` | 🟡 Partial | Grove `AccountOverviewScreen` wrapped by `account/overview/MHCAccountOverviewScreen`; add MHC items as entries of `MHCAccountOverviewSections` via the mapper |
| Withdraw from Study (confirm, `markAccountForStudyWithdrawal`, logout) replacing delete | `AccountSheet.withdrawFromStudy` | ✅ Done | T04: `account/overview/WithdrawFromStudySection.kt`, `account/StudyWithdrawal.kt`. Divergence: always visible above Logout; iOS shows it only in edit mode, in place of deletion (Grove has no edit-mode hook) |
| Study participation section (study title, short explanation, "Enrolled since", website link) | `AccountSheet.studyParticipationSection` | ❌ Missing | The section card exists (`StudyParticipationSection`) but holds only the "View Participation Stats" row; the rows listed here are open (T33) |
| Participation stats: enrollment date; engagement (tasks done, current and longest streak, surveys, articles, ECGs, walk/run tests); health totals (steps, heartbeats, distance, active energy, exercise time, sleep, workouts, flights); highlights (best step day, longest workout, max and resting HR); fun facts | `Account/ParticipationStatsView.swift` (`AccountSheet.studyParticipationSection`) | ❌ Missing | Not in the original audit. Health totals and highlights come from the stats documents (T61, T62), as on iOS (`ParticipationStatsProvider`); ECG count depends on D2 (T58). UI built with sample data behind a DUMMY DATA badge, see T58 |
| Post-trial activity nudges toggle (`extendedActivityNudgesOptIn`, debounced) | `AccountSheet.PostTrialNudgesToggle` | ✅ Done | `ui/ToggleSection` in the account overview (T34); shown to every signed-in participant until the study participation section exists (T33) |
| Update comorbidities | `Account/UpdateComorbiditiesButton.swift` | ❌ Missing | Depends on the comorbidities picker |
| SensorKit enable/manage | `SensorKit/SensorKitButton.swift` | ➖ N/A | |
| Data processing indicator ("Processing Health data…") | `AccountSheet.dataProcessingRow` | ❌ Missing | |
| Change language | `AccountSheet` → Settings app | ❌ Missing | Android 13+ per-app language settings |
| Send feedback (`feedback/{uuid}` with version, account, message, date, time zone, app version/build, device info) | `Feedback/FeedbackForm.swift`, `FeedbackManager.swift` | ✅ Done | `account/feedback/MHCFeedback`, `account/overview/FeedbackSheet` (T36) |
| About row (version); 5 taps show extended info (build, Firebase project, account ID, enrolled/fetched study revision, `lastSignedConsentVersion`) | `AccountSheet.AboutRow` | ✅ Done | `account/overview/AppInfoSection` (T33). Divergence: the extended info isn't behind five taps on the version but in the debug sheet, which the testing environment opens on a shake (every build runs as testing until T65); the fetched revision is the bundled one until T08 |
| Privacy policy link | `AccountSheet` | ✅ Done | `AppInfoSection` (T33); both study variants use the same URL |
| License information (open-source acknowledgements) | `AccountSheet` (`ContributionsList`) | ❌ Missing | |
| Debug menu (only if debug mode) | `Utils/Debug Stuff/DebugForm.swift` | ❌ Missing | See 2.13 |
| Prompted-actions "view all" digest in the account sheet | `AccountSheet` → `PromptedActionsDigest(.viewAll)` | ❌ Missing | |
| Verify email sheet (status, send link, open mail app, polling refresh) | `Account/VerifyEmailSheet.swift` | ❌ Missing | |
| Debug-mode feature flag (`enableAppDebugMode` account key, launch option, last-seen preference) | `Modules/AccountFeatureFlags.swift` | 🟡 Partial | Key defined, not used |

### 2.5 Demographics

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| All demographics account keys, stored in Firestore | `Account/Demographics/DemographicsAccountKeys.swift` | ✅ Done | `account/MHCDemographicsAccountKeys.kt` (identifiers match) |
| Value models: gender, sex, race, Latino, education US/UK, income US/UK, US region, UK region/county, NHS number, stage of change, referral source, comorbidities, blood type | `Account/Demographics/*` | ✅ Done | `model/demographics/*` |
| Demographics form UI: DOB, gender (inferred from sex and vice versa), sex at birth, blood type, height and weight entry, race multi-select, Latino (US only), comorbidities, state/territory, education, income, UK region → county, NHS number (checksum + help link), stage of change A–I, referral source | `DemographicsForm.swift`, `DemographicsLayout.swift`, pickers | ✅ Done | `demographics/ui/DemographicsLayout` with its mappers, shared by onboarding and the account sheet (T18–T19) |
| Height and weight shown and entered in the participant's measurement system (ft/in, lb), stored as cm and kg | `DemographicsLayout.swift` (`BodyMeasurementRow`, `SaveQuantitySampleView`) | ❌ Missing | Entered and shown in cm and kg only (T64) |
| Region- and trial-dependent required fields; completion colouring (red required / orange optional) | `DemographicsLayout.swift` | ✅ Done | `DemographicsRules`; the US or UK form follows the study variant (T63). Deliberate difference: optional questions are labelled "Optional" in italics below their title and every answer, given or missing, uses one secondary colour, instead of iOS's red (required) / orange (optional) values |
| Debounced autosave with diffing, including key removals | `DemographicsData.swift` | ✅ Done | `DemographicsEditor`; one-second diff saves, key removals, flush when the screen goes away and on Continue, unsaved edits kept across account refreshes and dropped on an account change |
| "Read from Health App" autofill (DOB, blood type, sex, height, weight) | `DemographicsForm.ReadFromHealthKitButton` | ✅ Done | `DemographicsAutofill`; latest height and weight only; DOB, sex and blood type aren't available in Health Connect |

### 2.6 Home tab

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Daily nudge card (latest `notificationHistory` entry, today or yesterday) | `Home Tab/DailyNudge.swift` | ✅ Done | `home/FirebaseHomeSuggestionsSource.kt` |
| Prompted actions digest card and checklist sheet | `Home Tab/Prompted Actions/*` | 🟡 Partial | UI card exists (`HomeContentMapper.promptedActions`); source is always empty, tap does nothing |
| Today's tasks, including open one-off tasks since enrollment | `Upcoming Tasks Tab/MHCTodaysEventsQuery.swift` | ✅ Done | `home/HomeTasksSource.kt` |
| Event interaction rules (today vs. late ≤ 14 days, active tasks repeatable, button titles) | `TasksList.eventInteractionConfig` | ✅ Done | `task/EventTileMapper.kt`, shared by Home, Tasks and missed tasks. Performing an uncompleted event completes it, as iOS does (`TasksList.swift:519`) |
| Performing a task (questionnaire / ECG / timed test) | `Task Handling/PerformTaskAnchor.swift` | 🟡 Partial | `task/EventPerformer.kt`, `task/TaskPerformer.kt`. Questionnaires and articles work (T23). ECG and timed-test actions are disabled until T29 and T31 (B2) |
| Article presentation (completes event on display) | `News Tab/ArticleSheet.swift` | ✅ Done | `MarkdownSheet` and `ArticleHeader`: image, title, localized date and tags from front matter (T28); completion remains on display |
| Empty state "You're All Set", plus always-available tasks when nothing is scheduled | `TasksList.Impl` | 🟡 Partial | Section shown; tapping only logs (needs T27, T29, T31) |
| Missed tasks row (count, past 2 weeks) opening a list grouped by day | `HomeTab.missedEventsSection`, `MissedEventQuery.swift` | ✅ Done | T26: `task/MissedEventsSource.kt` (iOS rule, B9), full-screen `missed/MissedTasksScreen`. The list's window is fixed to the day it opened; Home's rolls at midnight |
| "About the study" link | `HomeTab.learnMoreSection` | ✅ Done | |

### 2.7 Tasks tab ("Upcoming")

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Next 2 weeks of tasks grouped by day, with action buttons | `Upcoming Tasks Tab/UpcomingTasksTab.swift`, `TasksList.swift` | ✅ Done | T25: `upcoming/*`; tab labelled "Tasks", as on iOS. Tile timing follows iOS: hours below a day, then calendar days, weeks and months, and the year once it differs ("Last week, 9:00 AM", "In 2 weeks, 9:00 AM"); no time for a task lasting all of today; start and end for a fixed duration (`task/EventTileMapper.kt`). One known difference: 4 calendar weeks ahead but less than a month reads "Next month" rather than "In 4 weeks" |
| "Other Tasks – Always Available" section (ECG, 6MWT, 12-min run) | `TasksList.fallbackSections`, `AlwaysAvailableTaskActions.swift` | 🟡 Partial | Shown, excluding activities already scheduled; tapping only logs (needs T27, T29, T31) |
| Toolbar menu "Perform always-available task" (plus a 30-second walk test in debug) | `AlwaysAvailableTaskActionsMenu.swift` | 🟡 Partial | Menu with ECG, 6MWT and 12-min run; selections only log. No debug walk test (T49) |
| Unscheduled completion reporting (completes the matching open event in the next 2 weeks) | `PerformTask.reportCompletion` | ❌ Missing | |
| Task state after reinstall | SpeziScheduler (local store) | 🟡 Verify | Completions live only in the local scheduler store, so after a reinstall completed surveys show as open again. Re-enrolling recreates tasks effective from "now", so earlier occurrences (and their missed state) disappear, although `dateOfEnrollment` is reused. Check what iOS does before treating this as a gap (T59) |
| Local task reminders at the scheduled time (e.g. 09:00) | Spezi Scheduler notifications | ✅ Done | `scheduler(notifications = DEFAULT)`. Grove schedules reminders from the study's `notifications` config (thread and time), with exact alarms and reschedule on boot |
| Tapping a notification opens the app (iOS also tracks it and logs task-bound taps) | `MyHeartCountsStandard+Notifications.swift` | 🟡 Partial | FCM nudge taps are tracked (`MainActivity.trackNotificationOpen`). Grove task reminders open the app since G06, but MHC doesn't route to the task yet, track the open, or set its own icon and channel texts (T54) |

### 2.8 Questionnaires

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Render FHIR questionnaires from the study bundle in the preferred locale (14 surveys: Diet, WHO5, DiseaseQOL, Chronotype, ActivityFitness, GAD7, ExerciseAdequacy, ExerciseMindset, SUS, HeartRisk, ParQ, Fatigue, plus Nicotine and Info without schedules) | `PerformTaskAnchor` → `QuestionnaireView` | ✅ Done | T23: full-screen `task/PerformTaskScreen` with Grove `QuestionnaireComposable`. Content is adapted before rendering (`study/StudyQuestionnaire.prepare`, B10). Resolved in the study variant's content locale, the same locale task titles and instructions use (T63). Dark-mode renderer theme not checked |
| Upload `QuestionnaireResponse` to `users/{id}/questionnaireResponses/{id}` with canonical URL and app revision | `MyHeartCountsStandard+QuestionnaireResponse.swift` | ✅ Done | `MHCQuestionnaireUploader.kt` (status `completed`, quantity answers as `valueQuantity`, app revision from T05). Divergence: the screen closes and the task completes without waiting for the server acknowledgement; iOS awaits the write |
| HeartRisk extraction (systolic/diastolic BP and glucose written to the health store) | `QuestionnaireParsing/QuestionnaireDataExtractor.swift` | ❌ Missing | Depends on decision D1 |

### 2.9 Active tasks

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Timed walking/running test (6MWT, 12-min Cooper run): motion permission, countdown, phone step counting and distance, vibration at end, restart | `Task Handling/Active Tasks/Timed Walk Test/*` | ❌ Missing | |
| Crash/kill recovery of an in-progress test (persist; resume or finalize; cancel recovered test) | `TimedWalkingTest.recover`, `TaskContinuationAnchor.swift` | ❌ Missing | |
| Live Activity with countdown and result | `TimedWalkingTest.startLiveActivity` | ➖ N/A | Android analog: foreground service with an ongoing notification |
| Apple Watch companion (workout session, countdown UI) | `MyHeartCountsWatchApp/*`, `WatchConnection.swift` | ➖ N/A | Optional analog: Wear OS app |
| Result as FHIR Observation (LOINC 6MWT, steps, distance, duration, activity), uploaded directly to Firestore | `TimedWalkingTestResult+FHIR.swift` | ❌ Missing | |
| VO₂max estimate for the running test | `PastTimedWalkTestResultInfo.swift` | ❌ Missing | |
| Past timed test results list and details, start a new test | `Heart Health Dashboard/PastTimedWalkTestResults.swift` | ❌ Missing | |
| ECG task: instructions, auto-detect a new Apple Watch ECG, manual "Mark as Complete" | `Active Tasks/ECG/ECGInstructions.swift` | ➖ N/A | Health Connect has no ECG type. Decision D2. |

### 2.10 Heart Health dashboard

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Overall CVH score gauge (0–100; mean of ≥ 5 of 8 components) with explanatory header | `Heart Health Dashboard/CVHScore.swift`, `HeartHealthDashboard.swift` | ✅ Done | T41/T44: live `HeartHealthScoreSourceImpl` feeds `GaugeCard` and its header; the mean requires five of eight eligible factors, excluding mental wellbeing |
| Score definitions and bands: diet (MEPA), exercise minutes (weekly) *or* daily steps, nicotine, sleep, BMI (standard/Asian bands), LDL, fasting glucose, BP (custom), mental wellbeing (WHO5 × 4; shown but not in the composite) | `CVHScore.swift`, `Score.swift` | ✅ Done | T41 numerical bands, selection, and composite implemented in `HeartHealthScoreCalculatorImpl`; band explainers belong to T45. WHO-5 normalization corrects the signed-distance issue in the current reference. |
| Score component grid (9 tiles, gauge, last date or "Tap to learn more") | `HeartHealthDashboard.makeGridComponent` | ✅ Done | T44 live scores (shown as whole numbers with the fraction dropped, not rounded) and exercise-or-steps selection now feed `GaugeTileGrid`, with time/"Yesterday"/date captions; detail route remains T45 |
| Component detail: most recent value, gauge, recent-values chart with time range (7d/14d/1m/3m/6m/12m), band explainer highlighting the matched band, "About X" markdown from `hhdExplainer` with links | `DetailedHealthStatsView*.swift`, `Score+ExplainerUI.swift` | ✅ Done | T45: header, history, bands, explanations and debug samples; T46 supplies Add Data |
| Charts: line/bar/point, automatic aggregation interval, x-axis stride labels, tap highlight, BP dual series, sleep bars per night | `Health Dashboard/*` | ✅ Done | T43: metric-specific history, aggregation, calendar axes, highlights, blood-pressure series and daily sleep bars wired through Grove G07. |
| Sleep sessions (merge overnight samples; iOS only counts Apple Health system sleep) | `SleepSessionsQuery.swift` | 🟡 Partial | T61 merges Health Connect sessions that overlap or lie less than 60 minutes apart, uses native sleep aggregation, assigns the midpoint month and writes stats read by T62/T41. Native 8-hour sleep → Firestore → score 100 verified locally. Data-origin filtering is configurable; D6 policy review remains open. |
| BMI derived from weight and height when no BMI sample (weight < 6 months old) | `CVHScore.bodyMassIndexScore` | ✅ Done | T41 selects measured BMI versus BMI derived from the latest stats weight and height, with the reference age rule and standard/Asian bands. T61 supplies derived BMI from Health Connect weight and height; D3 records the policy. |
| Custom samples from Firestore (diet, WHO5, nicotine, LDL) with live listener and FHIR decoding | `MHCFirestoreQuery.swift`, `QuantitySample+FHIR.swift` | ✅ Done | T42: live account-scoped listeners, FHIR decoding and time-range filtering for diet, WHO-5, nicotine and LDL. |
| Data entry: BP, fasting glucose, BMI (BMI directly or weight + height; ft/in picker), LDL (to Firestore), diet/WHO5/nicotine via questionnaire; input limits; permission-denied errors | `Data Entry/*`, `HeartHealthDashboard.addSampleSheet` | ✅ Done | T46: native BP/glucose/height/weight saves, Firestore LDL/direct BMI, questionnaires, units, input limits and save errors. |
| "Understanding your score" learn-more section | `HeartHealthDashboard.learnMoreSection` | ✅ Done | `dashboard/HeartHealthExplainerSource` reads `hhdExplainer/LearnMore` (localized) from the study bundle; `ui/MarkdownCard` in a `DashboardSection`; left out when absent |
| Debug: browse Firestore custom samples | `DetailedHealthStatsView.BrowseFirestoreSamplesView` | ✅ Done | T45: expandable stored samples gated by account debug mode. |
| Achievements / goals | `Achievements/Achievement.swift`, `AchievementsView.swift` | ❌ Missing | Live on iOS since `67ae08b` (2026-09-12), not iOS-inactive; no ticket yet. UI components built (G07, `dashboard/ui`: `ProgressRingCard`, `TextBadge`, `CounterCard`); UI built with sample data as a page of the participation stats sheet, see T58 |

### 2.11 Health data collection and upload

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Live collection of study sample types while signed in and enrolled | `MyHeartCountsStandard+HealthKit.swift` | 🟡 Partial | `MHCStandard`, `MHCHealthDataHandler.kt`; 25 Health Connect record types polled every 15 min. Permissions are requested during onboarding since T10; participants onboarded before that still have none. The types are hard-coded in MHC: Grove's study-driven collection (G04) is pinned and enabled (`StudyHealthConfiguration.DEFAULT`), so the list duplicates it until T52 removes it |
| Coverage of the study's sample types | `Study.swift` (`healthDataCollection`) | 🟡 Partial | Health Connect lacks many HealthKit types (walking metrics, AFib burden, menstrual types, ECG, state of mind, …). Review whether Health Connect types such as menstruation, nutrition, hydration, cycling cadence and power should be added. |
| Local staging DB (dedup, elide samples deleted before upload), 3-day retention, 6 h upload | `Modules/HealthUploadStaging*.swift` | ✅ Done | `HealthStagingDatabase.kt`, `HealthUploadWorker.kt` |
| Batched zstd JSON upload to `liveHealthSamples`; deletions as zstd CSV to `healthDeletions` | `HealthUploadStagingUploader.swift` | ✅ Done | |
| Managed file upload with resume of orphaned files | `Modules/ManagedFileUpload.swift` | ✅ Done | `standard/health/ManagedFileUpload.kt` |
| Direct-Firestore upload for custom samples and timed-walk results (`HealthObservations_{type}/{id}`, batches of 100) | `uploadHealthObservations(.directFirestore)` | ❌ Missing | Needed by the dashboard, data entry and the timed walk test |
| FHIR extensions: upload time zone, study enrollment (ID + revision), app revision | `MyHeartCountsStandard+HealthKit.swift`, `Utils/FHIR/MHCAppRevision.swift` | ✅ Done | `standard/MHCAppRevision.kt` (T05), on health samples and questionnaire responses. Android also adds a `source-platform` extension |
| Monthly stats documents: per-metric summaries (hourly sums and min/max/avg, sleep sessions, individual readings, workouts, ECGs) kept current from enrollment, with at least 12 months of history, at `/users/{uid}/stats/{metric}/months/YYYY-MM`, one contribution per source; read back with source selection and merging by the dashboard, participation stats and achievements | `Modules/HealthKitStatsCalculator*.swift`, `Modules/StatsStore*.swift`, `Health Dashboard/StatsDocument*.swift`, `docs/MHCDataSpec.md` (User Data Statistics) | ✅ Done | T61 writes all 13 supported metrics; T62 reads and processes monthly documents with source selection, merging and live account-scoped updates. |
| Historical backfill (10 years), resumable, to `historicalHealthSamples` | `Health Import/HistoricalHealthSamplesExportManager.swift` | ❌ Missing | `HealthUploadCategory.HISTORICAL` defined but unused; needs the Health Connect history permission |
| ECG voltage and symptoms FHIR | `Health Import/HealthObservation.swift` | ➖ N/A | |
| Clinical records upload | `ClinicalRecordPermissions.swift`, `HealthKitSamplesFHIRUploader.swift` | ➖ N/A | Optional analog: Health Connect medical records (D4) |
| Health Connect change-token expiry resync | — | ✅ Android-only | `MHCHealthDataHandler.onFullyResyncRequired` |
| SensorKit (visits, on-wrist, device usage, ECG, wrist temperature, HR, pedometer, light, accelerometer, pressure, PPG), background processing, manage sheet, "How to manage" article | `SensorKit/*` | ➖ N/A | Apple research-only framework |

### 2.12 Backend integration, notifications and environment

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Study bundle downloaded from the region's Storage bucket (`public/mhcStudyBundle.*`), cached, fallback to bundled on decode error; `informAboutStudies` on change | `Modules/StudyBundleLoader.swift`, `MyHeartCountsStandard.handleStudyBundleUpdates` | 🟡 Partial | `MHCStudyBundleProviderImpl.kt` loads the bundled asset only |
| Push token (FCM) sync to the account; removed before logout | `Modules/NotificationsManager.swift` | ✅ Done | `MHCPushTokenSynchronizer.kt`, `MHCAccountService` |
| Notification open tracking (`notificationTracking`) | `Modules/NotificationTracking.swift` | ✅ Done | `MHCNotificationTracking.kt` |
| Present notifications while the app is in the foreground | `MyHeartCountsStandard+Notifications.swift` | ✅ Done | T07: `notification/MHCNudgeNotifications.kt`, channel `nudges`, shared with background nudges. The small icon is the app icon; no monochrome notification icon exists yet |
| Environment tracking: last active, time zone, language, measurement system | `Modules/EnvironmentTracking.swift` | ✅ Done | `MHCEnvironmentTracker.kt`; values `metric` / `ussystem` / `uksystem` (T06). Still to confirm against production Firestore data (a TODO in the code) |
| Cloud functions: `joinWaitlist`, `markAccountForStudyWithdrawal`, `markAccountForStudyReenrollment` | various | 🟡 Partial | `joinWaitlist` and `markAccountForStudyWithdrawal` (T04) are used; `markAccountForStudyReenrollment` waits for T15 |

### 2.13 Developer and testing tooling

| Feature (iOS) | iOS reference | Android | Evidence / notes |
|---|---|---|---|
| Debug form: demographics, memory usage, upload notifications toggle, notification status, pending local notifications, trigger remote nudge (`notificationBacklog`), bulk-export control, staging DB stats, background task log, file upload queue, scheduler stats, file browser, reset rejected actions, answer questionnaire, check for update | `Utils/Debug Stuff/*` | ❌ Missing | |
| Debug "will/did upload" local notifications | `MyHeartCountsStandard+HealthKit.swift` | ❌ Missing | |
| UI test environment setup (reset, test-account login/sign-up, enroll, prefilled details) | `Modules/SetupTestEnvironment*.swift`, `MyHeartCountsShared/Launch Options/*` | ❌ Missing | Android has unit, screenshot and Firebase instrumented tests instead |
| Demo data and screenshot mode | `Utils/Demo/DemoSetup.swift`, `SharedContext/Screenshotting.swift`, `make_screenshots` | ❌ Missing | |
| iOS-specific workarounds (sheet re-hosting, tab-bar accessibility) | `Utils/SwiftUI/RootSheetRouting.swift`, `TabBarAccessibilityFix.swift` | ➖ N/A | |

### 2.14 iOS-inactive code (do not port)

- **News tab** (`News Tab/NewsTab.swift`, `Modules/NewsManager.swift`): not registered as a module and not in the tab bar.
- **Achievements / goals** (`Heart Health Dashboard/Achievements/Achievement.swift`).
- **Health Records onboarding step** (`HealthRecordPermissions.includeInOnboarding = false`).
- **Per-sample FHIR SensorKit upload strategy** (`SensorKitDataFetcher+Uploading+FHIR.swift`).

---

## 3. Open product decisions

These should be settled before the dependent tickets start. The tickets below mark what each one
blocks.

| # | Decision | Blocks |
|---|---|---|
| D1 | **Resolved: write entered measurements to Health Connect.** Follow the native health-store behavior: request write access for BP, glucose, height and weight and update the privacy copy. Health Connect has no BMI record type, so directly entered BMI uses the existing Firestore BMI stats format; native weight/height entries feed derived BMI. LDL remains a direct Firestore observation. | T24, T46 |
| D2 | **ECG on Android.** Health Connect has no ECG record. Options: hide the ECG task and always-available entry on Android; show instructions with manual completion only; or ask the backend to exclude it per platform. | T29, T25, T58 |
| D3 | **BMI and exercise minutes.** Health Connect has no BMI record and no "Apple exercise time". Decide on derivations (BMI from latest weight + height; exercise minutes from `ExerciseSessionRecord` durations) so Android scores are comparable to iOS. | T41, T58, T61 |
| D4 | **Clinical records** via Health Connect medical records (FHIR) — in scope or not? | T53, G05 |
| D5 | **Wear OS companion** for the timed walk test — in scope or not? | T30 |
| D6 | **Sleep data sources.** iOS scores only Apple Health system sleep by default, and its stats writer applies the same filter to the `sleep` stats documents. Pick the equivalent Health Connect data-origin policy. | T61, T41 |
| D7 | **Health Connect in the shared stats documents.** The stats documents (`/users/{uid}/stats/{metric}/months/YYYY-MM`) keep each source's contribution separately; iOS writes as `com.apple.HealthKit`. Android needs its own source identifier, agreed with the iOS and backend maintainers, and a rule for data that reaches both stores (for example a Samsung watch synced to Health Connect and, through a third-party app, to HealthKit for the same participant). Whether metrics Health Connect can't supply (Apple exercise time, ECG) get an Android-derived value follows D2 and D3. | T61 |

---

## 4. Implementation order

1. **Correctness bugs.** Stop corrupting or losing data now.
2. **Data-collection foundations.** Every day these are missing loses study data.
3. **Onboarding parity.** Covers who gets enrolled and what they agreed to.
4. **Questionnaires and tasks.** Backend-derived dashboard scores (diet, WHO5, nicotine) depend on questionnaire responses, so these come before the dashboard.
5. **Active tasks.**
6. **Account sheet and prompted actions.**
7. **Heart Health dashboard.**
8. **Consent renewal.**
9. **Localization.**
10. **Tooling and optional platform work.**

Grove tickets (`G01`–`G13`) are placed in the phase whose MHC tickets need them, ahead of those
tickets. Because a Grove change reaches the app through the submodule pin (see README), each `G`
ticket is: merge in Grove, bump the pin, then use it in MHC.

| Phase | MHC tickets | Grove tickets needed first |
|---|---|---|
| 1 | T01–T07, **T10** ✅, T57 | G01 ✅ (for T10) |
| 2 | T08, T09, T11–T13, T61, T62 | G04 ✅, G03 (for T11), G02 ✅ (for T61) |
| 3 | T14–T22, T56 ✅, T63 ✅, T64 | G09 ✅ (for T08/T16 locale correctness) |
| 4 | T23–T28 (T28 ✅), T54, T55, T59 | G06 ✅ (for T54), G11 ✅ (replaces MHC's copied survey palette) |
| 5 | T29–T32 | — |
| 6 | T33–T40, T58 | G08 ✅ (for T38) |
| 7 | T41–T46 | G07 ✅ (for T43/T44/T45), G13 ✅ (for T45) |
| 8 | T47 | — |
| 9 | T48 | G10 ✅ (Grove side) |
| 10 | T49–T53, **T60** ✅ | G05 (for T53, only if D4 is yes) |

G12 (an encrypted-storage bug) blocked no ticket; it is done ✅.

Phases set priority, not hard gates. T23, T25 and T26 (Phase 4) landed while T10 (Phase 1)
was still open, because T10 waits for G01.

---

## 5. Tickets

Every ticket has a title, a description (behavior, iOS reference, Android starting point, acceptance
criteria) and tags. The first tag is the feature group; the second is `bug`, `feature`, `tooling`,
`decision` or `grove`. `grove` marks work done in `grove-kotlin`; those tickets are listed in section
5.1.

### Phase 1: correctness bugs

#### T01: Record trial opt-in and future-studies consent from the correct consent keys ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `consent`, `onboarding`, `bug`
**Description:** `MHCConsentUploader` reads `futureStudiesOptIn` / `trialOptIn` / `trialYes`, but the consent markdown declares `future-studies` / `short-term-physical-activity-trial` / `short-term-physical-activity-trial-yes` (iOS `Onboarding/Consent+MHC.swift`). Use the document's keys. Consider exposing them from one shared constants file next to the consent document provider. Fix `ConsentUploadTest` to use real keys, and add a test that parses the shipped `Consent+en-US.md` and asserts the keys exist.
**Acceptance:** Signing consent with "Yes" for the trial writes `didOptInToTrial = true` and `futureStudies` to the account document. The test fails if the markdown keys change.

#### T02: Stop completing tasks that weren't performed ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `tasks`, `bug`
**Description:** `HomeContentMapper.perform()` completes questionnaire, ECG and timed-test events directly. Until T23, T31 and T29 land, disable (or hide) the action for categories that can't be performed. Only articles may complete on display. After those tickets, route each category to its performer and complete only on success, mirroring iOS `PerformTaskAnchor` / `TasksList.handleAction`.
**Acceptance:** No event is completed unless the participant finished the associated activity. Unit tests in `HomeContentMapperTest` cover each category.

#### T03: Real launch routing and persisted onboarding completion ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `app-shell`, `bug`
**Description:** Replace the dummy splash in `MainActivityViewModel`. Persist an "onboarding complete" flag, set when `FinalEnrollment` succeeds and cleared on sign-out. Route to `Study` when the flag is set, the participant is signed in and enrolled; otherwise route to `Onboarding`. Mirror iOS `MyHeartCounts.swift` / `RootView.swift`. Add an optional "Logging out…" state while `willSignOut` runs.
**Acceptance:** A returning, enrolled participant lands on the Home tab after a cold start. Signing out returns to Welcome. Covered by a ViewModel test.
**Open:** the "Logging out…" state isn't built. Cleanup at launch when nobody is signed in is part of T13.

#### T04: Withdraw from Study ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `account`, `feature`
**Description:** Grove's `AccountOverviewScreen` offers logout but no deletion. Add "Withdraw from Study" through its `customSection` slot: show a confirmation dialog with the iOS copy ("You can re-enroll later if you choose"), call `MHCCloudFunctions.markAccountForStudyWithdrawal()`, then log out (which already runs `willSignOut`). iOS: `AccountSheet.withdrawFromStudy`.
**Acceptance:** No path in the UI deletes the account. Withdrawing sets the backend flag and signs out.

#### T05: Align the app-revision FHIR extension with iOS ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `health-data`, `bug`
**Description:** Emit `https://myheartcounts.stanford.edu/fhir/core/mhcAppRevision` with `version`, `build` (integer), `bundleIdentifier` and `osVersion` children on *every* uploaded resource, including questionnaire responses, consent-derived resources and custom samples. iOS: `Utils/FHIR/MHCAppRevision.swift`. Check with the backend team whether the old URL needs to remain for already-uploaded data.
**Acceptance:** Android and iOS resources carry identical extension structure. Tests in `HealthObservationMapperTest` assert it.
**As built:** only the new URL is written, with no transition period; `osVersion` is `Build.VERSION.RELEASE`.

#### T06: Use the same measurement-system values as iOS ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `backend`, `bug`
**Description:** Confirm the values iOS writes for `preferredMeasurementSystem` (`Locale.MeasurementSystem.identifier`: expected `metric` / `ussystem` / `uksystem`) against production Firestore, then align `MHCEnvironmentTracker.measurementSystem()`.
**Acceptance:** The same locale produces the same stored value on both platforms.
**Open:** the values were switched without checking production Firestore; a TODO in `MHCEnvironmentTracker` tracks that check. Existing Android documents update on the next sign-in or locale change.

#### T07: Show nudges while the app is in the foreground ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `notifications`, `bug`
**Description:** In `MHCMessagingService.onMessageReceived`, post a local notification for notification payloads when the app is in the foreground, on a dedicated "Nudges" channel. Keep open tracking by passing the notification ID in the intent. iOS always presents incoming notifications (`receiveIncomingNotification` returns banner/list/sound).
**Acceptance:** A nudge sent while the app is open appears in the tray, and opening it is tracked.

#### T10: Request Health Connect permissions during onboarding and afterwards ✅ Done in [#42](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/42) (onboarding part)
**Tags:** `health-data`, `onboarding`, `bug`
**Description:** Fixes B7. On the HealthAccess step's Continue, call Grove's `Health.requestPermissionsIfNeeded(activity)`. It requests every configured read type plus `READ_HEALTH_DATA_HISTORY` and `READ_HEALTH_DATA_IN_BACKGROUND`, and Grove already ships the rationale activity. Wait for the result (`isFullyAuthorizedState`) before advancing, and allow skipping. For participants who are already enrolled, re-check on launch and prompt if permissions are missing, for example as a prompted action (T39). Use G01 to handle "Health Connect not installed / needs update" (Android 10–13, minSdk 29). Update the HealthAccess copy to name Health Connect. iOS: `HealthKitPermissions.swift`. Depends on G01.
**Acceptance:** A fresh install on Android 10, 13 and 14+ ends onboarding with Health Connect permissions granted (or explicitly skipped), and background collection delivers records.
**As built:** `standard/health/HealthAccess` (+Impl) wraps Grove `Health` and asks from the activity in the foreground (tracked through activity lifecycle callbacks on the injected `Application`), so view models need no activity. The HealthAccess step's Continue calls it, awaits the result and continues regardless; while Health Connect needs installing or updating, the step shows "Install Health Connect" (Play Store, web fallback) and Skip, and switches back to Continue when the participant returns with it installed. Copy names Health Connect and no longer mentions write access. Device-verified on Android 16 (fresh install, dev Firebase): the step requests the 25 read types plus history and background, onboarding continues after the screen closes, and all 25 collectors start. The install path is covered by unit tests and was checked in Grove's sample app on Android 12, not in MHC.
**Open:** participants who onboarded before this fix have no permissions and are never asked; iOS has no re-prompt (it asks only during onboarding), so this is Android-only. Prompting them needs the prompted-actions sheet (T39) or a one-off prompt at launch. Not tested on Android 10–13 with Health Connect installed from the store.

#### T57: Backfill consent opt-ins for early Android participants
**Tags:** `consent`, `backend`, `bug`
**Description:** Participants who signed consent on Android before T01 lack `futureStudies` and `didOptInToTrial`. Those who signed after T01, but before untouched consent elements were seeded with their initial values, may lack `futureStudies`. The answers are stored in each consent PDF's `responses` custom metadata (`users/{id}/consent/*.pdf`). Agree with the backend team whether to backfill the account fields from that metadata (a one-off script or function), or to treat the missing fields as unknown in analysis.
**Acceptance:** Every Android participant who signed consent has both fields set, or the gap is documented for analysis.

### Phase 2: data-collection foundations

#### T08: Download the study bundle from Firebase with a bundled fallback
**Tags:** `backend`, `feature`
**Description:** Port `StudyBundleLoader`. Download `public/mhcStudyBundle.<ext>` from the region's Storage bucket. Unpack it into a cache and keep only the newest copy. Fall back to the bundled asset on a decode failure. Keep the last good bundle on network errors. Expose a typed error: `unableToFetchFromServer` / `unableToDecode` / `noRegion`. Call `studyManager.informAboutStudies` whenever the bundle changes (revision migration). Add a periodic WorkManager refresh every 12 h (iOS `AppRefresh`). Grove `study-definition` already provides `StudyBundle.unpack/open`, and `StudyManager.informAboutStudies` handles revision migration. The download, caching and refresh are MHC work.
**Acceptance:** Publishing a new bundle revision is picked up without an app update. The enrolled study revision updates. Offline launches keep working.

#### T09: "Unable to load study" onboarding step
**Tags:** `onboarding`, `feature`
**Description:** After eligibility, require a successfully loaded bundle. On failure, show a network or decode error page with Retry and a "Check for app update" link to the Play Store. iOS: `UnableToLoadStudyDefinitionStep.swift`. Depends on T08.
**Acceptance:** Offline onboarding shows the error page, and Retry recovers once the connection returns.

#### T11: Historical health data backfill
**Tags:** `health-data`, `feature`
**Description:** Port `HistoricalHealthSamplesExportManager` on top of Grove's bulk export (G03). After enrollment (and on launch if it isn't finished), export every collected type from `historicalDataCollection` (10 years) in resumable time batches. Map each batch to FHIR (the same mapper as live data). Upload zstd JSON to `historicalHealthSamples` via `ManagedFileUpload` (the `HISTORICAL` category exists). Reset the session on sign-out. Depends on T10 (history permission) and G03.
**Acceptance:** A new participant with years of Health Connect data has it uploaded, and the backfill survives process death.

#### T12: Direct-Firestore upload for custom observations 🟡 Partial
**Tags:** `health-data`, `backend`, `feature`
**Description:** Add the iOS `directFirestore` strategy. Write FHIR Observations to `users/{id}/HealthObservations_{sampleTypeId}/{uuid}` in batches of 100, with the standard extensions. Use it for custom quantity samples (`MHCCustomSampleType{BloodLipidMeasurement,DietMEPAScore,WHO5Score,NicotineExposure}`) and `MHCHealthObservationTimedWalkingTestResultIdentifier`. Include the FHIR encode/decode of `QuantitySample` (LOINC 18262-6 for LDL; Spezi coding system for the others). iOS: `MyHeartCountsStandard+HealthKit.swift`, `QuantitySample+FHIR.swift`.
**Acceptance:** Round-trip tests. Documents are readable by the iOS dashboard query code.
**As built:** `CustomSampleUploader` writes all four custom quantity types in batches of 100, with UUID document identities, LOINC 18262-6 for LDL, Spezi coding for custom types, dates and standard provenance extensions. T46 uses it for LDL. Timed-walk result upload remains open with T31.

#### T13: Study activation date and logout cleanup parity
**Tags:** `app-shell`, `feature`
**Description:** Store a local `studyActivationDate` on enrollment and clear it on sign-out. Also clear rejected prompted actions and reset the historical-export session on sign-out. Run cleanup at launch when no user is signed in. iOS: `MyHeartCountsStandard.enroll` / `performLogoutCleanup`.
**Acceptance:** Signing out and in as a different participant leaves no state from the previous one.

#### T61: Write monthly stats documents from Health Connect ✅ Done (validated against local emulators)
**Tags:** `health-data`, `backend`, `feature`
**Description:** Produce the shared monthly stats documents at `/users/{uid}/stats/{metric}/months/YYYY-MM` from Health Connect, following the format in iOS `docs/MHCDataSpec.md` (User Data Statistics): `version` 0, `metric`, and exactly one of `hourly` / `sessions` / `samples` mapping a source identifier to its entries. Metrics and stored units as on iOS: `steps`, `exercise-time`, `active-energy`, `walking-running-distance`, `flights-climbed` (hourly sums); `heart-rate` (hourly min/max/avg); `sleep` (per session, hours asleep, month of the session's midpoint); `blood-pressure` (sys/dia pairs); `weight`, `height`, `bmi`, `resting-heart-rate` (individual readings); `workouts`; `electrocardiograms` (per D2). Buckets use local calendar time with ISO 8601 timestamps carrying the offset. Keep the months from the enrollment month, and at least the last 12 months, up to date: refresh on app foreground and periodically in the background (iOS: every 12 h), incrementally with change tokens. A refresh replaces only Android's own source array for that metric and month; other sources' arrays stay untouched; an empty read without deletion evidence never clears data, and clearing a missing month never creates an empty document. Hourly sums and min/max/avg come from G02's `Health.aggregate`; average weights are written only when exact (see `StatsAggregation.md`). Sleep follows D6, exercise time D3, and the source identifier D7. Health Connect history beyond 30 days needs the history permission (T10). iOS: `Modules/HealthKitStatsCalculator*.swift`. Depends on G02, T10 and D7.
**Acceptance:** For a participant with Health Connect data, the documents match the spec, are decoded by the iOS reader (`StatsDocumentCodingTests` fixtures), and update after new data arrives; other sources' contributions are preserved.
**As implemented (branch `feature/heart-health-dashboard`):** Android has collectors for the 13 supported metrics, spec-shaped encoding, source-scoped Firestore writes, account-scoped checkpoints, incremental change-token processing and deletion tracking. History covers enrollment and at least 12 months, subject to Health Connect permissions; without history access, months starting at most 30 days before this installation first saw read access are synced, matching Health Connect's rule for reads before the first grant. Refresh is connected to enrollment/sign-in, foreground entry, record changes and a 12-hour WorkManager job; sign-out cancels sync and clears checkpoints. Dependencies, including the sync coroutine scope and scheduler context, are injected. Firestore APIs wait for Firebase readiness internally. Supporting Grove changes (paginated record/change reads, read-access reporting, shared test fixtures and cancellation-preserving `runCatchingAsync`) are merged in Grove #10 and pinned.
**Current policies:** configurable source ID `androidx.health.connect`; exercise time from exercise-session duration; BMI derived from weight and height; sleep from all readable origins using native aggregation. No ECG producer because Health Connect has no ECG record type. These are the current local choices for D3/D6/D7; cross-platform agreement on the source identifier and overlap policy remains open.
**Validation:** All 298 app unit tests, app lint, screenshot verification and instrumentation APK assembly pass, along with 36 Grove health tests and 11 concurrency tests. Three on-device Firestore writer tests pass: dotted source IDs preserve other contributions, repeated replacement is idempotent, and clearing a missing month creates no document. A separate temporary fixture app inserted 11 native Health Connect record types; the study app produced all 13 supported stats metrics through its real collection/sync/writer pipeline. Verified sums and units, heart-rate min/max/avg, sleep sessions, BP pairs, BMI derivation and workout identity/type/duration. Updates changed steps to 2,400, weight to 81 kg and BMI to 20.25 without duplicates; confirmed deletions cleared fixture contributions while preserving pre-existing steps and an independent source. Clean cold restart preserved the resulting state. The WorkManager stats worker completed successfully. Heart Health displayed exercise and sleep scores from these native records. The actual unmodified iOS `StatsDocument` decoder and shared unit codec, compiled for an iOS simulator, accepted all 13 Android-written documents with zero malformed entries. Temporary fixture records and the fixture app were removed.
**Integration:** `MyHeartCounts-Firebase` upstream `main` at `3f206df` already grants participant-scoped access under `/users/{userId}/stats/{document=**}`; ten local access checks pass, including denied access for other users and unauthenticated callers. No backend edits or deployment were needed. Remote Firebase deployment status has not been verified. D3/D6/D7 defaults remain configurable and subject to cross-platform policy review; ECG production remains unavailable on Health Connect.

#### T62: Read stats documents ✅ Fully implemented
**Tags:** `health-data`, `backend`, `feature`
**Description:** The read side of the stats documents for the dashboard, charts, participation stats and achievements: one-shot and live (snapshot listener) queries per metric over a time range, selecting the month documents by ID; decoding that tolerates malformed entries; source policies `automatic` / `only` / `preferred` with the iOS merge rules (cumulative sums prefer one source per overlapping bucket and fill gaps from others, heart-rate min/max merge, averages pool only with compatible weights, overlapping sleep sessions prefer one source); re-aggregation into day or custom intervals with diagnostics such as an unaligned interval; restart on account change. iOS: `Modules/StatsStore*.swift`, `Health Dashboard/StatsDocument*.swift`, `StatsDocumentsQuery.swift`, `docs/StatsQueries.md`, `docs/StatsAggregation.md`.
**Acceptance:** Unit tests on the iOS document fixtures reproduce iOS query results, including source selection; a document written on another device shows up live.
**As built (branch `feature/heart-health-dashboard`, not merged):** package `healthstats/`, each part an interface with an `…Impl` registered in `healthStatsConfigurations()`; failures are returned as `Result`s, never thrown. `HealthStatsEntryDecoder` / `HealthStatsDocumentDecoder` read the stored maps (entry kind from its fields, mixed shapes rejected, unknown fields ignored, fractional seconds kept, legacy `provenance.observationID` event identity; a bad entry is counted, not fatal). `HealthStatsSourceSelector` does validation, unit conversion and per-bucket source selection with the four policies (default order: `com.apple.HealthKit`, then lexical; heart-rate min/max/weighted-average merge only). `HealthStatsIntervalReducer` groups into calendar intervals in the `TimeProvider`'s zone with the three alignment policies. `HealthStatsProcessor` maps requests (`HealthStatsRequest.Quantity` / `SleepSessions` / `BloodPressure` / `Workouts` / `Electrocardiograms`) to results with diagnostics. `HealthStatsMonths` picks the month documents (one month either side, capped at the month after now). `HealthStatsStore` offers `fetch(request, readPolicy)` and a live `updates(request)` flow that follows the signed-in account (restarts on change, silent while signed out) and reuses the last result when only cache/pending-write metadata changed; `MHCFirestore.healthStatsMonths(metricId)`. Divergences: units come from a fixed table (count, count/min, count/s, ms/s/min/hr/d, mm/cm/m/km/in/ft/yd/mi, g/kg/lb/st, kcal/Cal/kJ, mmHg) rather than a full unit system, so an entry in any other unit counts as malformed; workout activity types are kept as their numeric code without checking it is a known type; the live flow has no explicit "not signed in" error and no suspension during an explicit logout. T41 consumes these streams directly.

### Phase 3: onboarding parity

#### T14: Skip consent when the current version is already signed ✅ Done
**Tags:** `consent`, `onboarding`, `feature`
**Description:** After login, compare the bundle's consent `version` against `lastSignedConsentVersion` (a minor-or-greater bump requires signing). Skip the disclaimers, comprehension and consent when not needed. If the version can't be determined, show the consent flow. iOS: `ConsentManager.shouldSign`, `AccountOnboarding.advance`.
**Acceptance:** A re-installing participant with a current consent goes straight to the health permissions step.
**As built:** `standard/consent/MHCConsentSigning` (+Impl) reads the signed-in account's `lastSignedConsentVersion` and the bundled consent document's version (`MHCConsentDocumentProvider`, the participant's locale) and requires signing when the participant never signed, when the document is newer by a minor or major version, or when either version is unknown; a patch-only difference does not. `OnboardingStepProvider` asks it after the account steps (Login, StudyOverview for someone already signed in, and ReactivateAccount), once the account's details have loaded, and goes straight to HealthAccess when signing isn't required; the reactivation check still comes first. Going back from HealthAccess then returns to StudyOverview instead of the consent steps that were skipped. As on iOS, a participant who skips consent has no consent responses in this onboarding, so the final step doesn't show the trial row. Covered by `MHCConsentSigningTest` (against the bundled consent, version 0.1.0; the patch-only case isn't exercised because the bundled version is fixed) and `OnboardingStepProviderTest`. Checked on the emulator against the Firebase emulator suite after a reinstall: a participant whose account holds `lastSignedConsentVersion` 0.1.0 goes from Login straight to HealthAccess, while one who never signed, and one who signed 0.0.9, get the disclaimers.

#### T15: Reactivate a previously withdrawn account ✅ Done
**Tags:** `onboarding`, `account`, `feature`
**Description:** If `hasWithdrawnFromStudy == true` after login, show the "Reactivate Account" page. *Reactivate* calls `markAccountForStudyReenrollment` and continues. *Don't reactivate* logs out and returns to login. iOS: `ReactivatePreviouslyWithdrawnAccount`.
**Acceptance:** A withdrawn participant can only continue after an explicit reactivation.
**As built:** A new `OnboardingStep.ReactivateAccount` follows Login (and StudyOverview for a participant already signed in) when the account's `hasWithdrawnFromStudy` is `true`. `OnboardingStepProvider.getNext` is now suspending: leaving those steps waits, up to 10 s, until the signed-in account's details are no longer `isIncomplete`, since the login screen moves on before the stored fields arrive. The page has the iOS copy, an account icon and no back button in the app bar. *Reactivate Account* calls `markAccountForStudyReenrollment` and continues with the disclaimers; if the call fails, an error toast is shown and the participant stays on the page. *Don't reactivate* signs out and returns to Login; a failed sign-out shows an error toast. System back goes to StudyOverview, whose Continue comes back to the page while the participant is still withdrawn. The step records no `mostRecentOnboardingStep`, as on iOS. Needs three Grove fixes, merged in Grove #10 and pinned: `FirestoreAccountStorage` marks details `isIncomplete` until the account's stored document has been received in this session (an empty cache, or one holding only values written since, e.g. by the environment tracker right after sign-in, no longer passes for loaded); `FirebaseAccountServiceImpl` merges external details below the Firebase-owned values and sets its flags one by one, which also stops every Firestore update from resetting `isVerified` and `isAnonymousUser` to `false`; and `AccountDetailsFlagsKey` is `internal` rather than `private`, so JVM unit tests can build details with flags. Covered by `OnboardingStepProviderTest` (branching, waiting for loaded details, the timeout, back navigation), layout-mapper tests and a screenshot. Checked on the emulator against the Firebase emulator suite with a seeded withdrawn participant: the page appears after login, reactivating fails with the toast while the Functions emulator isn't running, and declining returns to Login. A successful reactivation hasn't been run live.

#### T16: Track the most recent onboarding step ✅ Done
**Tags:** `onboarding`, `feature`
**Description:** Write `mostRecentOnboardingStep` (iOS raw values: `welcome`, `eligibility`, `login`, `disclaimer1-4`, `comprehension`, `consent`, `healthAccess`, `workoutPreference`, `notifications`, `demographics`, `finalStep`) to the account on entering each step while signed in. Map the Android steps to these IDs. iOS: `ProgressTracking.swift`.
**Acceptance:** Firestore shows the same step IDs for both platforms.
**As built:** `onboarding/OnboardingProgressTracker` (+Impl) maps the onboarding steps to the iOS identifiers: StudyOverview, TrialComponent, DataCollection and RisksAndBenefits are `disclaimer1`–`disclaimer4` (the same four pages in the same order); Ineligible and CountryUnavailable record nothing, as they belong to the eligibility step on iOS. `OnboardingViewModel` hands it its current step; it writes a step once that step is current while somebody is signed in, which also covers the step shown right after login (the account's details arrive only after the login screen moves on, so checking at the moment the step changed would miss it). A failed write is logged, as on iOS. Differences from iOS: StudyOverview comes before Login for someone not yet signed in, so `disclaimer1` is only recorded for participants who are already signed in; and `login` is recorded only if the account's details arrive before the login screen moves on, which usually they don't (iOS records it when the step appears to someone already signed in, which Android skips). `demographics` is recorded with T21; `workoutPreference` follows with T17. Covered by `OnboardingProgressTrackerTest`; checked on the emulator against the Firebase emulator suite (`healthAccess` after login, `finalStep` on the final step).

#### T17: Workout preference step and editor
**Tags:** `onboarding`, `account`, `feature`
**Description:** Add the WorkoutPreference step after health access: 9 workout types (multi-select, iOS IDs), and a nudge notification time picker in 15-minute steps, default 09:00. Save to `preferredWorkoutTypes` / `preferredNotificationTime`. Make the screen reusable from settings as a sheet. iOS: `WorkoutPreferenceSetting*.swift`.
**Acceptance:** Values round-trip in the iOS string formats (`walk,run`, `09:00`).

#### T18: Demographics form, core fields and persistence ✅ Done
**Tags:** `demographics`, `feature`
**Description:** Build the form with the region- and trial-dependent layout: DOB, gender identity and sex at birth (each inferred from the other), blood type, height and weight, race/ethnicity multi-select ("prefer not to state" is exclusive), Latino (US only), education, income, referral source. Required vs. optional follows iOS `DemographicsLayout.swift`: comorbidities, education and stage of change are required only if the participant opted into the trial. Show red for missing required fields and orange for missing optional ones. Autosave after 1 s of inactivity, writing only changed keys and removing cleared keys. Flush when leaving the screen. iOS: `DemographicsForm.swift`, `DemographicsData.swift`.
**Acceptance:** Filling the form on Android shows identical values on iOS, and vice versa.

**As built:** `demographics/`: `DemographicsLayout` (one card per section; optional questions say "Optional" in italics below their title, and answers, given or missing, share one secondary colour), built by `DemographicsLayoutMapper` from the `DemographicsRules` field set of the form's region; menus for gender, sex at birth and blood type, a date picker for the date of birth, entry sheets for height and weight, and choice sheets for the rest. `DemographicsEditor` (a shared module) holds the signed-in participant's `DemographicsResponse`, read and written through `DemographicsCodec` (one typed binding per account key; the date of birth is stored as noon in the device's zone). It saves the changed keys one second after the last edit and removes cleared ones, keeps unsaved edits when the account's details refresh, drops them when somebody else signs in, and flushes when the screen goes away; a failed save shows a toast and is retried with the next edit or flush. Gender and sex at birth each fill the other only when it is unanswered (male/trans female → male, female/trans male → female). Opening comorbidities counts as answering "none" until a condition is chosen. The account sheet's Demographics row opens the same form as a screen titled Demographics, closed with its back button. The US or UK form follows the study variant (T63). Height and weight are entered in cm and kg; other units moved to T64. Covered by `DemographicsCodecTest`, `DemographicsRulesTest`, `DemographicsEditorTest`, `DemographicsLayoutMapperTest`, `DemographicsPickerMapperTest` and `DemographicsViewModelTest`, and the `DemographicsLayoutScreenshotTest` baselines.

#### T19: Demographics specialized pickers ✅ Done
**Tags:** `demographics`, `feature`
**Description:** Comorbidities picker (primary and secondary lists, a detail sheet with "Don't know" / month-year diagnosis date, and "None" only when nothing is selected; stored as `yyyy-MM`, `yyyy` or empty). US state/territory picker. UK region → county picker. NHS number field (10-digit mod-11 checksum, "Find your NHS Number" link). Stage-of-change picker (A–I with texts). Depends on T18.
**Acceptance:** Stored encodings match iOS (`england:Kent`, `DC`, …).

**As built:** Comorbidities sheet with primary and secondary conditions, "None" offered only while nothing is chosen, and a "Since" row per chosen condition opening a month/year picker ("Don't know" stores an empty date, year only stores `yyyy`). State/territory sheet with the territories in their own group. UK region: a nation sheet showing the chosen county, which opens that nation's county sheet; stored as `england:Kent`. NHS number entry sheet: spaces and dashes are stripped, Save is enabled only for a valid mod-11 checksum or an empty entry (which clears the number), and a "Find your NHS Number" link opens the NHS page. Stage-of-change sheet with the A–I statements. UK education and income sheets with their own options. Covered by `DemographicsChoiceSheetMapperTest`, `DemographicsPickerMapperTest` and the choice and entry sheet screenshots.

#### T20: Autofill demographics from Health Connect ✅ Done
**Tags:** `demographics`, `health-data`, `feature`
**Description:** "Read from Health Connect" button that fills height and weight from the latest records. (DOB, sex and blood type aren't available in Health Connect; hide them from the explanation text.) iOS: `ReadFromHealthKitButton`.
**Acceptance:** Tapping fills height and weight when records exist.

**As built:** The "Read from Health Connect" card asks for Health Connect access, then reads the latest height and weight independently; whichever was found replaces the answer, as on iOS. A toast reports when neither was recorded or both reads failed. Covered by `DemographicsAutofillTest` and `DemographicsViewModelTest`.

#### T21: Demographics onboarding step ✅ Done
**Tags:** `onboarding`, `demographics`, `feature`
**Description:** Insert the Demographics step before FinalEnrollment. Continue stays disabled until all required fields are complete. iOS: `DemographicsStep.swift`. Depends on T18 and T19.
**Acceptance:** Onboarding can't finish without required demographics.

**As built:** The step, titled Demographics, enters after Notifications (or straight after HealthAccess when notifications are already granted) and before FinalEnrollment; Back from FinalEnrollment returns to it. Continue is enabled once every required answer is given and advances only after the pending answers are stored. That second condition is a deliberate difference: iOS advances right away and flushes when the form disappears. Progress tracking records `demographics`. The SimplifyOnboarding debug flow keeps its short path. Covered by `OnboardingStepProviderTest`, `OnboardingStepLayoutMapperTest` and `OnboardingProgressTrackerTest`.

#### T64: Height and weight in the participant's measurement system
**Tags:** `demographics`, `feature`
**Description:** Show and enter height in feet and inches and weight in pounds for participants whose measurement system is US or imperial, in the demographics rows and entry sheets. Values stay stored as `heightInCM` / `weightInKG`, so switching units never changes what is stored. The measurement system follows the device locale (the same value `MHCEnvironmentTracker` writes as `preferredMeasurementSystem`). iOS: `DemographicsLayout.swift` (`BodyMeasurementRow`, which enters values through SpeziHealthKit's `SaveQuantitySampleView` in the sample type's display unit). Depends on T18.
**Acceptance:** A participant with a US locale enters 5 ft 8 in and 150 lb; the account stores 172.72 cm and 68.04 kg, and the rows show the imperial values again.

#### T22: Onboarding polish parity ✅ Done
**Tags:** `onboarding`, `consent`, `feature`
**Description:** Show the trial/baseline row on FinalEnrollment only if opted in. Remove the demo `delay(1s)` in `OnboardingViewModel.advance`. Add the privacy-policy link and MHC account header copy on the login step. Add a consent PDF share/export action. Align the disclaimer copy with the iOS `ONBOARDING_DISCLAIMER_*` strings.
**Acceptance:** Screen-by-screen copy and behavior match iOS.
**As built:** The final step shows the trial row only when the consent's trial question was answered yes (`OnboardingAnswers.optedIntoTrial`, the same condition as iOS); the login step shows the iOS header copy and a Privacy Policy link. The copy overrides Grove's `account_login_title` and `account_login_description` by name; Grove's login screen has a single description, so the note that original-study accounts don't transfer follows the first paragraph after a blank line instead of in a smaller line. The link is a `ui/FootnoteLink` passed as the `footer` of Grove's `AccountLoginScreen`, a new optional slot below the form (Grove #10, pinned), and opens the privacy policy in the onboarding web sheet. The study's URLs now live in `MHCWebsite` (homepage, privacy policy), used by Home, onboarding and the account overview. Already done before this ticket: the demo `delay(1s)` is gone, and the disclaimer copy matches iOS apart from deliberate adaptations ("phone" for "iPhone", "any time"). The consent step's app bar has a share icon, as iOS's top bar does. Grove's `ConsentScreen` takes an optional non-suspending `onFormChanged`, which its view model caches and calls with a `ConsentFormState` (responses and whether the form is complete) every time a response changes; the screen clears the callback when it leaves the composition (Grove #10, pinned). `OnboardingViewModel` keeps the latest state, enables the icon once the form is complete, disables it while sharing, and hands the form to `standard/consent/MHCConsentHandler` (which now does both the upload and the share and loads the document itself), which renders it with the new `MHCConsentFormRenderer` (initial values and region paper size, the same as for the upload), writes it to the cache's `shared/` folder named after the consent's title, as iOS does (`My Heart Counts Consent Form.pdf`; `file.pdf` without a title) and asks `MainActivity` to open the system share sheet through `NavigationEvent.ShareFile` and a `FileProvider`. A failure shows an error toast. Grove's `GroveIconButton` now dims its icon while disabled. Covered by layout-mapper tests and screenshots (`FootnoteLink`, Grove's login layout with a footer); checked on the emulator against the Firebase emulator suite, including sharing a completed consent form (a valid 9-page PDF reached the share sheet).

#### T56: Align sign-up account keys with iOS ✅ Done
**Tags:** `onboarding`, `account`, `bug`
**Description:** Fixes B11. iOS requires only `userId` and `name` at sign-up and keeps `dateOfBirth` and `mhcGenderIdentity` as manual keys, filled in by the demographics form (`Modules/DeferredConfigLoading.swift:249-273`). Android `MHCAccountConfiguration` collects `dateOfBirth` and supports Grove's `genderIdentity` (stored as `genderIdentity`, not `mhcGenderIdentity`). Make both manual, so that sign-up and the account overview show only email, password and name. Check which keys existing Android accounts already hold, and whether any `genderIdentity` values must be carried over to `mhcGenderIdentity`.
**Acceptance:** The sign-up form and the account overview list the same fields as on iOS, and gender is only ever stored under `mhcGenderIdentity`.
**As built:** `MHCAccountConfiguration` requires name, keeps email and password in the credential flow, registers date of birth as manual and removes the generic `genderIdentity` registration. The existing manual `mhcGenderIdentity` key remains the sole configured gender key. Date of birth remains readable and writable through account storage, without appearing in sign-up or the account overview. Checked against `DeferredConfigLoading.swift` and the account UI's requirement filters. A read-only audit of the local Firebase emulator found 28 participant documents: 20 with `dateOfBirth`, none with `genderIdentity` or `mhcGenderIdentity`; no data migration was needed there. Existing fields are preserved by merge writes. JDK 21 unit/screenshot tests, detekt and debug assembly pass; the new forms have not been checked on a device.
**Follow-up:** Dev and production accounts were not inspected. Audit any legacy `genderIdentity` fields before deciding on a backfill; the generic and study enums have different cases, so no automatic conversion or field deletion is performed.

#### T63: Stanford and Imperial study variants ✅ Done
**Tags:** `onboarding`, `account`, `feature`
**Description:** The study runs as two variants, Stanford (US) and Imperial College London (UK), independent of the Firebase backend that hosts them. The variant sets the study locale (the device language with the variant's region), which decides how consent and other study-bundle resources resolve; the homepage link (Imperial: `https://www.imperial.ac.uk/nhli/research/my-heart-counts/`; its privacy policy is still the Stanford placeholder); the demographics layout (US or UK fields, T18); and the article fallback header image (Stanford's image for Stanford only). It also selects the news feed (`public/news/` or `public/news-UK/`), which matters only if the iOS-inactive News tab is ever ported. Selection: eligibility sets the variant from the chosen country and keeps it in memory until final enrollment, which persists it locally (`enrolledStudyVariant`) next to the backend choice; an install that has a backend but no variant defaults to Stanford. After account creation or login, a missing `studyVariant` account key is filled from the selected variant, and an existing value wins. When complete account details arrive, the account's value becomes the active variant and refreshes the local cache for enrolled sessions; this never changes the backend. Testing path: in debug and internal builds, entering `pls let me in anyway` in the UK "Coming Soon" email field enrolls the participant as Imperial against the US backend, without sending it to the waitlist; Android currently treats GB only as a "Coming Soon" country (`model/Country.kt`). UK consent needs an `en-GB` consent resource, which the bundle does not contain yet. iOS: `SharedContext/StudyVariant.swift`, `Modules/DeferredConfigLoading.swift`, `MyHeartCountsStandard.synchronizeStudyVariant`, `Onboarding/EligibilityScreening.swift`, `Onboarding/AccountOnboarding.swift`, `Screening/RegionComingSoon.swift`, `MyHeartCounts+Website.swift`, `Account/MHCAccountKeys.swift`, README "Temporary UK testing". Relates to T18 and to the region website row in 2.1.
**Acceptance:** Through the testing path, a participant who selects the UK is enrolled as Imperial: `studyVariant = imperial` is written to the account, study resources resolve with the `en-GB` locale, and the homepage link points to Imperial. The variant survives relaunch and, through the account, reinstall; selecting the US enrolls as Stanford exactly as today.
**As built:** `model/StudyVariant` (`Stanford` / `Imperial`, serialized as `stanford` / `imperial`) carries each variant's region, homepage and privacy policy. `study/StudyVariantProvider` (a DI singleton) holds the variant as a `StateFlow` (`variant`) and as a plain read (`currentVariant`), persists it in its own storage file, defaults to Stanford, and derives `contentLocale()`: the device language when the study offers content in it (English, Spanish) and English otherwise, combined with the variant's region. The study manager's preferred locale, consent (text and PDF paper size), articles, questionnaires, both explainer sources, the demographics form, the website links and the article fallback image all read it, replacing the backend region (`MHCFirebaseLoader.persistedRegionLocale` is gone, and Firebase no longer touches the study locale). Leaving eligibility for the study selects the variant of the participant's country. The UK testing path is the `UKStudyTesting` feature flag (off by default, toggled in the debug sheet) instead of iOS's unlock phrase: while it is on, an otherwise eligible participant in the United Kingdom is admitted to the study as Imperial on the US backend rather than sent to the waitlist. The "Coming Soon" page's study-website link points to Imperial for GB. `study/StudyVariantSynchronizer`, a Grove module that starts on `configure()`, keeps the study locale in step with the variant, restores the `studyVariant` account key whenever complete, non-anonymous account details or the device's variant change (so re-running eligibility after a login cannot override it), and writes the device's variant to an account without one. The demographics form switches region as soon as the variant changes. Sign-out without an enrollment clears the variant with the backend region. `res/values-en-rGB` carries the British spellings. Deliberate differences: the variant is persisted when selected rather than at final enrollment, as the Android backend region already is; and a missing account value is filled on any complete sign-in rather than only during onboarding, which also backfills accounts created before T63. Covered by `StudyVariantProviderTest`, `StudyVariantSynchronizerTest` and new `ArticleMapperTest` cases; JDK 21 unit tests, detekt and debug assembly pass. Not checked on a device.
**Follow-up:** The bundle has no `en-GB` consent, so an Imperial participant's consent resolves to the English (US) document until one ships. A UK Firebase project still needs its own configuration source before `MHCFirebaseLoader.optionsFor` can tell regions apart. The news feed path is not modelled (no News tab).

### Phase 4: questionnaires and tasks

#### T23: Questionnaire presentation and response upload ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `questionnaires`, `tasks`, `feature`
**Description:** Resolve the questionnaire from the enrolled study bundle in the preferred locale (with fallback). Render it with Grove's `QuestionnaireComposable` (Android FHIR Data Capture). Confirm that all 14 surveys render, including quantity items with units, `enableWhen`, and any Spezi/ResearchKit-specific extensions they use. Fix content or MHC-side preprocessing where they don't render. On submit, upload through `MHCQuestionnaireUploader` (canonical URL, app revision from T05), then complete the event. Cancelling leaves the event open. iOS: `PerformTaskAnchor.swift`, `MyHeartCountsStandard+QuestionnaireResponse.swift`.
**Acceptance:** Every scheduled survey can be answered, and each response lands in `questionnaireResponses`.

#### T24: Extract HeartRisk values into health data
**Tags:** `questionnaires`, `health-data`, `feature`
**Description:** After a HeartRisk response, extract systolic/diastolic BP and glucose by `linkId` (iOS `MyHeartCountsStandard+QuestionnaireResponse.parseIfApplicable`). Persist them according to D1: either Health Connect records or custom samples.
**Acceptance:** Values entered in HeartRisk show up on the dashboard.

#### T25: Upcoming Tasks tab ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `tasks`, `feature`
**Description:** Replace the placeholder. Show the next 2 weeks grouped by day, using the same tiles and interaction rules as Home, a "No Upcoming Tasks" empty state, and an "Other Tasks – Always Available" section excluding activities already scheduled. Add a top-bar menu for always-available tasks (including a 30-second walk test in debug mode). iOS: `UpcomingTasksTab.swift`, `TasksList.swift`, `AlwaysAvailableTaskActions*.swift`. Build from Grove `ui-scheduler` (`TaskListLayout`, `TaskSection`, `TaskTile`, `NoTasksContent`, `TaskActionsMenu`), as Home does.
**Acceptance:** The layout matches iOS, and tasks can be performed from this tab.

#### T26: Missed tasks list ✅ Done in [#40](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/40)
**Tags:** `tasks`, `feature`
**Description:** Wire Home's missed-tasks row to a list of missed events from the past 2 weeks, grouped by day, with actions. Use the iOS rule: the most recent uncompleted occurrence whose next occurrence is upcoming and not yet completed. iOS: `MissedEventQuery.swift`. Reuse `MissedTasksRow`.
**Acceptance:** The count on Home equals the entries in the list.
**As built:** `task/MissedEventsSource` queries past and upcoming events with `Scheduler.queryEvents` and matches each past event's next occurrence (`ScheduleCalculator.nextOccurrence`). Grove `queryMissedEvents` isn't used, because it counts every elapsed, uncompleted event (B9). Tile mapping and event performing were extracted into `task/` and are shared with Home and the Tasks tab.

#### T27: Unscheduled task completion reporting
**Tags:** `tasks`, `feature`
**Description:** When an always-available activity (ECG, 6MWT, 12-min run) finishes, complete the first matching open scheduled event within the next 2 weeks, if the completion policy allows. iOS: `PerformTask.reportCompletion`.
**Acceptance:** A 6MWT run from "Other Tasks" completes the scheduled 6MWT.

#### T28: Article presentation parity ✅ Done
**Tags:** `tasks`, `feature`
**Description:** Extend `MarkdownSheet` to show the article header: title, date, tags and header image from front matter (the `stanford` asset fallback). Use it for scheduled articles (Welcome, Week2WhatsNext). iOS: `ArticleSheet.swift`, `Article.swift`.
**Acceptance:** Articles look like iOS.
**As built:** `ArticleMapper` maps the document's front matter to a typed `ArticleHeader`: title, optional ISO 8601 publication date formatted for the participant's locale and injected time zone, comma-separated tags with blanks removed, and an optional image. Missing `headerImage` and the `stanford` asset name resolve to the bundled Stanford photograph; explicit HTTPS images load through Coil, while unknown assets or malformed references show no image. The photograph is packaged as a WebP with its original license attribution. `EventPerformer` asks the mapper to prepare `MarkdownSheet`, including the header and a body with its redundant leading `#` title removed. The composable only renders the prepared header, divider and Markdown in a single scrolling list. Section headings and bodies without a header title are preserved. Presentation still completes an article, and the shared performer covers Home, Tasks and Missed Tasks. Compared with `Article.swift`, `ArticleSheet.swift` and `PerformTaskAnchor.swift`; deliberate platform differences are the fixed Grove app bar and a themed translucent title background instead of a scroll-following navigation title and material blur. Tags wrap below the date rather than scrolling horizontally, and the header grows to fit long or enlarged text without clipping. Tests cover date offsets, locale and zone changes, missing and malformed metadata, tags, image selection, leading-title removal, preservation of other headings and code, and presentation callbacks; the full-sheet Paparazzi baseline includes every header field and was inspected visually. Both `ArticleHeader` and `MarkdownSheet` have light/dark previews; the header previews and six focused screenshot baselines cover all metadata, title only, date only, tags only, and a long title with wrapping tags at a narrow width, including 150% font size.
**Follow-up:** Fallback branding follows the study variant since T63: an article without `headerImage` shows the Stanford photograph for Stanford only. Shipped articles carry no date or tags today, so those optional fields are covered by unit and screenshot fixtures. Remote image fetching and the updated sheet have not been checked on a device.


#### T54: Open the task when a reminder is tapped
**Tags:** `notifications`, `tasks`, `feature`
**Description:** G06 ✅ makes a reminder tap open the launch activity (`MainActivity`) with the task event attached. In `MainActivity`, read it with `SchedulerNotifications.openedNotification(intent)` (task ID and occurrence start) in `onCreate` and `onNewIntent`, route to the Home or Tasks tab and highlight or open the task. Track the open in `notificationTracking`, as FCM nudges already do. Set the MHC notification icon (a monochrome drawable is needed; nudges use the launcher icon today), color and channel name/description through `SchedulerNotificationsConfiguration.DEFAULT.copy(…)`. iOS handles task-bound notification taps in `MyHeartCountsStandard+Notifications.swift`. Depends on G06.
**Acceptance:** Tapping a survey reminder opens the app on that survey.

#### T55: Fix survey content for standard FHIR renderers
**Tags:** `questionnaires`, `study-definition`, `bug`
**Description:** Fixes the content side of B10 in `MyHeartCounts-StudyDefinitions`. Code `itemControl` under the CodeSystem `http://hl7.org/fhir/questionnaire-item-control`, set `repeats: true` on check-box items, declare `questionnaire-unitOption` on quantity items, and use decimal bounds on quantity items. Coordinate with iOS, since ResearchKitOnFHIR has to keep rendering the same files. Once a study revision with the fixes ships, remove the matching rewrites from `study/StudyQuestionnaire.prepare`, keeping its tests as a regression check on the shipped surveys (`MHCStudyBundleTest`).
**Acceptance:** All 14 surveys render and record answers without app-side preprocessing, on both platforms.

#### T59: Task state after reinstall and re-enrollment
**Tags:** `tasks`, `decision`
**Description:** Scheduler completions are stored only on the device. After a reinstall, completed surveys show as open again, and re-enrolling recreates tasks effective from "now", so occurrences before the reinstall (and their missed state) are gone, although `dateOfEnrollment` is reused. First check what iOS does after a reinstall (SpeziScheduler also stores outcomes locally). If iOS restores state, for example from `questionnaireResponses`, do the same here. Otherwise, record the behavior as shared by both platforms.
**Acceptance:** Reinstall behavior is documented, and matches iOS.

### Phase 5: active tasks

#### T29: ECG task on Android
**Tags:** `active-tasks`, `decision`
**Description:** Implement D2. At minimum, never show an ECG action that can't be completed meaningfully. Options: hide it, or show instructions with manual completion.
**Acceptance:** The ECG behavior on Android is documented and consistent between Home and Tasks.

#### T30: Wear OS companion for timed tests (optional)
**Tags:** `active-tasks`, `platform`, `feature`
**Description:** If D5 is yes, start a Wear OS exercise session with heart rate and a countdown when the phone test starts. iOS: `MyHeartCountsWatchApp/*`, `WatchConnection.swift`.
**Acceptance:** A watch session records the test workout.

#### T31: Timed walking/running test
**Tags:** `active-tasks`, `feature`
**Description:** Port `TimedWalkingTest` for the 6MWT and the 12-minute Cooper run. Request the `ACTIVITY_RECOGNITION` permission, with an error section and a settings link. Count steps with the step counter sensor (or the Recording API); estimate distance from steps × stride, or use GPS if the product team agrees. Show a large countdown. Run a foreground service with an ongoing notification (the Live Activity analog). Vibrate at the end. Persist the in-progress test and on relaunch resume it or finalize it; a recovered test can be cancelled. Show a result section with steps, distance and the VO₂max estimate `(distance − 504.9) / 44.73` for runs. Upload a FHIR Observation with the iOS LOINC components via T12. Allow restart. iOS: `Timed Walk Test/*`.
**Acceptance:** A test survives app kill and uploads a result compatible with iOS.

#### T32: Past timed test results
**Tags:** `active-tasks`, `dashboard`, `feature`
**Description:** A screen reachable from Heart Health → "Past Data" that lists results from Firestore (newest first) with details and "New Test" buttons. iOS: `PastTimedWalkTestResults.swift`, `PastTimedWalkTestResultInfo.swift`. Depends on T12 and T31.
**Acceptance:** Results from both platforms for the same account are listed.
**Progress:** Heart Health now includes the "Past Data" section after the score explainer, with "Past Timed Walk/Run Test Results" linking to `MHCRoute.PastTimedWalkTestResults`. The destination has an app bar, back navigation and placeholder content. Firestore results, details and new-test actions remain open.

### Phase 6: account sheet and prompted actions

#### T33: Account sheet study section and app info 🟡 Partial
**Tags:** `account`, `feature`
**Description:** Extend the account overview with: a Study Participation section (study title, short explanation, "Enrolled since", tap for website); an About row (version; 5 taps show extended info with build, Firebase project ID, account ID, enrolled/fetched study revision, `lastSignedConsentVersion`); a privacy policy link (region-aware); license information (open-source licenses screen); and Change Language (Android per-app language settings). iOS: `AccountSheet.swift`.
**Acceptance:** Sections match iOS.
**Progress:** About row and privacy policy link are built. `account/overview/AppInfoSection` is a card below the post-trial nudges toggle with the app name and version and a Privacy Policy row that opens `https://myheartcounts.stanford.edu/privacy` (the URL for both variants). Diverges from iOS by decision: the extended info (version and build, Firebase project ID and account ID, enrolled and bundled study revision and `lastSignedConsentVersion`; "n/a" when unavailable, labels untranslated as on iOS) isn't behind five taps on the version but is the App Info section of `debug/DebugSheet`, which the testing environment opens on a shake and production never offers (every build, release included, runs as testing until T65); the sheet also lists the feature flags with a restart button. The bundled revision stands in for iOS's fetched one until the bundle is downloaded (T08). Covered by view-model, mapper and screenshot tests; checked on the emulator against the Firebase emulator suite. Open: Study Participation section (which also hosts the post-trial nudges toggle on iOS and is shown only while enrolled), license information, Change Language. The Study Participation section is started: `account/overview/StudyParticipationSection` is the first card of the overview and holds a "View Participation Stats" row opening the participation stats sheet (T58). Its other rows (study title and short explanation, "Enrolled since", tap for the website) are still open.

#### T34: Post-trial nudges toggle ✅ Done
**Tags:** `account`, `feature`
**Description:** A toggle bound to `extendedActivityNudgesOptIn` with 250 ms debounce and iOS copy. iOS: `AccountSheet.PostTrialNudgesToggle`.
**Acceptance:** Toggling updates the account document.
**As built:** `account/PostTrialNudges` (+Impl) reads the opt-in from the account details (`false` when unset) and writes only that key. A `ui/ToggleSection` (a generic card with a title, subtitle and switch, here with the iOS copy) is the first item of the account overview's custom sections, above Withdraw from Study. `MHCAccountOverviewViewModel` mirrors the stored value, switches at once on a tap, and writes 250 ms after the last change, cancelling a pending write. Diverges from iOS, which ignores a failed write: the switch returns to the stored value and an error toast is shown, through a `GroveScaffold` without an app bar that `MHCAccountOverview` wraps around Grove's overview. It also diverges in being shown to every signed-in participant rather than only inside the enrolled-only Study Participation section, which arrives with T33. Covered by view-model, mapper and screenshot tests. Checked against the Firebase emulator suite: a toggle writes `extendedActivityNudgesOptIn`, rapid toggles store only the last value, and a backend change updates the switch live. The failure path needs a Grove fix: `FirebaseAccountServiceImpl.updateAccountDetails` used to log a rejected Firestore write and still return success, so the switch kept the rejected value. It now refreshes the user and then returns the failure (Grove #10, pinned); with that, a write rejected by the emulator's rules reverts the switch and shows the error toast.

#### T35: Review signed consent forms
**Tags:** `consent`, `account`, `feature`
**Description:** List `users/{id}/consent/*.pdf` newest first, showing version and title (from the `consentFormMetadata` custom metadata) and the signing date. Tap to download and open the PDF in a viewer; support pull to refresh. iOS: `SignedConsentForms.swift`.
**Acceptance:** Consent PDFs signed on either platform can be opened.

#### T36: Send feedback ✅ Done
**Tags:** `account`, `feature`
**Description:** A feedback form writing to `feedback/{uuid}` with the iOS schema: `version: 1`, `accountId`, `message`, `date`, `timeZone`, `appVersion`, `appBuildNumber`, and `deviceInfo {osVersion, model, type}`. Block dismissal while text is unsent. iOS: `Feedback/*`.
**Acceptance:** The backend receives documents in the same shape as from iOS.
**As built:** A "Send Feedback" row (`account/overview/SupportSection`, the card Change Language can join) opens `FeedbackSheet` through the overview's scaffold: a text field focused on open with the iOS footer copy, a close button and a Send button in the app bar that is enabled once the message isn't blank and shows progress while sending. The sheet can't be swiped or tapped away while a message is unsent or sending; the close button discards it, as iOS's Cancel does. `account/feedback/MHCFeedback` (+Impl) writes `feedback/{UUID}` with the iOS fields: `version` 1, `accountId`, `message` (trimmed), `date` as a timestamp, `timeZone`, `appVersion`, `appBuildNumber` and `deviceInfo` (`osVersion`, `model` = the device model, `type` = the device's hardware name, now carried by `MHCAppRevision` alongside the OS version). Success closes the sheet; a failure shows an error toast and keeps the message. `MHCAppRevision` moved from the overview's view model into its mapper, which builds the About row. Covered by view-model, mapper and screenshot tests; checked on the emulator against the Firebase emulator suite, where the project's rules accepted the document.

#### T37: Update comorbidities from settings
**Tags:** `account`, `demographics`, `feature`
**Description:** A settings entry that opens the comorbidities picker and flushes on Done. iOS: `UpdateComorbiditiesButton.swift`. Depends on T19.
**Acceptance:** Changes persist to `comorbidities`.

#### T38: Verify email sheet
**Tags:** `account`, `feature`
**Description:** Show the verification status; "Get Link" sends the Firebase verification email; "Open Mail" opens the mail app; poll every 2 s to refresh the account until verified. iOS: `VerifyEmailSheet.swift`. Depends on G08 ✅: `AccountService.sendEmailVerification()` for "Get Link" and `refresh()` for the polling, which re-reads only the Firebase user, not Firestore.
**Acceptance:** The status flips to verified without restarting the app.

#### T39: Prompted actions framework
**Tags:** `prompted-actions`, `home`, `feature`
**Description:** Replace the empty `pendingActions` with a real source. Actions have states (pending, completed, unavailable). Availability is relative to `studyActivationDate` (T13); SensorKit and clinical-records actions use a 21-day window. Rejections are persisted locally with a "Stop Suggesting This?" confirmation. The digest card opens a checklist sheet and closes automatically once everything is done. The account sheet has a "view all" variant including completed actions. Initial actions: **Complete Demographics** (T18) and **Verify Account Email** (T38). IDs must match iOS (`edu.stanford.MyHeartCounts.HomeTabAction.*`). iOS: `Home Tab/Prompted Actions/*`. Grove `ui-scheduler` already has `PromptedActionsCard` and `PromptedActionRow`; the state, persistence and action logic are MHC work.
**Acceptance:** A participant with incomplete demographics or an unverified email sees and can resolve the actions.

#### T40: Data processing indicator
**Tags:** `account`, `health-data`, `feature`
**Description:** Show "Processing Health data…" in the account sheet while live or historical uploads run. In debug mode, link it to a progress view. iOS: `AccountSheet.dataProcessingRow`, `DataProcessingDebugView.swift`.
**Acceptance:** The indicator reflects `ManagedFileUpload` and historical-export activity.

#### T58: Participation stats
**Tags:** `account`, `health-data`, `feature`
**Description:** A "View Participation Stats" entry in the study participation section (T33) opening a stats screen: an explainer with the enrollment date; engagement (tasks done, current and longest streak, surveys answered, articles read, ECGs recorded, walk/run tests); health totals since enrollment (steps, heartbeats estimate, distance, active energy, exercise time, sleep, workouts, flights climbed); highlights (best step day, longest workout, max heart rate, average resting heart rate); and fun facts. Engagement counts come from local scheduler records; health totals, highlights and the ECG count come from the stats documents (T62), as on iOS. Exercise time follows D3, and the ECG tile follows D2. iOS: `Account/ParticipationStatsView.swift`, `ParticipationStatsProvider*.swift`, `docs/ParticipationStats.md`. Depends on T62.
**Acceptance:** The screen shows the same sections as iOS, with values computed from scheduler outcomes and the stats documents.
**Progress:** UI only, with sample data. `MHCRoute.ParticipationStats` opens a bottom sheet from all three iOS entry points: the medal action that `StudyAppBarProvider` adds to the Home and Heart Health tabs, and the "View Participation Stats" row of the account overview's Study Participation section (T33). The sheet has three pages that slide like onboarding steps: the stats (enrollment card with achievements unlocked and upcoming, engagement, health totals, health highlights, fun facts), the explainer (from the info action), and the full achievements list (from the enrollment card). Built from the `dashboard/ui` components (G07: `CounterCard`, `StatTileGrid`, `IconTextCard`, `ProgressRingCard`, laid out by `DashboardSectionList`); `stats/ParticipationStatsContentMapper` builds the whole sheet, and every value comes from `SampleParticipationStatsContentMapper` and `achievements/SampleAchievementsSource`. Screenshot tests cover each page.
**TODO (cleanup when wiring real data):** remove the hardcoded `DUMMY DATA` badge in `stats/ParticipationStatsScreenContent`, and replace `SampleParticipationStatsContentMapper` and `SampleAchievementsSource` with implementations reading scheduler outcomes and the stats documents (T62/T63). Achievements still need a ticket of their own for unlock tracking (iOS `AchievementsManager`, `achievementTracking` in Firestore).

### Phase 7: Heart Health dashboard

#### T41: CVH score engine ✅ Fully implemented
**Tags:** `dashboard`, `feature`
**Description:** Port `ScoreDefinition`/`ScoreResult` and all band tables (diet, exercise minutes, steps, nicotine, sleep, BMI standard/Asian by race/ethnicity, LDL, glucose, BP custom, mental wellbeing as WHO5 × 4), plus the composite (mean of the scores clamped to 0–1, only if ≥ 5 of 8 components are available; mental wellbeing is excluded). Inputs, as on iOS `5bd8ee3`: from the stats documents (T62) — exercise minutes summed over the 7 days ending yesterday; the step-count tile instead when there is no exercise time but there are steps, scored on the mean of the daily sums over days with data; the most recent sleep session within 14 days; BMI averaged over 14 days, else derived from weight (last 3 months, and under 6 months old) and the most recent height ever, the newer of the two winning; BP over 3 months. From Firestore custom samples (T42) over 2 months — diet, WHO5, nicotine, LDL. Fasting glucose straight from Health Connect over 14 days (Grove `Health.query`), the one input iOS still reads from the device. D3 applies through the values T61 writes. iOS: `CVHScore.swift`, `CVHScore+ScoreDefinitions.swift`, `Score.swift`. Depends on T42 and T62.
**Acceptance:** Unit tests reproduce iOS results on shared fixtures; scores update live as the stats documents and custom samples change.
**As built:** `HeartHealthScoreSourceImpl` replaces the empty-score source in DI. Live T62 streams supply exercise, daily steps, sleep, BMI, weight, height and BP; T42 supplies diet, WHO-5, nicotine and LDL. Health Connect supplies the latest glucose reading, converted to mg/dL and refreshed every 15 seconds. `HeartHealthQueryRanges` is injected; all range getters use the current injected `TimeProvider`, and calendar changes renew subscriptions. Windows follow the reference's calendar boundaries: seven completed days for exercise/steps, fourteen days including today, two calendar months including this month for custom observations, three including this month for weight/BP, and all time for height. Account/ethnicity changes restart inputs from empty; a refresh or a new day keeps the previous inputs until new values arrive, so the scores never blank in between. Failures leave the other factors usable. `HeartHealthScoreCalculator` applies numerical bands, the exercise/steps choice (including the unaligned-step guard), most-recent selections, BMI fallback, and a five-factor composite excluding mental wellbeing. The temporary ViewModel probe is removed. Numerical band explainers are still T45. Deliberate difference: WHO-5 uses positive `value * 4 / 100`, clamped to 0–1; the reference `ScoreDefinition.range` uses signed distance in the reverse direction, producing negative intermediate scores. Invalid/non-finite numeric inputs are unavailable rather than passed into gauges.

#### T42: Firestore custom-sample queries ✅ Fully implemented
**Tags:** `dashboard`, `backend`, `feature`
**Description:** Live Firestore listeners on `HealthObservations_{customType}` with time-range filtering on `effectiveDateTime` / `effectivePeriod` and FHIR decoding to `QuantitySample`. iOS: `MHCFirestoreQuery.swift`. Depends on T12.
**Acceptance:** Diet, WHO5, nicotine and LDL values from the backend appear.
**As built (branch `feature/heart-health-dashboard`, not merged):** package `customsamples/`. `CustomQuantitySampleType` (LDL in mg/dL, diet, WHO-5, nicotine; the iOS fasting-glucose and A1c types are not read by the dashboard and are left out) and `CustomQuantitySample`. `CustomSampleDecoder` reads the stored FHIR `Observation` fields directly, with the checks iOS's decoding enforces: `resourceType` Observation, a UUID `id`, a `status`, `code.coding`, exactly one `value[x]` and it a `valueQuantity` with a known unit and a number, exactly one `effective[x]` (dateTime, instant, or a period with start and end); FHIR partial dates start in the participant's zone. With a time range, a reading counts when its `effectiveDateTime` lies in it and a period when it overlaps it; anything else (including `effectiveInstant`) is left out, as on iOS. `CustomSampleSource.samples(type, timeRange)` listens to `users/{uid}/HealthObservations_{type}`, follows the signed-in account and emits the readable samples newest first, as `Result`s. `HealthStatsUnit` became `model/HealthUnit`, gaining mg/dL and mmol/L (not convertible into one another). Divergences: a value in a unit that does not convert to the type's unit, or a period ending before it starts, is left out (iOS stops with a precondition failure). T12 now supplies custom-observation uploads; timed-walk uploads remain open. T41 consumes these streams directly.

#### T43: Chart components ✅ Fully implemented
**Tags:** `dashboard`, `feature`
**Description:** Line/bar/point charts with automatic aggregation intervals, date x-axis stride labelling, tap-to-highlight tooltips, a BP dual-series chart, a sleep per-night bar chart, and a time range picker (7d/14d/1m/3m/6m/12m). iOS: `Health Dashboard/*`. Implemented as Grove `ui` components (G07); this ticket wires them to MHC data: stats documents for health-derived metrics (T62), Firestore custom samples (T42), and Health Connect for blood glucose, matching iOS `DefaultHealthDashboardTile`. Depends on T42 and T62.
**Acceptance:** Charts render the same data as iOS for the same time range.
**As built:** Component detail charts use Grove’s Cartesian chart with calendar-range menus, localized axis strides, highlights and metric-specific series. Steps remain daily bars; sleep totals all sessions ending on each calendar day and formats highlights as hours/minutes. Exercise and BMI use range-dependent aggregation, glucose uses native readings averaged into calendar buckets, and LDL/WHO-5 retain individual raw observations. Blood pressure shows separate red systolic and blue diastolic lines with points; steps are orange and LDL red. Highlights identify aggregate date intervals and format BMI to one decimal. WHO-5 chart values remain on the original questionnaire scale; its gauge retains the normalized score.

#### T44: Heart Health tab
**Tags:** `dashboard`, `feature`
**Description:** Replace the placeholder with the overall gauge (0–100, grey when unavailable) and header text; a "Score Components" grid of 9 tiles (the exercise *or* steps tile chosen by data availability), each with a gauge and last date or "Tap to learn more…", plus the Apple-Watch footer adapted for Android; "Understanding Your Heart Health Score" from `hhdExplainer/LearnMore`; and "Past Data" linking to T32. iOS: `HeartHealthDashboard.swift`. Depends on T41, T42 and G07 (gauge).
**Acceptance:** The dashboard shows scores with parity to iOS.
**Progress (app branch `feature/heart-health-dashboard` off `main`, not merged):** the tab follows the Home pattern — `HeartHealthViewModel` (action sink, Loading/Error/Content state with retry), `HeartHealthContentMapper` (`dashboard/ui/GaugeCard` with 0–100 range and header footnote; "Score Components" `GaugeTileGrid` with the footer adapted to Health Connect; captions time today / "Yesterday" / date, "Tap to learn more…" without a score; tap → `MHCRoute.HeartScoreDetail`), `HeartHealthScreenContent`, `HeartHealthAction`, `HeartScoreComponent` (10 factors), `HeartHealthScores`. `HeartHealthContentLayout` renders an ordered `sections: List<DashboardSection>` in a `LazyColumn`, omitting the optional explainer when absent. The "Understanding Your Heart Health Score" section is done (`HeartHealthExplainerSource` reads the localized `hhdExplainer/LearnMore` from the study bundle; `ui/MarkdownCard`). A medal app-bar action opens `MHCRoute.ParticipationStats`. Scores now come from `HeartHealthScoreSourceImpl`, which combines T42/T62 and Health Connect glucose with T41 scoring; the temporary ViewModel probe is removed. Open: past-results list/detail and new-test actions (T32). The "Past Data" section and "Past Timed Walk/Run Test Results" disclosure row are now wired to `MHCRoute.PastTimedWalkTestResults` with a back-enabled placeholder destination. The app bar orders Stats and Achievements before Account.

#### T45: Score component detail screen ✅ Fully implemented
**Tags:** `dashboard`, `feature`
**Description:** Most recent value (units; sleep as h + min; BP as sys/dia) with gauge; recent-values chart (none for diet and nicotine); band explainer with the matched band highlighted (hidden for mental wellbeing); "About X" markdown from `hhdExplainer` with `link*` front-matter buttons; "Add Data" where supported; debug browsing of Firestore samples. iOS: `DetailedHealthStatsView*.swift`, `Score+ExplainerUI.swift`. Depends on T43 and T44.
**Acceptance:** Every component opens a detail screen matching iOS.
**As built:** Component details open as bottom sheets with a close action, large metric heading and a top-bar plus action where data entry is supported. The content mapper supplies the latest-value gauge, metric-titled history chart and range picker, score bands, localized explanations and links, and expandable custom samples for debug accounts. Route arguments use Grove’s `groveViewModel` args receiver. Debug samples remain inline and use the account’s `enableDebugMode` flag; launch-option support belongs to T49.

#### T46: Dashboard data entry ✅ Fully implemented
**Tags:** `dashboard`, `health-data`, `feature`
**Description:** BP (systolic/diastolic with field navigation and limits 60–250 / 30–150); fasting glucose (40–400); BMI (BMI directly, or weight + height with a ft/in picker and auto-computed BMI; saves all three); LDL (30–400, to Firestore via T12); diet, WHO5 and nicotine via their questionnaires (T23). Locale-based units with limits; error message when permission is missing. Storage per D1. iOS: `Data Entry/*`, `HeartHealthDashboard.addSampleSheet`.
**Acceptance:** Entered values update the matching score immediately.
**As built:** `dashboard/entry/` provides dated BP, fasting glucose, LDL and BMI forms, exclusive upper input limits, localized numeric parsing, weight in pounds for the US and UK measurement systems and kilograms otherwise, height in feet/inches only for the US system and centimetres otherwise, with BMI calculation, retained form state and save errors. BP, glucose, height and weight save atomically to Health Connect with manual-entry metadata and stable retry identifiers; the app requests their write permissions and explains them in its privacy copy. LDL uses `CustomSampleUploader` to write a FHIR observation directly to Firestore. Direct BMI is preserved in monthly BMI stats (local-offset timestamp in whole seconds, like the other stats entries) because Health Connect has no BMI record type; weight/height entry uses native records and the existing derived-BMI pipeline. A successful save refreshes the score inputs and requests a background stats refresh, so saving never waits for the server. Diet, WHO-5 and nicotine open their study questionnaires through the existing renderer and response uploader without completing a scheduled event. Detail and entry screens pass route arguments through Grove’s `groveViewModel` args receiver. Native measurements cannot be future-dated, as required by Health Connect. Entry opens as a nested bottom sheet with grouped date/time and measurement rows, a BMI versus weight/height selector, and Grove’s async Save text action in the top bar. A dedicated mapper builds display-ready content with click callbacks and field-level state flows. Numeric inputs use compact widths beside their units, with date/time controls aligned to the trailing side. A value outside its limits shows "Only values from X to Y are allowed" below its row. Blood pressure, glucose and LDL take whole numbers only, with a number keyboard that ignores other characters. The imperial height picker offers 3–8 ft and 0–11 in, starting at 5 ft 4 in, and is not range-checked in metres. The form defines its limits per field, and both validation and the picker options read them from there.

### Phase 8: consent renewal

#### T47: Consent renewal flow
**Tags:** `consent`, `feature`
**Description:** After onboarding, when a (re)loaded study bundle carries a consent version newer (minor or above) than `lastSignedConsentVersion`, show a non-dismissible flow: an explainer with the last-signed date, then the consent form, then upload and account update. "Never signed" triggers the flow by default, with a switch to disable it for tests. iOS: `ConsentManager.swift`, `ConsentRenewalFlow.swift`. Depends on T08 and T14.
**Acceptance:** Publishing consent `0.2.0` prompts every enrolled Android participant once.

### Phase 9: localization

#### T48: Spanish and British English localization
**Tags:** `localization`, `feature`
**Description:** Add `values-es` and `values-en-rGB` from iOS `Localizable.xcstrings` (≈550 Spanish and ≈530 en-GB entries). Make sure study content (consent, articles, questionnaires, `hhdExplainer`) resolves in the preferred locale with the iOS fallback rule (region-specific English for US/UK). Include plurals. Grove-rendered screens (account, consent, onboarding, scheduler, questionnaire chrome) are translated in the app too: a string in the app's `values-es` replaces the Grove string of the same name. Those translations are prepared below; add them as `res/values-es/grove_strings.xml` together with the app's own Spanish, so Spanish ships complete (a partial `values-es` fails lint's `MissingTranslation` and mixes languages on screen).
**Acceptance:** The app is fully usable in Spanish, and UK participants get en-GB copy.
**Progress:** The en-GB part landed with T63: `res/values-en-rGB` overrides the 11 app strings whose British spelling differs (the reference app's other en-GB entries are identical to US English), and study content resolves in the study variant's locale. Spanish is still open.
<details>
<summary>Prepared Spanish for the Grove strings MHC shows (87; wording after SpeziAccount's Spanish, informal <i>tú</i>; needs review by a Spanish speaker)</summary>

```xml
<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="ExtraTranslation">
    <!-- Grove components, in Spanish; each string replaces the Grove string of the same name. -->
    <!-- ui -->
    <string name="edit_action_title">Editar</string>
    <string name="date_picker_confirm_button_title">Aceptar</string>
    <string name="date_picker_dismiss_button_title">Cancelar</string>
    <string name="viewstate_default_error_title">Error</string>
    <string name="viewstate_default_error_message">Se ha producido un error desconocido.</string>
    <string name="viewstate_confirm_title">Aceptar</string>
    <!-- account -->
    <string name="account">Cuenta</string>
    <string name="account_key_account_id">Identificador de Cuenta</string>
    <string name="account_key_user_id">Identificador de Usuario</string>
    <string name="account_key_email">Correo electrónico</string>
    <string name="account_key_name">Nombre</string>
    <string name="account_key_password">Contraseña</string>
    <string name="account_key_gender">Género</string>
    <string name="account_key_date_of_birth">Fecha de Nacimiento</string>
    <string name="account_key_date_of_birth_placeholder">Selecciona tu fecha de nacimiento</string>
    <string name="account_key_date_of_birth_unspecified">Sin especificar</string>
    <string name="account_login_title">Tu Cuenta</string>
    <string name="account_login_description">Por favor inicia sesión en tu cuenta. O crea una nueva si aún no tienes una.</string>
    <string name="account_login_or_divider">o</string>
    <string name="account_login_password_placeholder">Contraseña</string>
    <string name="account_login_forgot_password">¿Olvidaste tu contraseña?</string>
    <string name="account_login_button">Iniciar sesión</string>
    <string name="account_login_sign_up_info">¿Aún no tienes una cuenta?</string>
    <string name="account_login_sign_up">Regístrate</string>
    <string name="account_login_user_id_email">Dirección de Correo Electrónico</string>
    <string name="account_login_user_id_username">Nombre de Usuario</string>
    <string name="account_login_invalid_user_id">El valor de «%1$s» no es válido</string>
    <string name="account_login_password_reset_success">Se ha enviado un correo electrónico para restablecer la contraseña.</string>
    <string name="account_login_password_reset_failed">No se pudo restablecer la contraseña. Inténtalo de nuevo.</string>
    <string name="account_login_failed">No se pudo iniciar sesión. Inténtalo de nuevo.</string>
    <string name="account_login_sign_in_failed">No se pudo iniciar sesión. Inténtalo de nuevo.</string>
    <string name="account_overview_title">Resumen de la Cuenta</string>
    <string name="account_overview_section_title">Cuenta</string>
    <string name="account_overview_logout_button">Cerrar sesión</string>
    <string name="account_overview_logout_failed">No se pudo cerrar la sesión. Inténtalo de nuevo.</string>
    <string name="account_overview_edit_button">Editar</string>
    <string name="account_overview_done_button">Listo</string>
    <string name="account_overview_sign_in_security">Inicio de Sesión y Seguridad</string>
    <string name="account_overview_change_password">Cambiar Contraseña</string>
    <string name="account_name_given_name">Nombre</string>
    <string name="account_name_family_name">Apellido</string>
    <string name="account_name_given_name_placeholder">Escribe tu nombre</string>
    <string name="account_name_family_name_placeholder">Escribe tu apellido</string>
    <string name="account_sign_up_title">Crear una nueva cuenta</string>
    <string name="account_sign_up_description">Por favor, rellena los detalles a continuación para crear tu nueva cuenta.</string>
    <string name="account_sign_up_button">Registrarse</string>
    <string name="account_sign_up_failed">No se pudo completar el registro. Inténtalo de nuevo.</string>
    <string name="account_edit_save_button">Guardar</string>
    <string name="account_edit_failed">No se pudo actualizar. Inténtalo de nuevo.</string>
    <string name="account_change_password_new_password">Nueva Contraseña</string>
    <string name="account_change_password_confirm_password">Confirmar Contraseña</string>
    <string name="account_change_password_no_match">Las contraseñas no coinciden.</string>
    <string name="account_change_password_failed">No se pudo cambiar la contraseña. Inténtalo de nuevo.</string>
    <string name="account_category_title_credentials">Credenciales</string>
    <string name="account_category_title_name">Nombre</string>
    <string name="account_category_title_contact_details">Detalles de Contacto</string>
    <string name="account_category_title_personal_details">Detalles Personales</string>
    <string name="account_gender_identity_female">Femenino</string>
    <string name="account_gender_identity_male">Masculino</string>
    <string name="account_gender_identity_non_binary">No Binario</string>
    <string name="account_gender_identity_transgender">Transgénero</string>
    <string name="account_gender_identity_prefer_not_to_state">Prefiero no responder</string>
    <!-- ui-validation -->
    <string name="ui_validation_empty_not_allowed">Este campo no puede estar vacío.</string>
    <string name="ui_validation_password_min_length">Tu contraseña debe tener al menos 8 caracteres.</string>
    <string name="ui_validation_password_med_length">Tu contraseña debe tener al menos 10 caracteres.</string>
    <string name="ui_validation_password_strong_length">Tu contraseña debe tener al menos 12 caracteres.</string>
    <string name="ui_validation_ascii_letters_only">Usa solo letras, números y signos de puntuación comunes</string>
    <string name="ui_validation_minimal_email">Introduce una dirección de correo electrónico con un formato válido.</string>
    <string name="ui_validation_letters_only">El texto solo puede contener letras.</string>
    <!-- questionnaire -->
    <string name="questionnaire_cancel">Cancelar</string>
    <string name="questionnaire_cancel_confirm_description">¿Seguro que quieres cancelar?</string>
    <string name="questionnaire_ok">Aceptar</string>
    <!-- firebase -->
    <string name="firebase_sign_in_anonymous">Continuar como invitado</string>
    <!-- consent -->
    <string name="consent_continue_button">Continuar</string>
    <string name="consent_no_selection">Sin selección</string>
    <string name="consent_signature_section_title">Firma</string>
    <string name="consent_given_name_label">Nombre</string>
    <string name="consent_given_name_placeholder">Escribe tu nombre</string>
    <string name="consent_family_name_label">Apellido</string>
    <string name="consent_family_name_placeholder">Escribe tu apellido</string>
    <string name="consent_error_title">Formulario de consentimiento no disponible</string>
    <string name="consent_error_message">No se pudo cargar el formulario de consentimiento. Inténtalo de nuevo.</string>
    <string name="consent_error_retry">Intentar de nuevo</string>
    <!-- onboarding -->
    <string name="onboarding_next">Siguiente</string>
    <string name="task_completed">Completado</string>
    <!-- scheduler notifications -->
    <string name="scheduler_notifications_channel_name">Recordatorios de tareas</string>
    <string name="scheduler_notifications_channel_description">Recordatorios de tus próximas tareas.</string>
</resources>
```

</details>

### Phase 10: tooling and optional platform work

#### T49: Debug mode and debug menu
**Tags:** `dev-tooling`, `tooling`
**Description:** Enable debug mode from the `enableAppDebugMode` account key (plus a debug-build override and a remembered last value). Add a debug screen with: upload queue and staging DB stats, upload worker log, notification status and FCM token (copyable), a trigger for a test remote nudge (`notificationBacklog`), historical export control and reset, scheduler stats, reset of rejected prompted actions, "answer questionnaire" shortcuts, optional "will/did upload" notifications, and the 30-second walk test. iOS: `Utils/Debug Stuff/*`.
**Acceptance:** Debug features are hidden unless debug mode is on.

#### T50: Test-environment setup for instrumented UI tests
**Tags:** `dev-tooling`, `tooling`
**Description:** Instrumentation arguments equivalent to the iOS launch options: reset data, log in or sign up a test account against the emulator, enroll, prefill demographics and consent version, choose the study bundle source (bundled / Firebase / URL), unit overrides, prompted-action filter. iOS: `SetupTestEnvironment*.swift`, `MyHeartCountsShared/Launch Options/*`.
**Acceptance:** UI tests can start directly in an enrolled state.

#### T51: Demo data and screenshot mode
**Tags:** `dev-tooling`, `tooling`
**Description:** Seed Health Connect and Firestore with demo data (steps, sleep, BP, glucose, BMI, custom scores), show the demo nudge, hide prompted actions; integrate with Fastlane screengrab. iOS: `Utils/Demo/DemoSetup.swift`, `make_screenshots`.
**Acceptance:** Store screenshots can be produced reproducibly.

#### T52: Health data type coverage review
**Tags:** `health-data`, `feature`
**Description:** Map the study definition's HealthKit types to all available Health Connect records. Decide on additions such as menstruation, ovulation, cervical mucus, sexual activity, intermenstrual bleeding, nutrition, hydration, elevation, power and cadence. Document intentionally unsupported types. Once G04 exists, drive the collected types from the study definition instead of the hard-coded list in `MHCHealthConfiguration.kt`.
**Acceptance:** A written mapping table, with the collected types updated.

#### T53: Clinical records via Health Connect (optional)
**Tags:** `health-data`, `platform`, `feature`
**Description:** If D4 is yes, request Health Connect medical-record permissions through a prompted action (21-day window) and upload FHIR resources individually. iOS: `ClinicalRecordPermissions.swift`, `MHCPromptedActions.enableClinicalRecords`. Depends on G05.
**Acceptance:** Consenting participants' medical records reach the backend.

#### T65: Switch release builds back to the production environment
**Tags:** `dev-tooling`, `release`
**Description:** Every build, release builds included, runs as `AppEnvironment.Testing`: the binding in `di/Configurations.kt` is fixed to it so that test builds distributed to devices keep the changeable feature flags (`StoredFeatureFlags`) and the debug sheet opened by shaking the device. Before the actual release to participants, choose the environment by build again: `Production` for release builds (`DefaultFeatureFlags`, no shake detection, no debug sheet) and `Testing` for debug builds, or tie it to the T49 debug mode.
**Acceptance:** A release build has fixed feature flags and never opens the debug sheet on a shake; debug builds keep both.

#### T60: Documentation hygiene ✅ Done
**Tags:** `dev-tooling`, `tooling`
**Description:** Shipped code still carries about 60 references to the other platform, mostly in KDoc (for example `home/FirebaseHomeSuggestionsSource.kt`, `notification/MHCNotificationTracking.kt`, `standard/health/HealthObservationProvenance.kt`), although KDoc should describe each declaration on its own terms. Rewrite them as contract descriptions.
**Acceptance:** A search for `iOS` in `myheartcounts/src/main` finds nothing.
**As built:** All 54 mentions in 20 files under `myheartcounts/src/main` are rewritten as contract descriptions, along with other lineage wording ("Ports …", "counterpart of …", SpeziOnboarding). Comments that only restated a value's origin were dropped. `HealthKit` remains only where it names the shared dataset's contract: the `HK…` sample identifiers that key the shared collections, and the `com.apple.HealthKit` stats source. The same cleanup was applied to the five mentions in the instrumented Firebase tests (`androidTest/.../firebase/`). Detekt and compilation of `main` and `androidTest` pass.

---

### 5.1 Grove Kotlin tickets

These are capabilities MHC-Android depends on that `grove-kotlin` doesn't provide yet (see section
2.0). They are implemented in Grove, merged to Grove's `main`, and picked up by bumping the pin.

#### G01: Health Connect availability and update handling ✅ Done in Grove (#9, pinned)
**Tags:** `grove`, `health-data`
**Description:** Expose the Health Connect SDK status from the `health` module (`HealthConnectClient.getSdkStatus`: available / unavailable / provider update required) as state on `Health`. Add a helper that opens the Play Store listing (or the system update flow) to install or update Health Connect. Make `requestPermissionsIfNeeded` and collection behave predictably when it's unavailable: no crash, a clear state, and a retry once it becomes available. Relevant for Android 10–13, where Health Connect is a separate app, and for devices where it is disabled.
**Needed by:** T10.
**Acceptance:** On a device without Health Connect, the app can detect this, send the participant to install it, and resume permission requests afterwards.
**As built:** `Health` is now an interface (`internal` `HealthImpl`), with `availability: StateFlow<HealthConnectAvailability>` (re-checked when the app comes to the foreground), `refreshAvailability()`, `installOrUpdateHealthConnect()`, and a suspending `requestPermissionsIfNeeded(activity): Boolean` that returns when the screen closes or its activity finishes. Grove switches from its no-op client to a working one once Health Connect becomes available, and requests the history and background permissions only where the installed Health Connect supports them. Grove core gains `application()` next to `appContext()` in DI. The `health { }` DSL receivers are renamed `HealthModuleBuilder` → `HealthConfigurationScope` and `PrivacyConfigBuilder` → `PrivacyConfigurationScope` (now documented); call sites that only use the lambdas, as MHC does, are unaffected. New test fixtures: `FakeHealth` (`health`) and `FakeLocalStorage` (`storage-local`, in-memory), the latter replacing a private stub in `DefaultHealthClientTest`.

#### G02: Aggregate and statistics queries ✅ Done in Grove (#9, pinned)
**Tags:** `grove`, `health-data`
**Description:** Add aggregation to `Health`, based on Health Connect's `aggregate` / `aggregateGroupByDuration` / `aggregateGroupByPeriod`: sum, average, min and max per interval (hour, day, week, custom) over a `HealthQueryTimeRange`, plus a "most recent value" helper, with a data-origin filter. Typed results in the record type's unit.
**Needed by:** T61 (hourly sums and min/max/avg for the stats documents). T41, T43 and T58 read those values back from the stats documents (T62) rather than querying Health Connect themselves.
**Acceptance:** Daily step totals for 7 days and weekly sums can be fetched in one call each, matching Health Connect's own aggregation.
**As built:** `Health.aggregate(metric, timeRange, source)` returns one value and `Health.aggregate(metric, timeRange, interval, source)` a list of `AggregationBucket(start, end, value)`, both taking Health Connect's `AggregateMetric<T>` (for example `StepsRecord.COUNT_TOTAL`, `HeartRateRecord.BPM_AVG`, `ExerciseSessionRecord.EXERCISE_DURATION_TOTAL`), so values are typed in the metric's unit and every metric Health Connect offers is available. `AggregationInterval.Fixed(duration)` (`hourly`) splits into exact durations; `AggregationInterval.Calendar(period)` (`daily`, `weekly`, `monthly`) into local-time periods using the injected `TimeProvider`'s zone, so days across a clock change are 23 or 25 hours. Buckets start at the range start; weekly buckets aren't aligned to a weekday unless the range is. A missing permission (record type, history, background) yields `null` or an empty list; splitting an open-ended range throws. `Health.mostRecent(type, timeRange, source)` returns the newest record. Fixed along the way: `query(sortedBy = BY_START_TIME_DESC, limit = n)` read the *oldest* `n` records and only then sorted them; it now asks Health Connect for descending order. `DefaultHealthClientTest` covers the total, daily buckets across a daylight-saving change, hourly buckets with a source filter, the permission and open-range cases, and the descending read request. Not run against a real Health Connect.

#### G03: Resumable historical bulk export
**Tags:** `grove`, `health-data`
**Description:** A bulk-export API in `health`, the Grove counterpart of the iOS bulk exporter that MHC uses. For a set of record types and a start date, it walks backwards or forwards in time-window batches and hands each batch to a caller-supplied processor. Progress is persisted so sessions resume after process death. It supports concurrency limits, retries of failed batches, pause, and reset or delete of session state. It runs as long-running WorkManager work and needs `READ_HEALTH_DATA_HISTORY` beyond 30 days.
**Needed by:** T11.
**Acceptance:** A 10-year export over 25 record types completes across several process restarts with no gaps or duplicates.

#### G04: Study-driven health data collection ✅ Done in Grove (#9, pinned)
**Tags:** `grove`, `study`, `health-data`
**Description:** Replace the stub `StudyManagerImpl.setupBackgroundHealthDataCollection`. Map the study definition's `healthDataCollection.sampleTypes` / `optionalSampleTypes` (HealthKit identifiers) to Health Connect record types with a documented mapping table, and register or unregister collection when the participant enrolls, unenrolls or the study revision changes. Provide an extension point for the app to override the mapping.
**Needed by:** T52 (replacing MHC's hard-coded list); T08 (new study revisions changing collected types).
**Acceptance:** Enrolling into the MHC bundle starts collection of the mapped types without an app-side list.
**As built:** `Health.updateConfiguration { … }` applies a runtime configuration (same DSL as `health { }`) on top of the registered one; each update replaces the previous. `StudyManager` sends one whenever enrollments or known bundles change: read access to the required types of every known or enrolled bundle (so onboarding can ask before enrolling), background collection for enrolled studies' required types plus optional types already readable. `StudyHealthDataMapping.Default` maps the bundle's `<kind>;<identifier>` sample types onto 21 equivalent Health Connect record types. Health integration is opt-in: `studyManager(health = StudyHealthConfiguration(mapping = …, pollingInterval = …))`, with `StudyHealthConfiguration.DEFAULT` giving the default mapping polled every 15 min. A non-`null` configuration makes `Health` a required dependency (a missing module fails at resolution instead of silently disabling collection); `null`, the default, leaves the manager without it. MHC passes `StudyHealthConfiguration.DEFAULT` in `MyHeartCountsApplication`; it landed with the pin bump to Grove #9. Device-verified with the MHC bundle: 20 of its types are collected (it declares no wheelchair pushes). MHC still configures its own list (T52 removes it, keeping only the 4 Android-only types).
**Open:** a configuration update never changes permissions. A record type it adds is registered but idle until reading it is granted, `isFullyAuthorizedState` turns `false`, and nothing asks the participant: MHC only requests during onboarding, so a study revision adding a type (for example via T08) isn't collected until the app asks again. Needs a re-ask trigger, together with the T10 follow-up for participants onboarded before T10 (prompted action, T39, or a prompt at launch when requirements grew). Also known: removing a type resets its change token, so re-adding it later skips the records written in between; and a collector downgraded from background to foreground-only keeps running in the background (`addHealthDataCollector` ignores the downgrade).

#### G05: Health Connect medical records (FHIR) read API (only if D4 is yes)
**Tags:** `grove`, `health-data`
**Description:** The module already declares `READ_MEDICAL_DATA_*` permissions but has no API. Add medical-resource permission requests and reads (`readMedicalResources`, change tokens) that return FHIR JSON with the data source.
**Needed by:** T53.
**Acceptance:** An app can request medical-record access and receive new and changed FHIR resources.

#### G06: Tap action and branding for scheduler notifications ✅ Done in Grove (#9, pinned)
**Tags:** `grove`, `scheduler`, `notifications`
**Description:** `SchedulerNotificationReceiver` builds reminders without a content intent and with `android.R.drawable.ic_popup_reminder`. Make the configuration accept an intent factory (task ID, occurrence start and thread in its extras) and a small icon, color and channel name/description. Put the scheduled task's identity in the notification extras.
**Needed by:** T54.
**Acceptance:** A host app can make a reminder tap open a specific task and show its own icon.
**As built:** `SchedulerNotificationsConfiguration` gains `smallIcon`, `color`, `channelName` and `channelDescription` (resource IDs; `DEFAULT` keeps the system reminder icon and uses new Grove strings "Task Reminders" / "Reminders for your upcoming tasks."). The channel's name and description are refreshed on every schedule, so a language change reaches them. A tapped reminder opens the app's launch activity, carrying the task ID and occurrence start; `SchedulerNotifications.openedNotification(intent): OpenedTaskNotification?` reads them back. Diverges from the description: no intent factory and no thread in the extras, since the launch activity covers MHC and the task event is enough to find the task. Posting moved from the receiver into `NotificationSchedulerImpl.deliver`, and the alarm now carries the whole planned notification as one JSON extra; alarms armed before this update are dropped when they fire, and the next refresh (on every app start) re-arms them. Also fixed: posting checked the `POST_NOTIFICATIONS` permission, which only exists from Android 13, so reminders were never posted on Android 12 and below; it now checks `areNotificationsEnabled()`, which covers the permission where there is one. Tests: `NotificationPlannerTest` (occurrence start recorded apart from the fire time), `SchedulerNotificationsConfigurationTest`, and `NotificationSchedulerImplTest` (instrumented, 3 pass on API 31 and 36: posts with the configured icon and channel texts, reads the tap extras). The tap intent itself isn't covered: the test APK has no launch activity.

#### G07: Chart and gauge UI components ✅ Done in the app (`dashboard/ui`, #44)
**Tags:** `app`, `ui`, `dashboard`
**Description:** Compose components in `ui` (or a new `ui-charts` module): time-series chart with line, bar and point styles; several series (for example systolic and diastolic); x-axis date stride labelling by range; tap-to-highlight tooltip; "No Data" overlay; a semicircular gauge with a gradient, highlight point, center label and min/max labels; a circular progress ring. These replace what Swift Charts and the MHC `Gauge` provide on iOS.
**Needed by:** T43, T44, T45.
**Acceptance:** Screenshot tests cover each chart type and the gauge.
**As built:** Built in Grove first, then moved into the app, because the components only serve MHC: package `edu.stanford.myheartcounts.dashboard.ui` ([#44](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/pull/44), merged; it also bumps the Grove pin to `cb77df6`). Grove #11, which proposed a Grove `:ui-dashboard` module, is superseded. The package provides the domain-agnostic `CartesianChart` (line/bar/point, multiple series, numeric X/Y pairs with optional X spans, identical explicit range/formatter configuration for both axes, generic highlights and accessibility stepping), `ChartCard`, and `MenuPicker`. `NumberAxisFormat` supports either axis; optional `TimeCoordinates` and `CalendarTimeAxisFormat` adapt instants/calendar labels to either numeric axis. `Range.fitting(values, includeZero)` chooses clean bounds upstream when wanted; zero is not forced into either range. `TileHeader` is shared with `GaugeTile`. Gauges, progress rings and supporting tiles/cards are implemented. Aggregation, metric-specific labels/units/colors and range choices belong to MHC mappers (T43). Validation: 234 JVM tests including 136 screenshot baselines, plus 6 device tests for numeric/time highlights, scrolling, accessibility stepping/reset, picker selection and theme-token re-resolution. TalkBack announcements have not been checked by hand. The `cartesianChart` DSL stores required text on the builder, supplies non-null numeric `0..1` axes by default, and builds the same model with a parameterless internal `build()`. Flat styling methods update an internal `CartesianChartConfig` with `copy`; there is no config parameter on the DSL entry point. Direct construction still accepts a defaulted appearance configuration for colors, typography, dimensions and highlights. Review fixes: bar series document that their Y range should include zero (`Range.fitting(..., includeZero = true)`), and `CalendarTimeAxisFormat` builds its ICU formats once per instance. The package also holds the non-chart dashboard components used by T44, T45 and T58: `Gauge`, `GaugeTile(Grid)`, `GaugeCard`, `GaugeHeader`, `ColorBandList`, `StatTile(Grid)`, `IconTextCard`, `ProgressRing(Row/RowList/Card)`, `TextBadge`, `ProgressBadge`, `LabeledValue`, `CounterCard`, `DashboardSection`, `DashboardSectionList`, `DashboardColorPalette`; Grove's `ui-theme` gained `ColorToken`/`TextStyleToken`/`SpacingToken`/`SizeToken` (Grove #10, pinned). Fix: `ValueScale` keeps searching 1/2/5 steps across successive powers of ten, so values whose outward rounding needs a fourth interval (e.g. 5 to 34.7) no longer crash a chart's value axis with `NoSuchElementException`; regression case in `ValueScaleTest`. Found through the Exercise Minutes detail chart on seeded data.

#### G08: Email verification support in the account service ✅ Done in Grove (#9, pinned)
**Tags:** `grove`, `account`
**Description:** Add `sendEmailVerification()` and `refresh()` / `reload()` (updating `isVerified` in `AccountDetails`) to `AccountService`, implemented in `FirebaseAccountService`. Grove currently only sends the verification email at sign-up and can't refresh the verified flag.
**Needed by:** T38 (and the "Verify Account Email" prompted action in T39).
**Acceptance:** After verifying through the email link, `account.details.isVerified` becomes true without signing out.
**As built:** `AccountService` gains `sendEmailVerification()` and `refresh()`, both failing when nobody is signed in. `FirebaseAccountService` sends Firebase's verification email, and on `refresh()` reloads the Firebase user and supplies a copy of the current details with the values Firebase owns replaced (`isVerified`, `isAnonymousUser`, name, user ID); externally stored values and other flags are kept, so polling causes no Firestore reads. If the account holds no details for that user yet, it falls back to the full load used at sign-in. `InMemoryAccountService` implements both as no-ops. MHC's `MHCAccountService` delegates to the wrapped service, so it needs no change. Compiled and detekt-clean only: not yet exercised against the Firebase emulator or a real project.

#### G09: Update the study manager's preferred locale at runtime ✅ Done in Grove (#9, pinned)
**Tags:** `grove`, `study`
**Description:** `studyManager(preferredLocale)` is fixed when the app is configured. MHC only learns the participant's region (US/UK) mid-onboarding, after `MyHeartCountsApplication` is configured, so a first-run participant keeps the device locale until the app restarts. Add a way to update the preferred locale, then re-resolve localized task titles and instructions and schedule notification texts. Mirror iOS, where the locale comes from the region-specific configuration that is loaded at eligibility.
**Needed by:** T08 and T16 (correct region-specific content in the first session), T48.
**Acceptance:** Choosing the UK at eligibility produces en-GB task titles in the same session.
**As built:** `StudyManager.updatePreferredLocale(locale)` applies the locale at once, so enrollments that follow use it, and re-registers the tasks of current enrollments in the background; their titles and instructions are re-resolved, and scheduled notification texts follow because the scheduler's notifications refresh on every task change. The configured `preferredLocale` stays the starting value. MHC calls it from `MHCFirebaseRegionInitializerImpl` right after persisting the region, with the same region locale that `MyHeartCountsApplication` uses on later launches. `StudyManagerImplTest` covers retitling an enrolled study and enrolling after the update, using a bundle with an en-GB article; `FakeStudyManager` records the last locale. Not checked on a device (first-run onboarding with the UK).

#### G10: Localize Grove strings (Spanish, British English) ✅ Done in Grove (#9, pinned)
**Tags:** `grove`, `localization`
**Description:** The `resources` module is English only. Add `values-es` and `values-en-rGB` for every Grove string MHC surfaces: account login, sign-up and overview, consent, onboarding, scheduler/task UI, health privacy and rationale, questionnaire cancel alert, validation messages.
**Needed by:** T48.
**Acceptance:** With the device in Spanish, no English Grove strings remain in MHC flows.
**As built:** Diverges from the description by decision: a string the app shows belongs to the app, so Grove stays English-only and MHC supplies the translations, overriding each Grove string by name in its own `values-es` (Android's resource merge lets the app win). The Grove part was what blocked that: the login screen's "Please enter a valid …" message was built by concatenating a prefix with the user-ID label, which Spanish word order can't follow. It is now one format string (`account_login_invalid_user_id`, "Please enter a valid %1$s"), and `StringResource` arguments that are themselves `StringResource`s are resolved before formatting. The Spanish for the 87 Grove strings MHC shows is prepared under T48 and lands with it; no British override is needed, since the only Grove string with a British spelling difference (the default Health privacy text) isn't shown by MHC.

#### G11: Questionnaire renderer theme from the Grove palette ✅ Done in Grove (#9, pinned)
**Tags:** `grove`, `questionnaires`, `ui`
**Description:** Android FHIR Data Capture is a View-based Fragment and reads its colours only from an XML theme (`questionnaire_theme` on the host activity theme). Grove's palettes exist only as Kotlin constants (`CardinalRedColorPalette.kt`); `ui-theme` ships just `cardinal_red_background`. Publish each palette's Material 3 roles as XML colours in `ui-theme` (light and night, with a test that they match the Kotlin palette), and have the `questionnaire` module ship a ready renderer theme per palette (parent `Theme.Questionnaire`, all M3 colour roles mapped). Document that the host activity theme needs `viewInflaterClass = MaterialComponentsViewInflater`; otherwise the renderer's buttons inflate as AppCompat buttons and ignore their Material styles.
**Needed by:** T23 follow-up. MHC currently copies the Cardinal Red hex values into `res/values[-night]/questionnaire_colors.xml` and defines `Theme.MyHeartCounts.Questionnaire` itself. With G11 it only points `questionnaire_theme` at Grove's theme and deletes the copy.
**Acceptance:** A host app gets surveys in its Grove palette, light and dark, by setting one theme attribute, with no colour values of its own.
**As built:** `ui-theme` publishes every Material 3 role of both palettes as XML colours (`cardinal_red_<role>`, `accent_blue_<role>`, 36 roles each, in `values` and `values-night`); `ColorPaletteResourcesTest` fails when they drift from the Kotlin palettes. It also declares a `groveColor<Role>` attribute per role, set by each palette's host theme (`Theme.Grove.CardinalRed`, and the new `Theme.Grove.AccentBlue`). `questionnaire` ships a single `Theme.Grove.Questionnaire` (parent `Theme.Questionnaire`) mapping 34 Material Components colour attributes to those `?attr/groveColor…` values; it works because the renderer applies `questionnaire_theme` on top of the host activity's theme, so the palette follows whichever Grove theme the activity uses. Scrim and surface tint have no Material Components attribute. The `viewInflaterClass` requirement is documented next to the theme. MHC now sets `questionnaire_theme` to `Theme.Grove.Questionnaire` and no longer has `questionnaire_colors.xml` or `Theme.MyHeartCounts.Questionnaire`; it landed with the pin bump to Grove #9. Compared with the copy, the renderer additionally receives the tertiary, error-container, inverse, bright and dim roles. `QuestionnaireThemeTest` (instrumented, `questionnaire`) checks the resolved colours for both palettes and passes on API 31; the survey hasn't been checked visually.

#### G12: Encrypted local storage fails on values of 256 bytes or more ✅ Done in Grove (#9, pinned)
**Tags:** `grove`, `storage`, `bug`
**Description:** After consent, `LocalStorage` logs `IllegalBlockSizeException: input must be under 256 bytes`. The size limit suggests a value being encrypted directly with an RSA key instead of through a symmetric (AES-GCM) key wrapped by the keystore key. Find which MHC or Grove write triggers it, check whether the value is lost, and make encrypted storage handle values of any size.
**Needed by:** none directly; any feature persisting a larger encrypted value (consent, account, prompted actions).
**Acceptance:** Encrypted values of several KB round-trip, covered by a test in `storage-local`.
**As built:** The write was Grove's `AccountDetailsCache`, which persists the signed-in account's details with `EncryptedUsingKeyStore`; after consent MHC's account keys push the JSON past the 214 bytes RSA-2048 with OAEP/SHA-1 can encrypt. The failure was only logged and the file left as it was, so after a restart the cache served the last copy that still fit until remote account data replaced it in memory. `LocalStorageImpl` now encrypts each value with a new AES-256-GCM key and only that key with the RSA key pair (both `EncryptedUsingKeyStore` and `Encrypted(keyPair)`), behind a format header; files without the header are still read as before, so existing values stay readable. `LocalStorageTests` (instrumented) round-trip an ~8 KB value with both settings and read a value in the old format; all 11 pass on an emulator.

#### G13: Render Markdown tables ✅ Done in Grove (#10, pinned)
**Tags:** `grove`, `markdown`
**Description:** Grove's `markdown` module has no table block: a GitHub-flavored table (header row, `|---|` delimiter row with optional `:` alignment, body rows) is rendered as paragraphs of raw `|` text. The study's Heart Health explanations use tables in `hhdExplainer/BloodLipids` and `hhdExplainer/BloodPressure` (en-US and es-US), shown in the "About" section of the score detail screen (T45). iOS renders them as tables: SpeziViews' `MarkdownView` (1.12.14) hands each markdown block to MarkdownUI's `Markdown` view (swift-markdown-ui 2.4.1), which parses GitHub-flavored tables with its default `basic` theme: a 1 pt border around every cell in the secondary color, no background, the header row in semibold, cell padding of 0.72 em horizontally and 0.35 em vertically, column alignment from the delimiter row, and cell text wrapping within the available width rather than scrolling. Add a table block to the parser and a Compose rendering matching that.
**Needed by:** T45.
**Acceptance:** The LDL and blood pressure explanations show their tables as on iOS (bordered cells, semibold header, wrapped text); parser and screenshot tests cover alignment and inline emphasis.
**As built:** `MarkdownNode.Table` (columns with header and `Alignment` Leading/Center/Trailing, rows padded or cut to the column count) is parsed ahead of the other node kinds when a line with a pipe is followed by a delimiter row of as many cells; outer pipes are optional, `\|` stays a literal pipe, and the body ends at a blank line, heading or list item. `MarkdownTableView` renders it from `MarkdownNodeView`: a hairline `outline` border around every cell, the header in semibold, cell padding of 0.72 em by 0.35 em of the body font, column alignment, inline emphasis and links. Columns take their widest cell when the table fits; otherwise columns that fit an equal share keep their width and the wider ones share the rest, never below their longest word, with text wrapping. `MarkdownNode.links()` covers table cells. The app's consent PDF writes a table row by row (consent documents have none today). Tests: 9 parser tests (alignments, no outer pipes, escaped pipes, short and long rows, ends of a table, mismatched delimiter, pipes in paragraphs, links in cells) and a screenshot with the study's blood pressure and LDL tables; checked on a device in the LDL and blood pressure explanations of the score detail screen.

#### G14: Shared form and section components (`ui` or a new `ui-form` module)
**Tags:** `grove`, `ui`, `tech-debt`
**Description:** This is not only an app problem: Grove's own UI modules, such as `ui-account`, `ui-scheduler` and `consent`, each recreate the same setup, and the app does it again on top. Every one of them builds its own list-style building blocks on top of `GroveCard` and `ItemDisplayRowComposable`: a section with a title, a card of labelled values separated by dividers, a titled text card, and rows with a switch, a value picker, a date, a text field or navigation. Each copy repeats the card, divider, padding and typography decisions, so they drift apart. The copies in Grove: `ui` (`ItemDisplayRow`, `DescriptionGridRow`), `ui-account` (`AccountOverviewSection`, `AccountOverviewItem`, `AccountSectionTitle`, `AccountValueText`, `StringDataDisplay`, `ValueTextDisplay`, the `BooleanDataEntry`, `ChoicesDataEntry`, `InstantDataEntry` and `StringDataEntry` rows, `SignUpSection`, `LabeledHorizontalDivider`), `ui-scheduler` (`TaskSection`, `TaskListHeader`) and `consent` (`ConsentToggleSection`, `ConsentSelectSection`). In the app: `ui/ToggleSection`, `ui/BooleanOptionRow` and `ui/NavigationCard`; the dashboard's `dashboard/ui/DashboardSection`, `TileHeader` and `IconTextCardRow`; the score entry form's `EntryFieldGroup`, `EntryFieldRow`, `EntryOptionsRow`, `EntryDateRow` and `EntryRowLabel`; the account overview's `AppInfoSection`, `StudyParticipationSection` and `SupportSection`; and the debug sheet's `DebugSectionTitle`, `DebugRowsCard` and `DebugTextCard`. Provide one set in Grove as concretely typed `ComposableContent`s with no domain types: a section (optional title and footer around a list of rows) and row kinds for a labelled value, a switch, a picker, a date, a text field and navigation, each with a preview and a screenshot test. Decide first whether they belong in `ui`, next to `GroveCard` and `ItemDisplayRowComposable`, or in a new `ui-form` module that the other UI modules depend on; then move the Grove modules and the app onto them and delete their copies. Domain-specific parts stay where they are: `ui-account` keeps its account-value wiring and `consent` its consent responses, each built from the shared rows.
**Acceptance:** `ui-account`, `ui-scheduler`, `consent` and the app's dashboard components, account overview, score entry form and debug sheet are built from the shared components, with no module- or app-local section, row or section-title types left; screenshots are unchanged except where a difference was decided.

## 6. Summary by feature group

| Group | ✅ | 🟡 | ❌ | ➖ / 💤 |
|---|---|---|---|---|
| App shell and lifecycle | 8 | 4 | 4 | – |
| Onboarding | 19 | 1 | 3 | 1 💤 |
| Consent lifecycle | – | – | 2 | – |
| Account and settings | 5 | 2 | 9 | 1 ➖ |
| Demographics | 6 | – | 1 | – |
| Home | 6 | 3 | 0 | – |
| Tasks tab | 2 | 4 | 1 | – |
| Questionnaires | 2 | – | 1 | – |
| Active tasks | – | – | 5 | 3 ➖ |
| Heart Health dashboard | 10 | 1 | 1 | – |
| Health data | 5 | 2 | 2 | 3 ➖ |
| Backend, notifications, environment | 4 | 2 | – | – |
| Tooling | – | – | 4 | 1 ➖ |

Counts are feature rows from sections 2.1–2.13, excluding the Android-only row; the "verify" row in 2.7
counts as 🟡. The foundations are solid: Firebase per region, account, eligibility and waitlist, consent
upload, launch routing, withdrawal, the Home and Tasks tabs with missed tasks, questionnaires, push tokens
and foreground nudges, and the Health Connect permission request with the collection and upload pipeline, most of it built on
Grove. The largest gaps are re-asking participants who onboarded before the permission fix, the Heart Health
dashboard, active tasks, the account sheet extras, and the remote study bundle and consent
lifecycle.
