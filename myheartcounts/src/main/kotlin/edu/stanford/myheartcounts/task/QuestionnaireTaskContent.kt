//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.questionnaire.CancelBehavior
import org.grovealliance.questionnaire.QuestionnaireComposable
import org.grovealliance.questionnaire.QuestionnaireResult
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.hl7.fhir.r4.model.Questionnaire

/**
 * A full-screen [questionnaire] for the participant to answer, reporting how it ended through
 * [onResult]. The questionnaire offers no cancel button of its own; leaving it is up to the app bar
 * of [scaffoldState] and the system back action.
 */
data class QuestionnaireTaskContent(
    val scaffoldState: GroveScaffoldState,
    val questionnaire: Questionnaire,
    val onResult: (QuestionnaireResult) -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveScaffold(state = scaffoldState) {
            QuestionnaireComposable(
                questionnaire = questionnaire,
                modifier = modifier,
                cancelBehavior = CancelBehavior.Disabled,
                onResult = onResult,
            )
        }
    }
}
