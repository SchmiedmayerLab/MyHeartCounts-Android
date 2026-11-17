//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveSection
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSection
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The signed forms in [section], or the [empty] message while it is empty, read again by
 * [onRefresh] when the list is pulled down, while [isRefreshing].
 */
data class SignedConsentFormListLayout(
    val section: StateFlow<GroveSection>,
    val empty: StringResource,
    val isRefreshing: StateFlow<Boolean>,
    val onRefresh: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val section by section.collectAsStateWithLifecycle()
        val sectionItems = section.items()
        val isRefreshing by isRefreshing.collectAsStateWithLifecycle()
        val empty = empty.text()
        PullToRefreshBox(
            modifier = modifier,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacings.medium),
            ) {
                if (section.isEmpty) {
                    item(key = "empty") {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacings.large),
                            text = empty,
                            style = TextStyles.bodyMedium,
                            color = Colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    sectionItems()
                }
            }
        }
    }
}

private class SignedConsentFormListLayoutPreviewProvider : PreviewParameterProvider<SignedConsentFormListLayout> {
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
    private val base = SignedConsentFormListLayout(
        section = MutableStateFlow(
            groveSection(key = "forms") {
                card(key = "forms") {
                    row(row = row)
                    row(
                        row = row.copy(
                            key = "1767171600.pdf",
                            content = row.content.copy(
                                version = StringResource("0.9.0"),
                                signedAt = StringResource("Dec 31, 2025, 9:00 AM"),
                            ),
                        ),
                    )
                }
            },
        ),
        empty = StringResource("You haven't signed any consent forms yet."),
        isRefreshing = MutableStateFlow(false),
        onRefresh = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(section = MutableStateFlow(groveSection(key = "forms") {})),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(SignedConsentFormListLayoutPreviewProvider::class) content: SignedConsentFormListLayout,
) {
    GroveTheme { content.Content() }
}
