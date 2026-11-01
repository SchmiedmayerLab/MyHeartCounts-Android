//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import edu.stanford.myheartcounts.study.StudyVariantProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.studydefinition.FileReference

/**
 * Reads the study's explanation of each factor of the heart health score.
 */
interface HeartScoreExplainerSource {

    /**
     * The explanation of [component] in the study's content locale, or `null` when the study provides
     * none or it cannot be read.
     */
    suspend fun document(component: HeartScoreComponent): MarkdownDocument?
}

/**
 * Reads the explanations from the study bundle's `hhdExplainer` files.
 */
class HeartScoreExplainerSourceImpl(
    private val studyBundleProvider: MHCStudyBundleProvider,
    private val variantProvider: StudyVariantProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : HeartScoreExplainerSource {
    private val logger by groveLogger()

    override suspend fun document(component: HeartScoreComponent): MarkdownDocument? = runCatchingAsync {
        val filename = when (component) {
            HeartScoreComponent.ExerciseMinutes -> "ExerciseMinutes"
            HeartScoreComponent.StepCount -> "StepCount"
            HeartScoreComponent.Sleep -> "Sleep"
            HeartScoreComponent.Diet -> "DietScore"
            HeartScoreComponent.MentalWellbeing -> "MentalWellbeing"
            HeartScoreComponent.BloodPressure -> "BloodPressure"
            HeartScoreComponent.BloodLipids -> "BloodLipids"
            HeartScoreComponent.BloodGlucose -> "BloodGlucose"
            HeartScoreComponent.BodyMassIndex -> "BMI"
            HeartScoreComponent.NicotineExposure -> "NicotineExposure"
        }
        val bundle = studyBundleProvider.get().getOrThrow()
        withContext(ioDispatcher) {
            bundle.resolve(
                fileRef = FileReference(
                    category = FileReference.Category(rawValue = "hhdExplainer"),
                    filename = filename,
                    fileExtension = "md",
                ),
                locale = variantProvider.contentLocale(),
            )?.takeIf { it.exists() }?.readText()
        }?.let { MarkdownDocument.process(text = it) }
    }.onFailure { logger.w(it) { "The component explanation is unavailable" } }.getOrNull()
}
