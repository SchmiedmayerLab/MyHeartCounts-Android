//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.stanford.spezi.resources.Strings
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziShapes
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.bold
import kotlinx.coroutines.launch

/**
 * Presents a sequence of onboarding steps that progressively reveal as the user advances.
 *
 * @property header Title area shown above the revealed steps.
 * @property steps Ordered steps in the sequence.
 * @property actionText Label for the final action.
 * @property initialStepIndex First step revealed when the layout is displayed.
 * @property action Work performed after the final step is reached.
 */
class SequentialOnboardingLayout(
    val header: OnboardingTitle,
    val steps: List<Step>,
    val actionText: StringResource,
    val initialStepIndex: Int = 0,
    val action: suspend () -> Unit,
) : ComposableContent {

    /**
     * Describes one progressively revealed onboarding step.
     *
     * @property title Optional headline for the step.
     * @property description Body text for the step.
     */
    data class Step(
        val title: StringResource? = null,
        val description: StringResource,
    )

    /**
     * Creates a sequential page with a text title and optional subtitle.
     *
     * @param title Title shown above the revealed steps.
     * @param subtitle Optional supporting text shown below [title].
     * @param steps Ordered steps in the sequence.
     * @param actionText Label for the final action.
     * @param initialStepIndex First step revealed when the layout is displayed.
     * @param action Work performed after the final step is reached.
     */
    constructor(
        title: StringResource,
        subtitle: StringResource? = null,
        steps: List<Step>,
        actionText: StringResource,
        initialStepIndex: Int = 0,
        action: suspend () -> Unit,
    ) : this(
        header = OnboardingTitle(title = title, subtitle = subtitle),
        initialStepIndex = initialStepIndex,
        steps = steps,
        actionText = actionText,
        action = action,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var currentStepIndex by rememberSaveable(initialStepIndex) { mutableIntStateOf(initialStepIndex) }
        var isLoading by remember { mutableStateOf(false) }
        val isLastStep = currentStepIndex == steps.lastIndex
        val scope = rememberCoroutineScope()

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            ) {
                header.Content()
                Column(verticalArrangement = Arrangement.spacedBy(Spacings.medium)) {
                    steps.take(currentStepIndex + 1).forEachIndexed { index, step ->
                        StepRow(index = index, step = step)
                    }
                }
            }
            AsyncTextButton(
                text = if (isLastStep) actionText.text() else stringResource(Strings.onboarding_next),
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoading,
                onClick = {
                    if (!isLastStep) {
                        currentStepIndex++
                    } else {
                        isLoading = true
                        scope.launch {
                            action()
                            isLoading = false
                        }
                    }
                },
            )
        }
    }

    @Composable
    private fun StepRow(index: Int, step: Step) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Colors.surfaceContainerLowest,
                    shape = SpeziShapes.large,
                )
                .padding(horizontal = Spacings.medium, vertical = Spacings.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = Colors.primary, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    color = Colors.onPrimary,
                    style = TextStyles.titleSmall.bold(),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
            ) {
                step.title?.let { title ->
                    Text(
                        text = title.text(),
                        style = TextStyles.titleSmall.bold(),
                    )
                }
                Text(
                    text = step.description.text(),
                    style = TextStyles.bodyMedium,
                    color = Colors.onSurfaceVariant,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    SpeziTheme {
        SequentialOnboardingLayout(
            title = StringResource("How It Works"),
            subtitle = StringResource("Follow these steps to get started."),
            steps = listOf(
                SequentialOnboardingLayout.Step(
                    title = StringResource("Create your account"),
                    description = StringResource("Sign up with your email to join the study."),
                ),
                SequentialOnboardingLayout.Step(
                    title = StringResource("Complete consent"),
                    description = StringResource("Review and sign the informed consent form."),
                ),
                SequentialOnboardingLayout.Step(
                    title = StringResource("Start contributing"),
                    description = StringResource("Your data helps advance heart health research worldwide."),
                ),
            ),
            actionText = StringResource("Get Started"),
            action = {},
        ).Content()
    }
}
