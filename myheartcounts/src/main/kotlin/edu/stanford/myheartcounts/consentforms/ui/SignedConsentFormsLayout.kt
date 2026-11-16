//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSection
import org.grovealliance.ui.horizontalSlideBackward
import org.grovealliance.ui.horizontalSlideForward
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The [list] of signed forms, replaced by the open form's [document] while one is open, which
 * slides in over the list and back out again. Going back from the document calls [onBack]; the
 * list keeps its scroll position meanwhile.
 */
data class SignedConsentFormsLayout(
    val list: SignedConsentFormListLayout,
    val document: StateFlow<ConsentFormDocumentLayout?>,
    val onBack: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val document by document.collectAsStateWithLifecycle()
        val listState = rememberSaveableStateHolder()
        BackHandler(enabled = document != null, onBack = onBack)
        AnimatedContent(
            modifier = modifier,
            targetState = document,
            transitionSpec = {
                if (targetState != null) horizontalSlideForward else horizontalSlideBackward
            },
            label = "signedConsentFormsPage",
        ) { shown ->
            if (shown == null) {
                listState.SaveableStateProvider(key = LIST_STATE_KEY) {
                    list.Content(modifier = Modifier.fillMaxSize())
                }
            } else {
                shown.Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    private companion object {
        const val LIST_STATE_KEY = "list"
    }
}

private class SignedConsentFormsLayoutPreviewProvider : PreviewParameterProvider<SignedConsentFormsLayout> {
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
    private val base = SignedConsentFormsLayout(
        list = SignedConsentFormListLayout(
            section = MutableStateFlow(groveSection(key = "forms") { card(key = "forms") { row(row = row) } }),
            empty = StringResource("You haven't signed any consent forms yet."),
            isRefreshing = MutableStateFlow(false),
            onRefresh = {},
        ),
        document = MutableStateFlow(null),
        onBack = {},
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(SignedConsentFormsLayoutPreviewProvider::class) content: SignedConsentFormsLayout,
) {
    GroveTheme { content.Content() }
}
