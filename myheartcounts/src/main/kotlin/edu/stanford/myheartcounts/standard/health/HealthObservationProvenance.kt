//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType
import java.util.TimeZone

/**
 * The provenance every uploaded observation carries.
 *
 * These say who produced a sample and under what version of the study, which is what lets the
 * dataset be re-analyzed after the study or the app changes. The URLs are a cross-platform contract
 * with iOS's `defaultHealthObservationFHIRExtensions`.
 *
 * @param appRevision The app's version name, recorded as `mhcAppRevision`.
 * @param studyId The identifier of the study the participant is enrolled in, when they are.
 * @param studyRevision The revision of that study.
 * @param platform Which platform produced the sample, so Health Connect and HealthKit data can be
 * told apart after the fact — they share collections by design, and nothing else in the resource
 * distinguishes them.
 */
data class HealthObservationProvenance(
    val appRevision: String,
    val studyId: String?,
    val studyRevision: Int?,
    val platform: String = ANDROID_PLATFORM,
) {

    /**
     * Stamps [observation] with this provenance, replacing any extension it already carries under
     * the same URL.
     */
    fun applyTo(observation: Observation) {
        observation.replaceExtension(
            url = SAMPLE_UPLOAD_TIME_ZONE_URL,
            extension = Extension(SAMPLE_UPLOAD_TIME_ZONE_URL, StringType(TimeZone.getDefault().id)),
        )
        observation.replaceExtension(
            url = APP_REVISION_URL,
            extension = Extension(APP_REVISION_URL, StringType(appRevision)),
        )
        observation.replaceExtension(
            url = PLATFORM_URL,
            extension = Extension(PLATFORM_URL, StringType(platform)),
        )

        if (studyId == null || studyRevision == null) {
            observation.removeExtension(url = STUDY_ENROLLMENT_URL)
            return
        }
        observation.replaceExtension(
            url = STUDY_ENROLLMENT_URL,
            extension = Extension(STUDY_ENROLLMENT_URL).apply {
                addExtension(Extension("$STUDY_ENROLLMENT_URL/study-id", StringType(studyId)))
                addExtension(Extension("$STUDY_ENROLLMENT_URL/study-revision", IntegerType(studyRevision)))
            },
        )
    }

    private fun Observation.replaceExtension(url: String, extension: Extension) {
        removeExtension(url = url)
        addExtension(extension)
    }

    private fun Observation.removeExtension(url: String) {
        extension.removeAll { it.url == url }
    }

    companion object {
        /**
         * The value of the platform extension for this app.
         */
        const val ANDROID_PLATFORM = "android-health-connect"

        /**
         * The participant's time zone at upload time. Shared with iOS.
         */
        const val SAMPLE_UPLOAD_TIME_ZONE_URL = "https://bdh.stanford.edu/fhir/defs/sampleUploadTimeZone"

        /**
         * The app version that uploaded the sample. Shared with iOS.
         */
        const val APP_REVISION_URL = "https://bdh.stanford.edu/fhir/defs/mhcAppRevision"

        /**
         * The study and revision the participant was enrolled in. Shared with iOS, including the
         * `study-id` and `study-revision` sub-extensions.
         */
        const val STUDY_ENROLLMENT_URL = "https://myheartcounts.stanford.edu/fhir/StructureDefinition/study-enrollment"

        /**
         * Which platform produced the sample. Android-only: iOS records provenance through
         * HealthKit's own source metadata, which Health Connect has no equivalent of.
         */
        const val PLATFORM_URL = "https://myheartcounts.stanford.edu/fhir/StructureDefinition/source-platform"
    }
}
