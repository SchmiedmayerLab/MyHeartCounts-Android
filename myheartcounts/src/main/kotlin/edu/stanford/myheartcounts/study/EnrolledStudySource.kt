//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.grovealliance.study.StudyManager
import java.time.Instant

/**
 * The study the participant is enrolled in, described for display.
 */
interface EnrolledStudySource {

    /**
     * The participant's enrollment, re-emitting when it or the study variant changes; null while not
     * enrolled.
     */
    fun enrolledStudy(): Flow<EnrolledStudy?>
}

/**
 * An enrollment with the study's description in the participant's content language.
 *
 * @property title the study's title, or null when the study's description is unavailable
 * @property shortExplanation a one-line explanation of the study, or null when unavailable
 * @property enrollmentDate when the participant enrolled
 */
data class EnrolledStudy(
    val title: String?,
    val shortExplanation: String?,
    val enrollmentDate: Instant,
)

/**
 * Default [EnrolledStudySource] implementation, describing the enrollment with the loaded study
 * bundle's metadata when the bundle describes the enrolled study.
 */
class EnrolledStudySourceImpl(
    private val studyManager: StudyManager,
    private val studyBundleProvider: MHCStudyBundleProvider,
    private val variantProvider: StudyVariantProvider,
) : EnrolledStudySource {

    override fun enrolledStudy(): Flow<EnrolledStudy?> =
        combine(studyManager.enrollments, variantProvider.variant) { enrollments, _ ->
            val enrollment = enrollments.firstOrNull() ?: return@combine null
            val metadata = studyBundleProvider.get().getOrNull()
                ?.studyDefinition
                ?.takeIf { it.id == enrollment.studyId }
                ?.metadata
            val locale = variantProvider.contentLocale()
            EnrolledStudy(
                title = metadata?.title?.resolve(locale = locale),
                shortExplanation = metadata?.shortExplanationText?.resolve(locale = locale),
                enrollmentDate = enrollment.enrollmentDate,
            )
        }.distinctUntilChanged()
}
