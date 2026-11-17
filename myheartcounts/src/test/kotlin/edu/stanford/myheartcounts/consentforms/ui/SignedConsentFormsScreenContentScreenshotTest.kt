//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms.ui

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.consentforms.SignedConsentFormsScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSection
import org.junit.Test

class SignedConsentFormsScreenContentScreenshotTest : ScreenshotTest() {
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
    private val rows = listOf(
        row,
        row.copy(
            key = "1735722000.pdf",
            content = row.content.copy(
                version = StringResource("0.9.0"),
                signedAt = StringResource("Jan 1, 2025, 9:00 AM"),
            ),
        ),
        row.copy(
            key = "1704099600.pdf",
            content = row.content.copy(
                version = null,
                title = StringResource(MHCStrings.consent_forms_untitled),
                signedAt = null,
            ),
        ),
    )
    private val list = SignedConsentFormListLayout(
        section = MutableStateFlow(section(rows = rows)),
        empty = StringResource(MHCStrings.consent_forms_empty),
        isRefreshing = MutableStateFlow(false),
        onRefresh = {},
    )
    private val base = SignedConsentFormsScreenContent(
        scaffoldState = GroveScaffoldState(
            appBar = groveAppBar {
                back {}
                title(MHCStrings.consent_forms_title)
            },
        ),
        state = MutableStateFlow(
            SignedConsentFormsScreenState.Content(
                layout = SignedConsentFormsLayout(
                    list = list,
                    document = MutableStateFlow(null),
                    onBack = {},
                ),
            ),
        ),
    )

    @Test
    fun `SignedConsentFormsScreenContent list`() {
        // when / then
        screenshot { base.Content() }
    }

    @Test
    fun `SignedConsentFormsScreenContent opening a form`() {
        // given
        val content = base.copy(
            state = MutableStateFlow(
                SignedConsentFormsScreenState.Content(
                    layout = SignedConsentFormsLayout(
                        list = list.copy(
                            section = MutableStateFlow(
                                section(
                                    rows = rows.mapIndexed { index, it ->
                                        it.copy(
                                            content = it.content.copy(
                                                isLoading = index == 0,
                                                isEnabled = false,
                                            ),
                                        )
                                    },
                                ),
                            ),
                        ),
                        document = MutableStateFlow(null),
                        onBack = {},
                    ),
                ),
            ),
        )

        // when / then
        screenshot { content.Content() }
    }

    @Test
    fun `SignedConsentFormsScreenContent empty`() {
        // given
        val content = base.copy(
            state = MutableStateFlow(
                SignedConsentFormsScreenState.Content(
                    layout = SignedConsentFormsLayout(
                        list = list.copy(section = MutableStateFlow(section(rows = emptyList()))),
                        document = MutableStateFlow(null),
                        onBack = {},
                    ),
                ),
            ),
        )

        // when / then
        screenshot { content.Content() }
    }

    private fun section(rows: List<CardRow<SignedConsentFormRow>>) = groveSection(key = "forms") {
        card(key = "forms") { rows(rows) }
    }
}
