//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.alpha
import org.grovealliance.ui.noRippleClickable
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold

/**
 * A signed form: its [title] under its [version], and when it was [signedAt] at the end. Shows
 * progress while [isLoading], and can be tapped only while [isEnabled].
 *
 * @param version the version of the document signed, if known
 * @param title the title of the document signed
 * @param signedAt when the form was signed, already formatted, if known
 * @param isLoading whether the form is being opened
 * @param isEnabled whether the form can be opened
 * @param onClick called when the row is tapped
 */
data class SignedConsentFormRow(
    val version: StringResource?,
    val title: StringResource,
    val signedAt: StringResource?,
    val isLoading: Boolean,
    val isEnabled: Boolean,
    val onClick: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isEnabled) Modifier.noRippleClickable(onClick = onClick) else Modifier)
                .alpha(disabled = !isEnabled && !isLoading)
                .padding(Spacings.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacings.tiny),
            ) {
                version?.let {
                    Text(
                        text = it.text(),
                        style = TextStyles.bodySmall,
                        color = Colors.onSurfaceVariant,
                    )
                }
                Text(
                    text = title.text(),
                    style = TextStyles.bodyLarge.bold(),
                    color = Colors.onSurface,
                )
            }
            Box(contentAlignment = Alignment.CenterEnd) {
                signedAt?.let {
                    Text(
                        modifier = Modifier.alpha(if (isLoading) 0f else 1f),
                        text = it.text(),
                        style = TextStyles.bodyMedium,
                        color = Colors.onSurfaceVariant,
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(Sizes.Icon.small))
                }
            }
        }
    }
}

private class SignedConsentFormRowPreviewProvider : PreviewParameterProvider<SignedConsentFormRow> {
    private val base = SignedConsentFormRow(
        version = StringResource("1.0.0"),
        title = StringResource("My Heart Counts Consent Form"),
        signedAt = StringResource("Jan 1, 2026, 9:00 AM"),
        isLoading = false,
        isEnabled = true,
        onClick = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(isLoading = true),
        base.copy(isEnabled = false),
        base.copy(
            version = null,
            signedAt = null,
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(SignedConsentFormRowPreviewProvider::class) content: SignedConsentFormRow) {
    GroveTheme { content.Content() }
}
