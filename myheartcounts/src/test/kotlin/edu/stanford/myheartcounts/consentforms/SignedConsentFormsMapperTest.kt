//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class SignedConsentFormsMapperTest {

    private val pushedActions = mutableListOf<SignedConsentFormsAction>()
    private val actionSink = ActionSource<SignedConsentFormsAction> { pushedActions.add(it) }.sink<SignedConsentFormsAction>()
    private val forms = MutableStateFlow<List<SignedConsentForm>>(emptyList())
    private val downloadingId = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private val open = MutableStateFlow<OpenConsentForm?>(null)
    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private val timeProvider = FakeTimeProvider().apply { setZone(zone = ZONE) }
    private val mapper = SignedConsentFormsMapperImpl(timeProvider = timeProvider)

    @Test
    fun `lists each form with its version, title and signing time, keyed by its identifier`() {
        // given
        val signedAt = Instant.parse("2026-01-01T09:00:00Z")
        forms.value = listOf(
            SignedConsentFormFixtures.create(
                id = "1767258000.pdf",
                title = "My Heart Counts Consent Form",
                version = "1.0.0",
                signedAt = signedAt,
            ),
        )

        // when
        val row = mapper.map(input = input()).list.rows().single()

        // then
        assertThat(row.key).isEqualTo("1767258000.pdf")
        assertThat(row.content.version).isEqualTo(StringResource("1.0.0"))
        assertThat(row.content.title).isEqualTo(StringResource("My Heart Counts Consent Form"))
        assertThat(row.content.signedAt).isEqualTo(StringResource(FORMATTER.format(signedAt)))
    }

    @Test
    fun `names a form stored without a title generically and leaves out its unknown details`() {
        // given
        forms.value = listOf(SignedConsentFormFixtures.create())

        // when
        val row = mapper.map(input = input()).list.rows().single().content

        // then
        assertThat(row.title).isEqualTo(StringResource(MHCStrings.consent_forms_untitled))
        assertThat(row.version).isNull()
        assertThat(row.signedAt).isNull()
    }

    @Test
    fun `opens a form when its row is tapped`() {
        // given
        forms.value = listOf(SignedConsentFormFixtures.create(id = "1767258000.pdf"))
        val row = mapper.map(input = input()).list.rows().single().content

        // when
        row.onClick()

        // then
        assertThat(pushedActions).containsExactly(SignedConsentFormsAction.FormSelected(id = "1767258000.pdf"))
    }

    @Test
    fun `shows progress on the form being opened and disables every row meanwhile`() {
        // given
        forms.value = listOf(
            SignedConsentFormFixtures.create(id = "first.pdf"),
            SignedConsentFormFixtures.create(id = "second.pdf"),
        )
        val list = mapper.map(input = input()).list

        // when
        downloadingId.value = "first.pdf"

        // then
        assertThat(list.rows().map { it.content.isLoading }).containsExactly(true, false).inOrder()
        assertThat(list.rows().map { it.content.isEnabled }).containsExactly(false, false)
    }

    @Test
    fun `reads the forms again when the list is pulled`() {
        // given
        val list = mapper.map(input = input()).list

        // when
        list.onRefresh()

        // then
        assertThat(pushedActions).containsExactly(SignedConsentFormsAction.Refresh)
    }

    @Test
    fun `shows the pages of the open form`() {
        // given
        val layout = mapper.map(input = input())
        val pdf = FakeRenderablePdf().apply { setPageAspectRatios(aspectRatios = listOf(0.7f, 0.75f)) }

        // when
        open.value = OpenConsentForm(
            download = DownloadedConsentFormFixtures.create(),
            pdf = pdf,
        )

        // then
        assertThat(layout.document.value?.pages?.map { it.aspectRatio }).containsExactly(0.7f, 0.75f).inOrder()
    }

    @Test
    fun `shows the list again once the form is closed`() {
        // given
        open.value = OpenConsentForm(
            download = DownloadedConsentFormFixtures.create(),
            pdf = FakeRenderablePdf(),
        )
        val layout = mapper.map(input = input())

        // when
        open.value = null

        // then
        assertThat(layout.document.value).isNull()
    }

    @Test
    fun `goes back from the open form`() {
        // given
        val layout = mapper.map(input = input())

        // when
        layout.onBack()

        // then
        assertThat(pushedActions).containsExactly(SignedConsentFormsAction.Back)
    }

    @Test
    fun `titles the list with the screen's title`() {
        // when
        val appBar = mapper.mapAppBar(
            open = null,
            actionSink = actionSink,
        )

        // then
        assertThat(appBar.title).isEqualTo(groveAppBar { title(MHCStrings.consent_forms_title) }.title)
    }

    @Test
    fun `titles the open form with its title`() {
        // given
        val open = OpenConsentForm(
            download = DownloadedConsentFormFixtures.create(
                form = SignedConsentFormFixtures.create(title = "My Heart Counts Consent Form"),
            ),
            pdf = FakeRenderablePdf(),
        )

        // when
        val appBar = mapper.mapAppBar(
            open = open,
            actionSink = actionSink,
        )

        // then
        assertThat(appBar.title).isEqualTo(groveAppBar { title(StringResource("My Heart Counts Consent Form")) }.title)
        assertThat(appBar.actions).hasSize(1)
    }

    private fun input() = SignedConsentFormsInput(
        forms = forms,
        downloadingId = downloadingId,
        isRefreshing = isRefreshing,
        open = open,
        scope = scope,
        actionSink = actionSink,
    )

    private companion object {
        val ZONE: ZoneId = ZoneId.of("America/Los_Angeles")
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZONE)
    }
}
