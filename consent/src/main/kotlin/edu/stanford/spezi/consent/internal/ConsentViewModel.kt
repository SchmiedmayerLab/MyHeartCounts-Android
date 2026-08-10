//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.consent.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import edu.stanford.spezi.consent.ConsentConfiguration
import edu.stanford.spezi.consent.ConsentResponses
import edu.stanford.spezi.consent.SignatureMetadata
import edu.stanford.spezi.consent.SignatureStroke
import edu.stanford.spezi.markdown.MarkdownDocument
import edu.stanford.spezi.ui.ActionSink
import edu.stanford.spezi.ui.ActionSource
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.LoadingLayout
import edu.stanford.spezi.ui.theme.Spacings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the consent form: loads the document, tracks responses, evaluates completion,
 * and invokes the consent callback when the user submits.
 */
internal class ConsentViewModel(
    configuration: ConsentConfiguration,
    mapper: ConsentLayoutMapper,
    dataSource: ConsentDocumentDataSource,
) : ViewModel() {

    private var consentCallback: (suspend (ConsentResponses) -> Unit)? = null

    private val initialMetadata = configuration.initialSignatureMetadata()

    private val responses = MutableStateFlow(ConsentResponses.Empty)
    private val currentResponses get() = responses.value
    private val actionSource = ActionSource(::onAction)
    private val actionSink = actionSource.sink<ConsentAction>()
    private val screenState = MutableStateFlow<ConsentLayoutState>(ConsentLayoutState.Loading)

    val layout = ConsentScreenLayout(
        state = screenState.asStateFlow(),
        actionSink = actionSink,
    )

    init {
        viewModelScope.launch {
            val newState = mapper.map(
                input = ConsentLayoutInput(
                    document = dataSource.loadDocument(),
                    initialMetadata = initialMetadata,
                    responses = responses,
                    actionSink = actionSink,
                    mainActionEnabled = isCompleteFlow(),
                    mainAction = ::onConsent,
                ),
            )
            screenState.update { ConsentLayoutState.Content(newState) }
        }
    }

    private fun onAction(action: ConsentAction) {
        when (action) {
            is ConsentAction.ToggleChanged -> responses.update { it.copy(toggles = it.toggles + (action.id to action.value)) }

            is ConsentAction.SelectionChanged -> responses.update { it.copy(selects = it.selects + (action.id to action.value)) }

            is ConsentAction.FirstNameChanged -> responses.update { response ->
                val current = response.signatures[action.id] ?: initialMetadata
                response.copy(signatures = response.signatures + (action.id to current.copy(givenName = action.value)))
            }

            is ConsentAction.LastNameChanged -> responses.update { response ->
                val current = response.signatures[action.id] ?: initialMetadata
                response.copy(signatures = response.signatures + (action.id to current.copy(familyName = action.value)))
            }

            is ConsentAction.SignatureStrokesChanged -> responses.update { response ->
                val current = response.signatures[action.id] ?: initialMetadata
                response.copy(signatures = response.signatures + (action.id to current.copy(strokes = action.strokes)))
            }

            is ConsentAction.OnConsentGivenCallbackUpdated -> consentCallback = action.callback
        }
    }

    private fun isCompleteFlow(): Flow<Boolean> = responses.map {
        val sections = (screenState.value as? ConsentLayoutState.Content)?.layout?.sections ?: return@map false
        sections.all { section ->
            when (section) {
                is ConsentToggleSection -> isToggleComplete(section = section)
                is ConsentSelectSection -> isSelectionComplete(section = section)
                is ConsentSignatureSection -> isSignatureComplete(section = section)
                else -> true
            }
        }
    }.distinctUntilChanged()

    private fun isToggleComplete(section: ConsentToggleSection): Boolean {
        val value = currentResponses.toggles[section.id] ?: section.initialValue
        return section.expectedValue == null || value == section.expectedValue
    }

    private fun isSelectionComplete(section: ConsentSelectSection): Boolean {
        val value = currentResponses.selects[section.id] ?: section.initialValue
        val isNonEmpty = value.isNotEmpty()
        return when (val exp = section.expectedSelection) {
            is ExpectedSelection.Anything -> exp.allowEmptySelection || isNonEmpty
            is ExpectedSelection.Option -> value == exp.id && isNonEmpty
        }
    }

    private fun isSignatureComplete(section: ConsentSignatureSection): Boolean {
        val metadata = currentResponses.signatures[section.id] ?: initialMetadata
        val didEnterNames = metadata.givenName.isNotBlank() && metadata.familyName.isNotBlank()
        val isSigned = metadata.strokes.any { it.points.isNotEmpty() }
        return didEnterNames && isSigned
    }

    private suspend fun onConsent() {
        consentCallback?.invoke(responses.value)
        consentCallback = null
    }
}

/**
 * Top-level composable layout produced by [ConsentViewModel]; delegates rendering
 * to the current [ConsentLayoutState].
 */
internal data class ConsentScreenLayout(
    val state: StateFlow<ConsentLayoutState>,
    val actionSink: ActionSink<ConsentAction>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        val layoutState by state.collectAsStateWithLifecycle()
        layoutState.layout.Content(modifier)
    }
}

/**
 * Represents the loading or ready state of the consent form layout.
 */
internal sealed interface ConsentLayoutState {
    val layout: ComposableContent

    data object Loading : ConsentLayoutState {
        override val layout = LoadingLayout()
    }

    data class Content(override val layout: ConsentContentLayout) : ConsentLayoutState
}

/**
 * The fully-mapped consent form: an ordered list of content sections and a reactive action button.
 */
internal data class ConsentContentLayout(
    val sections: List<ComposableContent>,
    val actionButton: Flow<AsyncTextButton>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        val button by actionButton.collectAsStateWithLifecycle(initialValue = null)
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            contentPadding = PaddingValues(Spacings.medium),
        ) {
            items(sections) { section ->
                section.Content(modifier = Modifier.fillMaxWidth())
            }
            button?.let {
                item { it.Content(modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

/**
 * User interactions and lifecycle events that mutate consent form state.
 */
internal sealed interface ConsentAction {
    data class OnConsentGivenCallbackUpdated(val callback: suspend (ConsentResponses) -> Unit) : ConsentAction
    data class ToggleChanged(val id: String, val value: Boolean) : ConsentAction
    data class SelectionChanged(val id: String, val value: String) : ConsentAction
    data class FirstNameChanged(val id: String, val value: String) : ConsentAction
    data class LastNameChanged(val id: String, val value: String) : ConsentAction
    data class SignatureStrokesChanged(val id: String, val strokes: List<SignatureStroke>) : ConsentAction
}

/**
 * Runtime inputs supplied to [ConsentLayoutMapper] when building a consent form's content.
 *
 * @property document The parsed markdown document to be mapped into consent form content.
 * @property initialMetadata Pre-filled signature metadata for signature elements.
 * @property responses The current state of all user responses.
 * @property actionSink Sink for dispatching [ConsentAction]s back to the owner.
 * @property mainActionEnabled A flow indicating whether the main action button should be enabled, based on the current responses.
 * @property mainAction The callback to be invoked when the main action button is clicked.
 */
internal data class ConsentLayoutInput(
    val document: MarkdownDocument,
    val initialMetadata: SignatureMetadata,
    val responses: StateFlow<ConsentResponses>,
    val actionSink: ActionSink<ConsentAction>,
    val mainActionEnabled: Flow<Boolean>,
    val mainAction: suspend () -> Unit,
)
