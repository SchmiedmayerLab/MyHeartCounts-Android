//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.featureflag.AppEnvironment
import edu.stanford.myheartcounts.featureflag.FeatureFlag
import edu.stanford.myheartcounts.featureflag.FeatureFlags
import edu.stanford.myheartcounts.featureflag.StoredFeatureFlags
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.grovealliance.ui.ActionSource

/**
 * Shows the debug sheet whenever the device is shaken: the app's details for support, kept current
 * while the sheet is open, and the feature flags with a restart that applies changes to them. Only the
 * testing environment offers this: in production shakes are never observed and the sheet is never
 * shown.
 */
@Suppress("detekt:LongParameterList")
class DebugViewModel(
    private val featureFlags: FeatureFlags,
    appInfoSource: DebugAppInfoSource,
    healthInfoSource: DebugHealthInfoSource,
    private val navigator: Navigator,
    environment: AppEnvironment,
    private val shakeDetector: ShakeDetector,
    mapper: DebugMapper,
) : ViewModel() {

    // Whether the debug sheet is offered at all; every check below depends on it alone.
    private val isEnabled = environment == AppEnvironment.Testing

    // Read only when the sheet opens, so that production never touches the flags.
    private val flagValues = FeatureFlag.entries.associateWith { MutableStateFlow(it.defaultValue) }
    private val actionSource = ActionSource(::onAction)
    private val _sheet = MutableStateFlow<DebugSheet?>(null)

    /**
     * The sheet to show, or `null` while it is hidden.
     */
    val sheet: StateFlow<DebugSheet?> = _sheet.asStateFlow()

    private val content = mapper.map(
        input = DebugMapper.Input(
            appInfo = appInfoSource.info,
            healthInfo = healthInfoSource.info,
            scope = viewModelScope,
            flagValues = flagValues.mapValues { (_, value) -> value.asStateFlow() },
            shakeToOpen = shakeDetector.isEnabled,
            actionSink = actionSource.sink(),
        ),
    )

    init {
        if (isEnabled) {
            viewModelScope.launch {
                shakeDetector.shakes.collect { show() }
            }
        }
    }

    private fun onAction(action: DebugMapper.Action) {
        when (action) {
            is DebugMapper.Action.FlagToggled -> setEnabled(
                flag = action.flag,
                enabled = action.enabled,
            )
            is DebugMapper.Action.ShakeToOpenToggled -> shakeDetector.setEnabled(enabled = action.enabled)
            DebugMapper.Action.RestartSelected -> navigator.push(event = NavigationEvent.RestartApp)
            DebugMapper.Action.Dismissed -> hide()
        }
    }

    private fun show() {
        if (!isEnabled) return
        flagValues.forEach { (flag, value) -> value.value = featureFlags.isEnabled(flag = flag) }
        _sheet.value = content
    }

    private fun hide() {
        _sheet.value = null
    }

    private fun setEnabled(flag: FeatureFlag, enabled: Boolean) {
        (featureFlags as? StoredFeatureFlags)?.setEnabled(
            flag = flag,
            enabled = enabled,
        )
        flagValues.getValue(flag).value = enabled
    }
}
