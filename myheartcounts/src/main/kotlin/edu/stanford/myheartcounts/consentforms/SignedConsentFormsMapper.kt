//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.asImageBitmap
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.consentforms.ui.ConsentFormDocumentLayout
import edu.stanford.myheartcounts.consentforms.ui.SignedConsentFormListLayout
import edu.stanford.myheartcounts.consentforms.ui.SignedConsentFormRow
import edu.stanford.myheartcounts.consentforms.ui.SignedConsentFormsLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSection
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Builds the signed consent forms screen: the list of forms, the open form and the app bar.
 * Interactions are dispatched as [SignedConsentFormsAction]s.
 */
interface SignedConsentFormsMapper {

    /**
     * The list of [SignedConsentFormsInput.forms], showing [SignedConsentFormsInput.open] while a
     * form is open.
     */
    fun map(input: SignedConsentFormsInput): SignedConsentFormsLayout

    /**
     * The app bar of the list, or of the [open] form while one is open.
     */
    fun mapAppBar(
        open: OpenConsentForm?,
        actionSink: ActionSink<SignedConsentFormsAction>,
    ): GroveAppBar
}

/**
 * What the signed consent forms screen is built from.
 *
 * @param forms the participant's forms, in display order
 * @param downloadingId the identifier of the form being opened, if any
 * @param isRefreshing whether the forms are being read again
 * @param open the open form, if any
 * @param scope the scope the screen's state is followed in
 * @param actionSink receives the screen's interactions
 */
data class SignedConsentFormsInput(
    val forms: StateFlow<List<SignedConsentForm>>,
    val downloadingId: StateFlow<String?>,
    val isRefreshing: StateFlow<Boolean>,
    val open: StateFlow<OpenConsentForm?>,
    val scope: CoroutineScope,
    val actionSink: ActionSink<SignedConsentFormsAction>,
)

/**
 * Default [SignedConsentFormsMapper] implementation, showing when each form was signed in the
 * zone of the [timeProvider]. While a form is being opened, no other form can be opened.
 */
class SignedConsentFormsMapperImpl(
    private val timeProvider: TimeProvider,
) : SignedConsentFormsMapper {

    override fun map(input: SignedConsentFormsInput) = SignedConsentFormsLayout(
        list = SignedConsentFormListLayout(
            section = combine(input.forms, input.downloadingId) { forms, downloadingId ->
                section(
                    forms = forms,
                    downloadingId = downloadingId,
                    input = input,
                )
            }.stateIn(
                scope = input.scope,
                started = SharingStarted.Eagerly,
                initialValue = section(
                    forms = input.forms.value,
                    downloadingId = input.downloadingId.value,
                    input = input,
                ),
            ),
            empty = StringResource(MHCStrings.consent_forms_empty),
            isRefreshing = input.isRefreshing,
            onRefresh = { input.actionSink.push(SignedConsentFormsAction.Refresh) },
        ),
        document = input.open
            .map { document(open = it) }
            .stateIn(
                scope = input.scope,
                started = SharingStarted.Eagerly,
                initialValue = document(open = input.open.value),
            ),
        onBack = { input.actionSink.push(SignedConsentFormsAction.Back) },
    )

    private fun section(
        forms: List<SignedConsentForm>,
        downloadingId: String?,
        input: SignedConsentFormsInput,
    ) = groveSection(key = FORMS_KEY) {
        card(key = FORMS_KEY) {
            forms.forEach { form ->
                row(
                    row = row(
                        form = form,
                        downloadingId = downloadingId,
                        input = input,
                    ),
                )
            }
        }
    }

    override fun mapAppBar(
        open: OpenConsentForm?,
        actionSink: ActionSink<SignedConsentFormsAction>,
    ) = groveAppBar {
        if (open == null) {
            back { actionSink.push(SignedConsentFormsAction.Close) }
            title(MHCStrings.consent_forms_title)
        } else {
            back { actionSink.push(SignedConsentFormsAction.Back) }
            title(title(form = open.download.form))
            action(
                imageResource = ImageResource(
                    image = Icons.Default.Share,
                    contentDescription = StringResource(MHCStrings.consent_forms_share),
                ),
            ) {
                actionSink.push(SignedConsentFormsAction.Share)
            }
        }
    }

    private fun row(
        form: SignedConsentForm,
        downloadingId: String?,
        input: SignedConsentFormsInput,
    ) = CardRow(
        key = form.id,
        content = SignedConsentFormRow(
            version = form.version?.let { StringResource(it) },
            title = title(form = form),
            signedAt = form.signedAt?.let { signedAt(instant = it) },
            isLoading = form.id == downloadingId,
            isEnabled = downloadingId == null,
            onClick = { input.actionSink.push(SignedConsentFormsAction.FormSelected(id = form.id)) },
        ),
    )

    private fun document(open: OpenConsentForm?) = open?.let {
        ConsentFormDocumentLayout(
            pages = it.pdf.pageAspectRatios.mapIndexed { index, aspectRatio ->
                ConsentFormDocumentLayout.Page(
                    aspectRatio = aspectRatio,
                    image = { width -> it.pdf.page(index = index, width = width).map { bitmap -> bitmap.asImageBitmap() } },
                )
            },
        )
    }

    private fun title(form: SignedConsentForm) =
        form.title?.let { StringResource(it) } ?: StringResource(MHCStrings.consent_forms_untitled)

    private fun signedAt(instant: Instant) = StringResource(
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(timeProvider.currentZone())
            .format(instant),
    )

    private companion object {
        const val FORMS_KEY = "forms"
    }
}
