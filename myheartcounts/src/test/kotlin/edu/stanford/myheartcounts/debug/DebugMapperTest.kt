//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.featureflag.FeatureFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.grovealliance.health.HealthConnectAvailability
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.junit.Test

class DebugMapperTest {

    private val mapper = DebugMapperImpl()
    private val appInfo = MutableStateFlow(
        DebugAppInfo(
            version = "1.0.0",
            build = "1",
            firebaseProjectId = null,
            accountId = null,
            isEmailVerified = null,
            enrolledStudyRevision = null,
            bundledStudyRevision = null,
            lastSignedConsentVersion = null,
        ),
    )
    private val healthInfo = MutableStateFlow(
        DebugHealthInfo(
            availability = HealthConnectAvailability.Available,
            isFullyAuthorized = false,
            historyAccess = null,
            backgroundAccess = null,
            read = emptyList(),
            write = emptyList(),
        ),
    )
    private val pushedActions = mutableListOf<DebugMapper.Action>()
    private val actionSink = ActionSource<DebugMapper.Action> { pushedActions.add(it) }.sink<DebugMapper.Action>()

    @Test
    fun `groups the app info into the app, the account and the study`() = runTest {
        // given
        val sheet = mapper.map(input = input(isEnabled = false, scope = backgroundScope))

        // when
        appInfo.value = DebugAppInfo(
            version = "1.2.0",
            build = "42",
            firebaseProjectId = "project",
            accountId = null,
            isEmailVerified = null,
            enrolledStudyRevision = "44",
            bundledStudyRevision = "45",
            lastSignedConsentVersion = "1.0.0",
        )

        // then
        assertThat(sheet.loadedSection(key = "app-info").infoCards().map { card -> card.map { it.label to it.value } })
            .containsExactly(
                listOf(
                    StringResource(MHCStrings.debug_info_version) to StringResource("1.2.0"),
                    StringResource(MHCStrings.debug_info_build) to StringResource("42"),
                ),
                listOf(
                    StringResource(MHCStrings.debug_info_project_id) to StringResource("project"),
                    StringResource(MHCStrings.debug_info_account_id) to StringResource(MHCStrings.debug_info_unavailable),
                    StringResource(MHCStrings.debug_info_email_verified) to StringResource(MHCStrings.debug_info_unavailable),
                ),
                listOf(
                    StringResource(MHCStrings.debug_info_enrolled_revision) to StringResource("44"),
                    StringResource(MHCStrings.debug_info_bundled_revision) to StringResource("45"),
                    StringResource(MHCStrings.debug_info_consent_version) to StringResource("1.0.0"),
                ),
            )
            .inOrder()
    }

    @Test
    fun `shows whether the email is verified`() = runTest {
        // given
        val sheet = mapper.map(input = input(isEnabled = false, scope = backgroundScope))

        // when
        appInfo.value = appInfo.value.copy(isEmailVerified = false)

        // then
        val section = sheet.loadedSection(key = "app-info")
        assertThat(section.infoCards()[1].last().value).isEqualTo(StringResource(MHCStrings.answer_no))
    }

    @Test
    fun `lists each flag with its name, description and value`() {
        // when
        val sheet = mapper.map(input = input(isEnabled = true))

        // then
        val row = sheet.section(key = "feature-flags").toggleRows().single()
        assertThat(row.title).isEqualTo(StringResource(MHCStrings.feature_flag_simplify_onboarding_title))
        assertThat(row.subtitle).isEqualTo(StringResource(MHCStrings.feature_flag_simplify_onboarding_description))
        assertThat(row.isOn.value).isTrue()
    }

    @Test
    fun `reports toggling a flag`() {
        // given
        val sheet = mapper.map(input = input(isEnabled = false))

        // when
        sheet.section(key = "feature-flags").toggleRows().single().onToggle(true)

        // then
        assertThat(pushedActions).containsExactly(
            DebugMapper.Action.FlagToggled(
                flag = FeatureFlag.SimplifyOnboarding,
                enabled = true,
            ),
        )
    }

    @Test
    fun `offers restarting the app`() {
        // when
        val sheet = mapper.map(input = input(isEnabled = false))

        // then
        assertThat(sheet.section(key = "feature-flags").restartButton().title).isEqualTo(StringResource(MHCStrings.feature_flags_restart))
    }

    @Test
    fun `reports a restart request`() = runTest {
        // given
        val sheet = mapper.map(input = input(isEnabled = false))

        // when
        sheet.section(key = "feature-flags").restartButton().action()

        // then
        assertThat(pushedActions).containsExactly(DebugMapper.Action.RestartSelected)
    }

    @Test
    fun `reports dismissing the sheet`() {
        // given
        val sheet = mapper.map(input = input(isEnabled = false))

        // when
        sheet.onDismiss()

        // then
        assertThat(pushedActions).containsExactly(DebugMapper.Action.Dismissed)
    }

    @Test
    fun `reports switching open on shake`() {
        // given
        val sheet = mapper.map(input = input(isEnabled = false))

        // when
        sheet.section(key = "shake").toggleRows().single().onToggle(false)

        // then
        assertThat(pushedActions).containsExactly(DebugMapper.Action.ShakeToOpenToggled(enabled = false))
    }

    @Test
    fun `lists the data types with and without access, marked by the access`() = runTest {
        // given
        val sheet = mapper.map(input = input(isEnabled = false, scope = backgroundScope))

        // when
        healthInfo.value = DebugHealthInfo(
            availability = HealthConnectAvailability.UpdateRequired,
            isFullyAuthorized = false,
            historyAccess = true,
            backgroundAccess = null,
            read = listOf(
                typeAccess(name = "Weight", isGranted = true),
                typeAccess(name = "Steps", isGranted = true),
                typeAccess(name = "HeartRate", isGranted = false),
            ),
            write = listOf(
                typeAccess(name = "Weight", isGranted = true),
                typeAccess(name = "Height", isGranted = false),
            ),
        )

        // then
        val health = sheet.loadedSection(key = "health")
        assertThat(health.title).isEqualTo(StringResource(MHCStrings.debug_health))
        assertThat(health.cardRows(key = "status").map { it.content }).containsExactly(
            row(label = MHCStrings.debug_health_availability, value = StringResource(MHCStrings.debug_health_update_required)),
            row(label = MHCStrings.debug_health_fully_authorized, value = StringResource(MHCStrings.answer_no)),
            row(label = MHCStrings.debug_health_history_access, value = StringResource(MHCStrings.answer_yes)),
            row(label = MHCStrings.debug_health_background_access, value = StringResource(MHCStrings.debug_info_unavailable)),
        ).inOrder()
        assertThat(health.textRow(key = "granted")).isEqualTo(
            DebugTextRow(
                title = StringResource(MHCStrings.debug_health_granted),
                text = StringResource("Steps (R), Weight (RW)"),
            ),
        )
        assertThat(health.textRow(key = "not-granted")).isEqualTo(
            DebugTextRow(
                title = StringResource(MHCStrings.debug_health_not_granted),
                text = StringResource("HeartRate (R), Height (W)"),
            ),
        )
    }

    @Test
    fun `reports no data types when none is required`() = runTest {
        // when
        val health = mapper.map(input = input(isEnabled = false, scope = backgroundScope)).loadedSection(key = "health")

        // then
        assertThat(health.textRow(key = "granted").text).isEqualTo(StringResource(MHCStrings.debug_health_none))
        assertThat(health.textRow(key = "not-granted").text).isEqualTo(StringResource(MHCStrings.debug_health_none))
    }

    private fun input(
        isEnabled: Boolean,
        scope: CoroutineScope = TestScope(),
    ) = DebugMapper.Input(
        appInfo = appInfo,
        healthInfo = healthInfo,
        scope = scope,
        flagValues = mapOf(FeatureFlag.SimplifyOnboarding to MutableStateFlow(isEnabled)),
        shakeToOpen = MutableStateFlow(true),
        actionSink = actionSink,
    )

    private fun row(
        label: Int,
        value: StringResource,
    ) = DebugInfoRow(
        label = StringResource(label),
        value = value,
    )

    private fun typeAccess(
        name: String,
        isGranted: Boolean,
    ) = DebugHealthInfo.TypeAccess(
        name = name,
        isGranted = isGranted,
    )
}
