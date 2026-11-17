//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.promptedactions.ui.PromptedActionsScreenContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.mutableScaffoldState
import kotlin.time.Duration.Companion.milliseconds

/**
 * Backs the prompted actions screen in the [PromptedActionsMode] passed in [savedStateHandle]
 * under [KEY_MODE]: lists the actions, opens the screen taking each, stops suggesting an action once
 * the participant confirmed it, and closes itself shortly after the last pending action is gone.
 */
class PromptedActionsViewModel(
    savedStateHandle: SavedStateHandle,
    source: PromptedActionsSource,
    private val rejections: PromptedActionRejections,
    private val navigator: Navigator,
    private val mapper: PromptedActionsMapper,
) : ViewModel() {

    private val mode = PromptedActionsMode.valueOf(requireNotNull(savedStateHandle.get<String>(KEY_MODE)))
    private val route = MHCRoute.PromptedActions(mode = mode)
    private val actionSink = ActionSource(::onAction).sink<PromptedActionsAction>()
    private val actions = source.actions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )
    private val scaffoldState = mutableScaffoldState(appBar = mapper.mapAppBar(actionSink = actionSink))

    /**
     * The screen's content.
     */
    val screen = PromptedActionsScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        sections = mapper.map(
            input = PromptedActionsInput(
                actions = actions,
                mode = mode,
                scope = viewModelScope,
                actionSink = actionSink,
            ),
        ),
    )

    init {
        viewModelScope.launch {
            var hadPending = false
            actions
                .map { mode.pending(actions = it).isNotEmpty() }
                .distinctUntilChanged().collect { hasPending ->
                    if (hadPending && !hasPending) {
                        delay(CLOSE_DELAY)
                        close()
                    }
                    hadPending = hasPending
                }
        }
    }

    private fun onAction(action: PromptedActionsAction) {
        when (action) {
            PromptedActionsAction.Close -> close()
            is PromptedActionsAction.PerformSelected -> navigator.push(
                event = NavigationEvent.NavigateTo(route = destination(type = action.type)),
            )
            is PromptedActionsAction.StopSuggestingSelected -> scaffoldState.showDialog(
                dialog = mapper.mapStopSuggestingDialog(
                    type = action.type,
                    actionSink = actionSink,
                ),
            )
            is PromptedActionsAction.StopSuggestingConfirmed -> rejections.reject(type = action.type)
        }
    }

    private fun destination(type: PromptedActionType): MHCRoute = when (type) {
        PromptedActionType.CompleteDemographics -> MHCRoute.Demographics
        PromptedActionType.VerifyAccountEmail -> MHCRoute.VerifyEmail
    }

    private fun close() {
        navigator.push(event = NavigationEvent.Close(route = route))
    }

    companion object {
        /**
         * The key the [PromptedActionsMode] is passed under, as its name.
         */
        const val KEY_MODE = "mode"

        /**
         * How long the screen stays once the last pending action is gone, so the change can be seen.
         */
        private val CLOSE_DELAY = 450.milliseconds
    }
}
