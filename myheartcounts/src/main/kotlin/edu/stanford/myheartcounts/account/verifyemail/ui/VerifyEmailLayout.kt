//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.ExternalLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold

/**
 * The email verification page: the [icon], [title] and [explanation] above the [body] for the
 * participant's current verification state.
 */
data class VerifyEmailLayout(
    val icon: ImageResource,
    val title: StringResource,
    val explanation: StringResource,
    val body: StateFlow<Body>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val body by body.collectAsStateWithLifecycle()
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            icon.Content(
                modifier = Modifier
                    .padding(vertical = Spacings.large)
                    .size(Sizes.Icon.large)
                    .align(Alignment.CenterHorizontally),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacings.small)) {
                Text(
                    text = title.text(),
                    style = TextStyles.headlineSmall.bold(),
                    color = Colors.onSurface,
                )
                Text(
                    text = explanation.text(),
                    style = TextStyles.bodyLarge,
                    color = Colors.onSurfaceVariant,
                )
            }
            when (val shown = body) {
                is Body.Unavailable -> Text(
                    text = shown.message.text(),
                    style = TextStyles.bodyMedium,
                    color = Colors.onSurfaceVariant,
                )
                is Body.Available -> {
                    shown.status.Content(modifier = Modifier.fillMaxWidth())
                    shown.stepsTitle?.let {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacings.medium),
                            text = it.text(),
                            style = TextStyles.titleMedium.bold(),
                            color = Colors.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                    shown.steps.forEach { it.Content(modifier = Modifier.fillMaxWidth()) }
                }
            }
        }
    }

    /**
     * What the page shows below its explanation.
     */
    sealed interface Body {

        /**
         * The account can't be read; [message] says so.
         */
        data class Unavailable(val message: StringResource) : Body

        /**
         * The verification [status], followed by the [steps] to verify under [stepsTitle] while
         * the email is not verified.
         */
        data class Available(
            val status: VerificationStatusCard,
            val stepsTitle: StringResource?,
            val steps: List<VerificationStepRow>,
        ) : Body
    }
}

private class VerifyEmailLayoutPreviewProvider : PreviewParameterProvider<VerifyEmailLayout> {
    private val step = VerificationStepRow(
        title = StringResource("1. Request a verification link"),
        explanation = StringResource("We'll email a link to participant@example.com"),
        emphasis = "participant@example.com",
        action = AsyncTextButton(
            title = StringResource("Send Verification Link"),
            action = {},
        ),
    )
    private val base = VerifyEmailLayout(
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
                steps = listOf(
                    step,
                    step.copy(
                        title = StringResource("2. Open the link in the email"),
                        explanation = StringResource("Tap the link in the email to complete the verification, then return here."),
                        emphasis = null,
                        action = ExternalLink(
                            text = StringResource("Open Email App"),
                            icon = ImageResource(image = Icons.Outlined.Email),
                            onClicked = {},
                        ),
                    ),
                ),
            ),
        ),
    )
    override val values = sequenceOf(
        base,
        base.copy(
            body = MutableStateFlow(
                VerifyEmailLayout.Body.Available(
                    status = VerificationStatusCard(
                        icon = ImageResource(image = Icons.Default.VerifiedUser),
                        text = StringResource("Your email address is verified"),
                    ),
                    stepsTitle = null,
                    steps = emptyList(),
                ),
            ),
        ),
        base.copy(
            body = MutableStateFlow(
                VerifyEmailLayout.Body.Unavailable(
                    message = StringResource("Unable to connect to account. Make sure you have a working network connection."),
                ),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(VerifyEmailLayoutPreviewProvider::class) content: VerifyEmailLayout) {
    GroveTheme { content.Content() }
}
