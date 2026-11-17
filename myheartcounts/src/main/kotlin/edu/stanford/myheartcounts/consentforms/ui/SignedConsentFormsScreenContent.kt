//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.consentforms.SignedConsentFormsScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSection
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The signed consent forms screen: the layout of its current [state] beneath its app bar.
 */
data class SignedConsentFormsScreenContent(
    val scaffoldState: GroveScaffoldState,
    val state: StateFlow<SignedConsentFormsScreenState>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val state by state.collectAsStateWithLifecycle()
        GroveScaffold(state = scaffoldState) {
            state.layout.Content(modifier = modifier.fillMaxSize())
        }
    }
}

private class SignedConsentFormsScreenContentPreviewProvider :
    PreviewParameterProvider<SignedConsentFormsScreenContent> {
    private val row = CardRow(
        key = "1767258000.pdf",
        content = SignedConsentFormRow(
            version = StringResource("1.0.0"),
            title = StringResource("My Heart Counts Consent Form"),
            signedAt = StringResource("Jan 1, 2026, 9:00 AM"),
            isLoading = false,
            isEnabled = true,
            onClick = {},
        ),
    )
    private val base = SignedConsentFormsScreenContent(
        scaffoldState = GroveScaffoldState(
            appBar = groveAppBar {
                back {}
                title(StringResource("Consent Documents"))
            },
        ),
        state = MutableStateFlow(
            SignedConsentFormsScreenState.Content(
                layout = SignedConsentFormsLayout(
                    list = SignedConsentFormListLayout(
                        section = MutableStateFlow(groveSection(key = "forms") { card(key = "forms") { row(row = row) } }),
                        empty = StringResource("You haven't signed any consent forms yet."),
                        isRefreshing = MutableStateFlow(false),
                        onRefresh = {},
                    ),
                    document = MutableStateFlow(null),
                    onBack = {},
                ),
            ),
        ),
    )
    override val values = sequenceOf(
        base,
        base.copy(state = MutableStateFlow(SignedConsentFormsScreenState.Loading)),
        base.copy(
            state = MutableStateFlow(
                SignedConsentFormsScreenState.Error(
                    layout = GroveErrorLayout(
                        title = StringResource("Consent Forms Unavailable"),
                        message = StringResource("Your consent forms couldn't be loaded. Please check your connection and try again."),
                        primaryButton = AsyncTextButton(
                            title = StringResource("Try Again"),
                            action = {},
                        ),
                    ),
                ),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(SignedConsentFormsScreenContentPreviewProvider::class) content: SignedConsentFormsScreenContent,
) {
    GroveTheme { content.Content() }
}
