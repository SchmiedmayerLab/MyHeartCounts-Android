//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.GroveAppBar
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignedConsentFormsViewModelTest {

    private val source = FakeSignedConsentFormSource()
    private val renderer = FakePdfPageRenderer()
    private val navigator = FakeNavigator()
    private val mapper = RecordingSignedConsentFormsMapper()
    private val newer = SignedConsentFormFixtures.create(
        id = "1767258000.pdf",
        title = "My Heart Counts Consent Form",
    )
    private val older = SignedConsentFormFixtures.create(id = "1767171600.pdf")

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        source.setForms(forms = Result.success(listOf(newer, older)))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lists the forms once they are read`() {
        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.layout().list.rows().map { it.key }).containsExactly(newer.id, older.id).inOrder()
    }

    @Test
    fun `offers to try again when the forms can't be read`() {
        // given
        source.setForms(forms = Result.failure(IllegalStateException("unreadable")))

        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.screen.state.value).isInstanceOf(SignedConsentFormsScreenState.Error::class.java)
    }

    @Test
    fun `reads the forms again when retried`() = runTest {
        // given
        source.setForms(forms = Result.failure(IllegalStateException("unreadable")))
        val viewModel = viewModel()
        val error = viewModel.screen.state.value as SignedConsentFormsScreenState.Error
        source.setForms(forms = Result.success(listOf(newer)))

        // when
        error.layout.primaryButton.action()

        // then
        assertThat(source.reads).isEqualTo(2)
        assertThat(viewModel.layout().list.rows().map { it.key }).containsExactly(newer.id)
    }

    @Test
    fun `lists the forms read again when the list is pulled`() {
        // given
        val viewModel = viewModel()
        source.setForms(forms = Result.success(listOf(newer)))

        // when
        viewModel.layout().list.onRefresh()

        // then
        assertThat(viewModel.layout().list.rows().map { it.key }).containsExactly(newer.id)
        assertThat(viewModel.layout().list.isRefreshing.value).isFalse()
    }

    @Test
    fun `keeps the listed forms when they can't be read again`() {
        // given
        val viewModel = viewModel()
        source.setForms(forms = Result.failure(IllegalStateException("unreadable")))

        // when
        viewModel.layout().list.onRefresh()

        // then
        assertThat(viewModel.layout().list.rows().map { it.key }).containsExactly(newer.id, older.id).inOrder()
        assertThat(viewModel.screen.state.value).isInstanceOf(SignedConsentFormsScreenState.Content::class.java)
    }

    @Test
    fun `shows the pages of a selected form`() {
        // given
        val viewModel = viewModel()
        renderer.setResult(result = Result.success(FakeRenderablePdf().apply { setPageAspectRatios(aspectRatios = listOf(0.7f)) }))

        // when
        viewModel.layout().list.rows().first().content.onClick()

        // then
        assertThat(viewModel.layout().document.value?.pages).hasSize(1)
        assertThat(mapper.appBarForm?.download?.form).isEqualTo(newer)
    }

    @Test
    fun `closes and removes the open form when going back`() {
        // given
        val viewModel = viewModel()
        val pdf = FakeRenderablePdf()
        renderer.setResult(result = Result.success(pdf))
        viewModel.layout().list.rows().first().content.onClick()

        // when
        viewModel.layout().onBack()

        // then
        assertThat(viewModel.layout().document.value).isNull()
        assertThat(pdf.isClosed).isTrue()
        assertThat(source.discarded.map { it.form }).containsExactly(newer)
        assertThat(mapper.appBarForm).isNull()
    }

    @Test
    fun `removes a downloaded form that can't be opened`() {
        // given
        val viewModel = viewModel()
        renderer.setResult(result = Result.failure(IllegalStateException("unreadable")))

        // when
        viewModel.layout().list.rows().first().content.onClick()

        // then
        assertThat(viewModel.layout().document.value).isNull()
        assertThat(source.discarded.map { it.form }).containsExactly(newer)
    }

    @Test
    fun `stays on the list when a form can't be downloaded`() {
        // given
        val viewModel = viewModel()
        source.setDownload(download = Result.failure(IllegalStateException("offline")))

        // when
        viewModel.layout().list.rows().first().content.onClick()

        // then
        assertThat(viewModel.layout().document.value).isNull()
        assertThat(viewModel.layout().list.rows().map { it.content.isEnabled }).containsExactly(true, true)
    }

    @Test
    fun `shares the open form`() {
        // given
        val viewModel = viewModel()
        source.setDownload(
            download = Result.success(
                DownloadedConsentFormFixtures.create(
                    form = newer,
                    contentUri = "content://edu.stanford.myheartcounts.fileprovider/shared/consent-forms/1767258000.pdf",
                ),
            ),
        )
        viewModel.layout().list.rows().first().content.onClick()

        // when
        mapper.actionSink.push(SignedConsentFormsAction.Share)

        // then
        assertThat(navigator.pushedEvents).containsExactly(
            NavigationEvent.ShareFile(
                uri = "content://edu.stanford.myheartcounts.fileprovider/shared/consent-forms/1767258000.pdf",
                mimeType = "application/pdf",
            ),
        )
    }

    @Test
    fun `closes the screen from the list`() {
        // given
        viewModel()

        // when
        mapper.actionSink.push(SignedConsentFormsAction.Close)

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.Close(route = MHCRoute.SignedConsentForms))
    }

    @Test
    fun `closes and removes the open form when the screen goes away`() {
        // given
        val viewModel = viewModel()
        val pdf = FakeRenderablePdf()
        renderer.setResult(result = Result.success(pdf))
        viewModel.layout().list.rows().first().content.onClick()

        // when
        viewModel.onCleared()

        // then
        assertThat(pdf.isClosed).isTrue()
        assertThat(source.discarded).hasSize(1)
    }

    private fun viewModel() = SignedConsentFormsViewModel(
        source = source,
        renderer = renderer,
        navigator = navigator,
        mapper = mapper,
    )

    private fun SignedConsentFormsViewModel.layout() =
        (screen.state.value as SignedConsentFormsScreenState.Content).layout

    private class RecordingSignedConsentFormsMapper : SignedConsentFormsMapper {
        private val mapper = SignedConsentFormsMapperImpl(timeProvider = FakeTimeProvider())
        private var currentActionSink: ActionSink<SignedConsentFormsAction>? = null
        val actionSink: ActionSink<SignedConsentFormsAction>
            get() = requireNotNull(currentActionSink)
        var appBarForm: OpenConsentForm? = null
            private set

        override fun map(input: SignedConsentFormsInput) = mapper.map(input = input)

        override fun mapAppBar(
            open: OpenConsentForm?,
            actionSink: ActionSink<SignedConsentFormsAction>,
        ): GroveAppBar {
            currentActionSink = actionSink
            appBarForm = open
            return mapper.mapAppBar(
                open = open,
                actionSink = actionSink,
            )
        }
    }
}
