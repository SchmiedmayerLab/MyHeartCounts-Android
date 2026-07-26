//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding.eligibility

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.onboarding.OnboardingLink
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.SpeziInputFieldComposable
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import kotlinx.coroutines.flow.Flow

/**
 * The content shown below an ineligibility or country-unavailability title: an optional [email] field
 * and [actionButton] (used for the launch waitlist) followed by a [link] out to the study.
 */
data class IneligibleContent(
    val email: EmailField?,
    val actionButton: AsyncTextButton?,
    val link: OnboardingLink,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            email?.Content(modifier = Modifier.fillMaxWidth())
            actionButton?.Content(modifier = Modifier.fillMaxWidth())
            link.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * A single-line email entry field whose current text is driven by [value] and whose edits are
 * reported through [onValueChange].
 */
data class EmailField(
    val value: Flow<String>,
    val placeholder: StringResource,
    val onValueChange: (String) -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val text by value.collectAsStateWithLifecycle(initialValue = "")
        SpeziInputFieldComposable(
            modifier = modifier.fillMaxWidth(),
            value = text,
            placeholder = placeholder.text(),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onValueChanged = onValueChange,
        )
    }
}
