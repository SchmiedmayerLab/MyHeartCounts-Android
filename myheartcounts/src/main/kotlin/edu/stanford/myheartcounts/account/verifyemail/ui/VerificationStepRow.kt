//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.ui.ExternalLink
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * One step towards verifying the email address: its [title] and [explanation], in which
 * [emphasis], if it appears, is drawn in the accent color, followed by the [action] taking the step,
 * if the step offers one.
 */
data class VerificationStepRow(
    val title: StringResource,
    val explanation: StringResource,
    val emphasis: String?,
    val action: ComposableContent?,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall)) {
                Text(
                    text = title.text(),
                    style = TextStyles.bodyLarge,
                    color = Colors.onSurface,
                )
                val explanation = explanation.text()
                val accent = Colors.primary
                Text(
                    text = buildAnnotatedString {
                        append(explanation)
                        val start = emphasis?.let { explanation.indexOf(it) } ?: -1
                        if (emphasis != null && start >= 0) {
                            addStyle(
                                style = SpanStyle(color = accent),
                                start = start,
                                end = start + emphasis.length,
                            )
                        }
                    },
                    style = TextStyles.bodySmall,
                    color = Colors.onSurfaceVariant,
                )
            }
            action?.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}

private class VerificationStepRowPreviewProvider : PreviewParameterProvider<VerificationStepRow> {
    private val base = VerificationStepRow(
        title = StringResource("1. Request a verification link"),
        explanation = StringResource("We'll email a link to participant@example.com"),
        emphasis = "participant@example.com",
        action = AsyncTextButton(
            title = StringResource("Send Verification Link"),
            action = {},
        ),
    )
    override val values = sequenceOf(
        base,
        base.copy(
            title = StringResource("2. Open the link in the email"),
            explanation = StringResource("Tap the link in the email to complete the verification, then return here."),
            emphasis = null,
            action = ExternalLink(
                text = StringResource("Open Email App"),
                icon = ImageResource(image = Icons.Outlined.Email),
                onClicked = {},
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(VerificationStepRowPreviewProvider::class) content: VerificationStepRow) {
    GroveTheme { content.Content(modifier = Modifier.padding(Spacings.medium)) }
}
