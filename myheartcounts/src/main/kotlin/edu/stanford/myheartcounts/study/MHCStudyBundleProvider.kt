//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import edu.stanford.spezi.core.Module
import edu.stanford.spezi.studydefinition.StudyBundle

/**
 * The study bundle's directory within the app's assets.
 */
const val STUDY_BUNDLE_ASSET_PATH = "mhcStudyBundle.spezistudybundle"

/**
 * Provides the study bundle describing the study a participant takes part in.
 *
 * The bundle is loaded when the app is configured and handed to the study manager, which carries an
 * existing enrollment forward when the bundle describes a newer revision of the study. This never
 * creates an enrollment; that happens once, when the participant completes onboarding.
 */
interface MHCStudyBundleProvider : Module {

    /**
     * The study bundle, loaded on first use and kept for later calls.
     *
     * Concurrent calls share a single load; a failed load is not remembered, so a later call
     * attempts it again.
     */
    suspend fun get(): Result<StudyBundle>
}
