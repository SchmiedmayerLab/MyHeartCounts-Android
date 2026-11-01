//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.annotation.StringRes
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.featureflag.FeatureFlag
import edu.stanford.myheartcounts.ui.ToggleSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.health.HealthConnectAvailability
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar

/**
 * Builds the debug sheet.
 */
interface DebugMapper {

    /**
     * Maps [input] into the sheet: a switch deciding whether a shake opens the sheet for the rest of
     * the app session; the feature flags section with one switch per flag and a restart
     * button; the app info section grouped into the app, the account and the study; the health
     * section with its status and the data types with and without access, both following their
     * changes; and an app bar that closes the sheet.
     */
    fun map(input: Input): DebugSheet

    /**
     * The data the debug sheet is built from, with the [scope] its health section is read in while
     * it is shown.
     */
    data class Input(
        val appInfo: Flow<DebugAppInfo>,
        val healthInfo: Flow<DebugHealthInfo>,
        val scope: CoroutineScope,
        val shakeToOpen: StateFlow<Boolean>,
        val flagValues: Map<FeatureFlag, StateFlow<Boolean>>,
        val actionSink: ActionSink<Action>,
    )

    /**
     * An intent raised by the debug sheet.
     */
    sealed interface Action {

        /**
         * The feature [flag] was switched to [enabled].
         */
        data class FlagToggled(
            val flag: FeatureFlag,
            val enabled: Boolean,
        ) : Action

        /**
         * Opening the sheet on a shake was switched to [enabled] for the rest of the app session.
         */
        data class ShakeToOpenToggled(
            val enabled: Boolean,
        ) : Action

        /**
         * A restart of the app was requested.
         */
        data object RestartSelected : Action

        /**
         * The sheet was closed or dismissed.
         */
        data object Dismissed : Action
    }
}

/**
 * Default [DebugMapper] implementation.
 */
class DebugMapperImpl : DebugMapper {

    override fun map(input: DebugMapper.Input) = DebugSheet(
        appBar = groveAppBar {
            title(MHCStrings.debug_title)
            close { input.actionSink.push(DebugMapper.Action.Dismissed) }
        },
        appInfo = input.appInfo
            .map { info -> mapAppInfo(info = info) }
            .stateIn(
                scope = input.scope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = null,
            ),
        health = input.healthInfo
            .map { info -> mapHealth(info = info) }
            .stateIn(
                scope = input.scope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = null,
            ),
        shake = DebugShakeSection(
            title = DebugSectionTitle(title = StringResource(MHCStrings.debug_shake)),
            toggle = ToggleSection(
                title = StringResource(MHCStrings.debug_shake_to_open),
                subtitle = StringResource(MHCStrings.debug_shake_to_open_description),
                isOn = input.shakeToOpen,
                onToggle = { enabled -> input.actionSink.push(DebugMapper.Action.ShakeToOpenToggled(enabled = enabled)) },
            ),
        ),
        featureFlags = DebugFeatureFlagsSection(
            title = DebugSectionTitle(title = StringResource(MHCStrings.debug_feature_flags)),
            flags = input.flagValues.map { (flag, value) ->
                ToggleSection(
                    title = StringResource(mapFlagTitle(flag = flag)),
                    subtitle = StringResource(mapFlagDescription(flag = flag)),
                    isOn = value,
                    onToggle = { enabled ->
                        input.actionSink.push(
                            DebugMapper.Action.FlagToggled(
                                flag = flag,
                                enabled = enabled,
                            ),
                        )
                    },
                )
            },
            note = StringResource(MHCStrings.feature_flags_restart_note),
            restartButton = AsyncTextButton(
                title = StringResource(MHCStrings.feature_flags_restart),
                action = { input.actionSink.push(DebugMapper.Action.RestartSelected) },
            ),
        ),
        onDismiss = { input.actionSink.push(DebugMapper.Action.Dismissed) },
    )

    private fun mapAppInfo(info: DebugAppInfo) = DebugAppInfoSection(
        title = DebugSectionTitle(title = StringResource(MHCStrings.debug_app_info)),
        cards = listOf(
            mapCard(
                MHCStrings.debug_info_version to info.version,
                MHCStrings.debug_info_build to info.build,
            ),
            mapCard(
                MHCStrings.debug_info_project_id to info.firebaseProjectId,
                MHCStrings.debug_info_account_id to info.accountId,
            ),
            mapCard(
                MHCStrings.debug_info_enrolled_revision to info.enrolledStudyRevision,
                MHCStrings.debug_info_bundled_revision to info.bundledStudyRevision,
                MHCStrings.debug_info_consent_version to info.lastSignedConsentVersion,
            ),
        ),
    )

    private fun mapHealth(info: DebugHealthInfo): DebugHealthSection {
        val read = info.read.associate { it.name to it.isGranted }
        val write = info.write.associate { it.name to it.isGranted }
        val names = (read.keys + write.keys).sorted()
        return DebugHealthSection(
            title = DebugSectionTitle(title = StringResource(MHCStrings.debug_health)),
            status = mapCard(
                MHCStrings.debug_health_availability to mapAvailability(availability = info.availability),
                MHCStrings.debug_health_fully_authorized to mapYesNo(value = info.isFullyAuthorized),
                MHCStrings.debug_health_history_access to info.historyAccess?.let { mapYesNo(value = it) },
                MHCStrings.debug_health_background_access to info.backgroundAccess?.let { mapYesNo(value = it) },
            ),
            granted = DebugTextCard(
                title = StringResource(MHCStrings.debug_health_granted),
                text = mapTypes(names = names) { name ->
                    mapAccess(
                        read = read[name] == true,
                        write = write[name] == true,
                    )
                },
            ),
            notGranted = DebugTextCard(
                title = StringResource(MHCStrings.debug_health_not_granted),
                text = mapTypes(names = names) { name ->
                    mapAccess(
                        read = read[name] == false,
                        write = write[name] == false,
                    )
                },
            ),
        )
    }

    /**
     * The [names] with an [access] marker, comma separated, or "None" when none has one.
     */
    private fun mapTypes(
        names: List<String>,
        access: (String) -> String?,
    ): StringResource {
        val marked = names.mapNotNull { name -> access(name)?.let { "$name ($it)" } }
        return if (marked.isEmpty()) StringResource(MHCStrings.debug_health_none) else StringResource(marked.joinToString(separator = ", "))
    }

    /**
     * The marker for [read] and [write] access, or `null` for neither.
     */
    private fun mapAccess(
        read: Boolean,
        write: Boolean,
    ): String? = when {
        read && write -> "RW"
        read -> "R"
        write -> "W"
        else -> null
    }

    private fun mapAvailability(availability: HealthConnectAvailability) = when (availability) {
        HealthConnectAvailability.Available -> "Available"
        HealthConnectAvailability.UpdateRequired -> "Update required"
        HealthConnectAvailability.Unavailable -> "Unavailable"
    }

    private fun mapYesNo(value: Boolean) = if (value) YES else NO

    private fun mapCard(vararg rows: Pair<Int, String?>) = DebugRowsCard(
        rows = rows.map { (label, value) ->
            DebugRowsCard.Row(
                label = StringResource(label),
                value = value,
            )
        },
    )

    @StringRes
    private fun mapFlagTitle(flag: FeatureFlag): Int = when (flag) {
        FeatureFlag.SimplifyOnboarding -> MHCStrings.feature_flag_simplify_onboarding_title
        FeatureFlag.UKStudyTesting -> MHCStrings.feature_flag_uk_study_testing_title
    }

    @StringRes
    private fun mapFlagDescription(flag: FeatureFlag): Int = when (flag) {
        FeatureFlag.SimplifyOnboarding -> MHCStrings.feature_flag_simplify_onboarding_description
        FeatureFlag.UKStudyTesting -> MHCStrings.feature_flag_uk_study_testing_description
    }

    private companion object {
        const val YES = "Yes"
        const val NO = "No"
    }
}
