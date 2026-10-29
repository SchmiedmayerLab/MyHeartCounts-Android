//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import androidx.annotation.StringRes
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.serialization.Serializable

/**
 * A factor contributing to the participant's heart health score.
 *
 * @param title the name of the factor, as shown to the participant
 */
@Serializable
enum class HeartScoreComponent(@StringRes val title: Int) {
    ExerciseMinutes(title = MHCStrings.heart_score_exercise_minutes),
    StepCount(title = MHCStrings.heart_score_step_count),
    Sleep(title = MHCStrings.heart_score_sleep),
    Diet(title = MHCStrings.heart_score_diet),
    MentalWellbeing(title = MHCStrings.heart_score_mental_wellbeing),
    BloodPressure(title = MHCStrings.heart_score_blood_pressure),
    BloodLipids(title = MHCStrings.heart_score_blood_lipids),
    BloodGlucose(title = MHCStrings.heart_score_blood_glucose),
    BodyMassIndex(title = MHCStrings.heart_score_body_mass_index),
    NicotineExposure(title = MHCStrings.heart_score_nicotine_exposure),
}
