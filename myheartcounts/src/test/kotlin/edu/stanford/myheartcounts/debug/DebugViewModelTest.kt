//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.featureflag.AppEnvironment
import edu.stanford.myheartcounts.featureflag.FeatureFlag
import edu.stanford.myheartcounts.featureflag.StoredFeatureFlags
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.NavigationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebugViewModelTest {

    private val featureFlags = StoredFeatureFlags(storage = InMemoryKeyValueStorage())
    private val appInfoSource = FakeDebugAppInfoSource()
    private val healthInfoSource = FakeDebugHealthInfoSource()
    private val navigator = FakeNavigator()
    private val shakeDetector = FakeShakeDetector()
    private val mapper = DebugMapperImpl()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hides the sheet until the device is shaken`() {
        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.sheet.value).isNull()
    }

    @Test
    fun `turning off open on shake keeps the sheet open and ignores later shakes`() {
        // given
        val viewModel = viewModel()
        shakeDetector.shake()

        // when
        viewModel.sheet.value?.shake?.toggle?.onToggle?.invoke(false)
        val sheetAfterToggle = viewModel.sheet.value
        sheetAfterToggle?.onDismiss?.invoke()
        shakeDetector.shake()

        // then
        assertThat(sheetAfterToggle).isNotNull()
        assertThat(viewModel.sheet.value).isNull()
        assertThat(shakeDetector.isEnabled.value).isFalse()
    }

    @Test
    fun `shows the sheet when the device is shaken`() {
        // given
        val viewModel = viewModel()

        // when
        shakeDetector.shake()

        // then
        assertThat(viewModel.sheet.value).isNotNull()
    }

    @Test
    fun `follows changes to the app info`() = runTest {
        // given
        val viewModel = viewModel()
        shakeDetector.shake()

        // when
        appInfoSource.setInfo(
            info = DebugAppInfo(
                version = "1.0.0",
                build = "1",
                firebaseProjectId = "project",
                accountId = "account-id",
                enrolledStudyRevision = "44",
                bundledStudyRevision = "44",
                lastSignedConsentVersion = "0.1.0",
            ),
        )

        // then
        val values = viewModel.sheet.value?.appInfo?.filterNotNull()?.first()?.cards?.flatMap { card -> card.rows.map { it.value } }
        assertThat(values).containsAtLeast("account-id", "0.1.0")
    }

    @Test
    fun `lists every flag with its current value`() {
        // given
        featureFlags.setEnabled(
            flag = FeatureFlag.SimplifyOnboarding,
            enabled = true,
        )
        val viewModel = viewModel()

        // when
        shakeDetector.shake()

        // then
        assertThat(viewModel.sheet.value?.featureFlags?.flags?.map { it.isOn.value }).containsExactly(true, false).inOrder()
    }

    @Test
    fun `stores a toggled flag`() {
        // given
        val viewModel = viewModel()
        shakeDetector.shake()

        // when
        viewModel.sheet.value?.featureFlags?.flags?.get(FeatureFlag.UKStudyTesting.ordinal)?.onToggle?.invoke(true)

        // then
        assertThat(featureFlags.isEnabled(flag = FeatureFlag.UKStudyTesting)).isTrue()
        assertThat(viewModel.sheet.value?.featureFlags?.flags?.map { it.isOn.value }).containsExactly(false, true).inOrder()
    }

    @Test
    fun `hides the sheet once it is dismissed`() {
        // given
        val viewModel = viewModel()
        shakeDetector.shake()

        // when
        viewModel.sheet.value?.onDismiss?.invoke()

        // then
        assertThat(viewModel.sheet.value).isNull()
    }

    @Test
    fun `restarts the app on request`() = runTest {
        // given
        val viewModel = viewModel()
        shakeDetector.shake()

        // when
        viewModel.sheet.value?.featureFlags?.restartButton?.action?.invoke()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.RestartApp)
    }

    @Test
    fun `observes shakes in testing`() {
        // when
        viewModel()

        // then
        assertThat(shakeDetector.isObserved).isTrue()
    }

    @Test
    fun `never observes shakes in production`() {
        // when
        viewModel(environment = AppEnvironment.Production)

        // then
        assertThat(shakeDetector.isObserved).isFalse()
    }

    @Test
    fun `never shows the sheet in production`() {
        // given
        val viewModel = viewModel(environment = AppEnvironment.Production)

        // when
        shakeDetector.shake()

        // then
        assertThat(viewModel.sheet.value).isNull()
    }

    private fun viewModel(environment: AppEnvironment = AppEnvironment.Testing) = DebugViewModel(
        featureFlags = featureFlags,
        appInfoSource = appInfoSource,
        healthInfoSource = healthInfoSource,
        navigator = navigator,
        environment = environment,
        shakeDetector = shakeDetector,
        mapper = mapper,
    )
}
