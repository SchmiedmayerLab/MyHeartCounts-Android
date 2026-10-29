//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A bottom sheet where the participant writes feedback for the study team, beneath [appBar].
 *
 * The sheet shows [message] and reports edits through [onMessageChanged]. It can be swiped or
 * tapped away only while [isDismissible] returns `true`, so an unsent message isn't lost by
 * accident.
 */
data class FeedbackSheet(
    val appBar: StateFlow<GroveAppBar>,
    val message: StateFlow<String>,
    val onMessageChanged: (String) -> Unit,
    val isDismissible: () -> Boolean,
) : BottomSheetComposableContent {

    override val skipPartiallyExpanded: Boolean = true

    override val onConfirmSheetState: (BottomSheetComposableContent.State) -> Boolean = { state ->
        state != BottomSheetComposableContent.State.Hidden || isDismissible()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val appBar by appBar.collectAsStateWithLifecycle()
        val message by message.collectAsStateWithLifecycle()
        val focusRequester = remember { FocusRequester() }

        StaticGroveScaffold(appBar = appBar) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = message,
                    onValueChange = onMessageChanged,
                    minLines = MIN_LINES,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Colors.surfaceContainerLowest,
                        unfocusedContainerColor = Colors.surfaceContainerLowest,
                    ),
                )
                Text(
                    text = stringResource(MHCStrings.account_feedback_footer),
                    style = TextStyles.bodySmall,
                    color = Colors.onSurfaceVariant,
                )
            }
        }

        LaunchedEffect(Unit) {
            if (message.isEmpty()) focusRequester.requestFocus()
        }
    }

    private companion object {
        const val MIN_LINES = 6
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        FeedbackSheet(
            appBar = MutableStateFlow(groveAppBar { title(StringResource("Feedback")) }),
            message = MutableStateFlow("The new dashboard is great!"),
            onMessageChanged = {},
            isDismissible = { true },
        ).Content(modifier = Modifier)
    }
}
