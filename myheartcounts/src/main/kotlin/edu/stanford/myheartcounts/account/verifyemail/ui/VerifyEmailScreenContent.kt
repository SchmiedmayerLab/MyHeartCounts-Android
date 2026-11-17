//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The email verification [layout] beneath its app bar.
 */
data class VerifyEmailScreenContent(
    val scaffoldState: GroveScaffoldState,
    val layout: VerifyEmailLayout,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveScaffold(state = scaffoldState) {
            layout.Content(modifier = modifier)
        }
    }
}

private class VerifyEmailScreenContentPreviewProvider : PreviewParameterProvider<VerifyEmailScreenContent> {
    private val step = VerificationStepRow(
        title = StringResource("1. Request a verification link"),
        explanation = StringResource("We'll email a link to participant@example.com"),
        emphasis = "participant@example.com",
        action = AsyncTextButton(
            title = StringResource("Send Verification Link"),
            action = {},
        ),
    )
    private val base = VerifyEmailScreenContent(
        scaffoldState = GroveScaffoldState(appBar = groveAppBar { close {} }),
        layout = VerifyEmailLayout(
            icon = ImageResource(image = Icons.Outlined.VerifiedUser),
            title = StringResource("Verify Email"),
            explanation = StringResource("A verified email lets us confirm it's really you."),
            body = MutableStateFlow(
                VerifyEmailLayout.Body.Available(
                    status = VerificationStatusCard(
                        icon = ImageResource(image = Icons.Default.GppMaybe),
                        text = StringResource("Your email currently is not verified"),
                    ),
                    stepsTitle = StringResource("How to verify"),
                    steps = listOf(step),
                ),
            ),
        ),
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(VerifyEmailScreenContentPreviewProvider::class) content: VerifyEmailScreenContent,
) {
    GroveTheme { content.Content() }
}
