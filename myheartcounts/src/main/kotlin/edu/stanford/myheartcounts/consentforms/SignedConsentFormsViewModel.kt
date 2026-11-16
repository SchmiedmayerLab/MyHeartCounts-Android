//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.consentforms.ui.SignedConsentFormsScreenContent
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.mutableScaffoldState
import org.grovealliance.ui.showErrorToast

/**
 * Backs the signed consent forms screen: reads the participant's forms, reads them again when
 * the list is pulled, and opens one form at a time on a page of its own, from where it can be
 * shared. A form opened is copied onto the device and removed again once it is closed.
 */
class SignedConsentFormsViewModel(
    private val source: SignedConsentFormSource,
    private val renderer: PdfPageRenderer,
    private val navigator: Navigator,
    mapper: SignedConsentFormsMapper,
) : ViewModel() {

    private val logger by groveLogger()
    private val actionSink = ActionSource(::onAction).sink<SignedConsentFormsAction>()
    private val forms = MutableStateFlow<List<SignedConsentForm>>(emptyList())
    private val downloadingId = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private val open = MutableStateFlow<OpenConsentForm?>(null)
    private val scaffoldState = mutableScaffoldState(
        appBar = mapper.mapAppBar(
            open = null,
            actionSink = actionSink,
        ),
    )
    private val state = MutableStateFlow<SignedConsentFormsScreenState>(SignedConsentFormsScreenState.Loading)
    private val layout = mapper.map(
        input = SignedConsentFormsInput(
            forms = forms.asStateFlow(),
            downloadingId = downloadingId.asStateFlow(),
            isRefreshing = isRefreshing.asStateFlow(),
            open = open.asStateFlow(),
            scope = viewModelScope,
            actionSink = actionSink,
        ),
    )

    /**
     * The screen's content.
     */
    val screen = SignedConsentFormsScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        state = state.asStateFlow(),
    )

    init {
        viewModelScope.launch {
            open.collect {
                scaffoldState.setAppBar(
                    appBar = mapper.mapAppBar(
                        open = it,
                        actionSink = actionSink,
                    ),
                )
            }
        }
        load()
    }

    private fun onAction(action: SignedConsentFormsAction) {
        when (action) {
            SignedConsentFormsAction.Close -> navigator.push(event = NavigationEvent.Close(route = MHCRoute.SignedConsentForms))
            SignedConsentFormsAction.Back -> closeForm()
            SignedConsentFormsAction.Retry -> load()
            SignedConsentFormsAction.Refresh -> refresh()
            is SignedConsentFormsAction.FormSelected -> openForm(id = action.id)
            SignedConsentFormsAction.Share -> open.value?.let { form ->
                navigator.push(
                    event = NavigationEvent.ShareFile(
                        uri = form.download.contentUri,
                        mimeType = PDF_MIME_TYPE,
                    ),
                )
            }
        }
    }

    private fun load() {
        state.value = SignedConsentFormsScreenState.Loading
        viewModelScope.launch {
            source.forms()
                .onSuccess {
                    forms.value = it
                    state.value = SignedConsentFormsScreenState.Content(layout = layout)
                }
                .onFailure {
                    state.value = SignedConsentFormsScreenState.Error(layout = errorLayout())
                }
        }
    }

    private fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch {
            source.forms()
                .onSuccess { forms.value = it }
                .onFailure { scaffoldState.showErrorToast(message = StringResource(MHCStrings.consent_forms_refresh_failed)) }
            isRefreshing.value = false
        }
    }

    private fun openForm(id: String) {
        val form = forms.value.firstOrNull { it.id == id }
        if (form == null || downloadingId.value != null || open.value != null) return
        downloadingId.value = id
        viewModelScope.launch {
            source.download(form = form)
                .onSuccess { download ->
                    renderer.open(file = download.file)
                        .onSuccess { pdf ->
                            open.value = OpenConsentForm(
                                download = download,
                                pdf = pdf,
                            )
                        }
                        .onFailure {
                            logger.e(it) { "Failed to open a signed consent form." }
                            source.discard(download = download)
                            scaffoldState.showErrorToast(message = StringResource(MHCStrings.consent_forms_open_failed))
                        }
                }
                .onFailure { scaffoldState.showErrorToast(message = StringResource(MHCStrings.consent_forms_open_failed)) }
            downloadingId.value = null
        }
    }

    private fun closeForm() {
        val form = open.value ?: return
        open.value = null
        form.pdf.close()
        source.discard(download = form.download)
    }

    public override fun onCleared() {
        closeForm()
        super.onCleared()
    }

    private fun errorLayout() = GroveErrorLayout(
        title = StringResource(MHCStrings.consent_forms_error_title),
        message = StringResource(MHCStrings.consent_forms_error_message),
        primaryButton = AsyncTextButton(
            title = StringResource(MHCStrings.consent_forms_retry),
            action = { actionSink.push(SignedConsentFormsAction.Retry) },
        ),
    )

    private companion object {
        const val PDF_MIME_TYPE = "application/pdf"
    }
}
