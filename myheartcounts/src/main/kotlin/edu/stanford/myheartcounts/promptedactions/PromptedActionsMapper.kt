//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.graphics.vector.ImageVector
import edu.stanford.myheartcounts.MHCPlurals
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.GroveAlertDialog
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveSectionBuilderScope
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSectionList
import org.grovealliance.ui.scheduler.PromptedActionRow
import org.grovealliance.ui.scheduler.PromptedActionsCard

/**
 * Presents prompted actions: as the card summarising them and as the screen listing them.
 */
interface PromptedActionsMapper {

    /**
     * The card summarising the pending actions presented in [mode], opening them with [onClick];
     * `null` in [PromptedActionsMode.Pending] while none is pending.
     */
    fun mapCard(
        actions: List<PromptedAction>,
        mode: PromptedActionsMode,
        onClick: () -> Unit,
    ): PromptedActionsCard?

    /**
     * The sections listing the actions of [PromptedActionsInput.mode], following their changes.
     */
    fun map(input: PromptedActionsInput): StateFlow<GroveSectionList>

    /**
     * The screen's app bar, closing it through [actionSink].
     */
    fun mapAppBar(actionSink: ActionSink<PromptedActionsAction>): GroveAppBar

    /**
     * The dialog confirming that the action of [type] should no longer be suggested, answered
     * through [actionSink].
     */
    fun mapStopSuggestingDialog(
        type: PromptedActionType,
        actionSink: ActionSink<PromptedActionsAction>,
    ): GroveAlertDialog
}

/**
 * What the prompted actions screen is built from.
 *
 * @param actions every prompted action
 * @param mode which actions the screen presents
 * @param scope the scope the actions are followed in
 * @param actionSink receives the screen's interactions
 */
data class PromptedActionsInput(
    val actions: StateFlow<List<PromptedAction>>,
    val mode: PromptedActionsMode,
    val scope: CoroutineScope,
    val actionSink: ActionSink<PromptedActionsAction>,
)

/**
 * Default [PromptedActionsMapper] implementation.
 */
class PromptedActionsMapperImpl : PromptedActionsMapper {

    override fun mapCard(
        actions: List<PromptedAction>,
        mode: PromptedActionsMode,
        onClick: () -> Unit,
    ): PromptedActionsCard? {
        val pending = mode.pending(actions = actions)
        if (pending.isEmpty() && mode == PromptedActionsMode.Pending) return null
        return PromptedActionsCard(
            icons = pending.map { ImageResource(image = icon(type = it.type)) },
            title = StringResource(
                if (pending.isEmpty()) MHCStrings.prompted_actions_all_set else MHCStrings.prompted_actions_card_title,
            ),
            subtitle = if (pending.isEmpty()) {
                StringResource(MHCStrings.prompted_actions_all_completed)
            } else {
                StringResource.quantity(MHCPlurals.prompted_actions_card_subtitle, pending.size)
            },
            onClick = onClick,
        )
    }

    override fun map(input: PromptedActionsInput) = input.actions
        .map { sections(actions = it, input = input) }
        .stateIn(
            scope = input.scope,
            started = SharingStarted.Eagerly,
            initialValue = sections(actions = input.actions.value, input = input),
        )

    override fun mapAppBar(actionSink: ActionSink<PromptedActionsAction>) = groveAppBar {
        close { actionSink.push(PromptedActionsAction.Close) }
        title(MHCStrings.prompted_actions_title)
    }

    private fun sections(
        actions: List<PromptedAction>,
        input: PromptedActionsInput,
    ): GroveSectionList {
        val pending = input.mode.pending(actions = actions)
        val completed = actions.filter { it.state == PromptedActionState.Completed }
        return groveSectionList {
            if (pending.isNotEmpty()) {
                section(key = PENDING_KEY) {
                    actionCards(
                        actions = pending,
                        input = input,
                    )
                    footer(
                        when (input.mode) {
                            PromptedActionsMode.Pending -> MHCStrings.prompted_actions_footer_pending
                            PromptedActionsMode.All -> MHCStrings.prompted_actions_footer_all
                        },
                    )
                }
            }
            if (input.mode == PromptedActionsMode.All && completed.isNotEmpty()) {
                section(key = COMPLETED_KEY) {
                    title(MHCStrings.prompted_actions_completed)
                    actionCards(
                        actions = completed,
                        input = input,
                    )
                    if (pending.isEmpty()) footer(MHCStrings.prompted_actions_footer_all)
                }
            }
        }
    }

    private fun GroveSectionBuilderScope.actionCards(
        actions: List<PromptedAction>,
        input: PromptedActionsInput,
    ) {
        actions.forEach { action ->
            card(key = action.type.id) {
                row(row = row(action = action, input = input))
            }
        }
    }

    private fun row(
        action: PromptedAction,
        input: PromptedActionsInput,
    ) = CardRow(
        key = action.type.id,
        content = PromptedActionRow(
            icon = ImageResource(image = icon(type = action.type)),
            title = StringResource(title(type = action.type)),
            message = StringResource(message(type = action.type)),
            action = AsyncTextButton(
                title = StringResource(buttonTitle(type = action.type)),
                action = { input.actionSink.push(PromptedActionsAction.PerformSelected(type = action.type)) },
            ),
            isCompleted = action.state == PromptedActionState.Completed,
            onStopSuggesting = if (input.mode == PromptedActionsMode.Pending && action.state == PromptedActionState.Pending) {
                { input.actionSink.push(PromptedActionsAction.StopSuggestingSelected(type = action.type)) }
            } else {
                null
            },
        ),
    )

    override fun mapStopSuggestingDialog(
        type: PromptedActionType,
        actionSink: ActionSink<PromptedActionsAction>,
    ) = GroveAlertDialog(
        title = StringResource(MHCStrings.prompted_actions_stop_suggesting_title),
        message = StringResource(MHCStrings.prompted_actions_stop_suggesting_message),
        confirmButton = GroveAlertDialog.Button(
            title = StringResource(MHCStrings.prompted_actions_stop_suggesting_confirm),
            style = GroveAlertDialog.Button.Style.Destructive,
            action = { actionSink.push(PromptedActionsAction.StopSuggestingConfirmed(type = type)) },
        ),
        dismissButton = GroveAlertDialog.Button(
            title = StringResource(MHCStrings.prompted_actions_stop_suggesting_cancel),
            action = {},
        ),
    )

    private fun icon(type: PromptedActionType): ImageVector = when (type) {
        PromptedActionType.CompleteDemographics -> Icons.Default.Badge
        PromptedActionType.VerifyAccountEmail -> Icons.Default.Email
    }

    private fun title(type: PromptedActionType): Int = when (type) {
        PromptedActionType.CompleteDemographics -> MHCStrings.prompted_action_demographics_title
        PromptedActionType.VerifyAccountEmail -> MHCStrings.prompted_action_verify_email_title
    }

    private fun message(type: PromptedActionType): Int = when (type) {
        PromptedActionType.CompleteDemographics -> MHCStrings.prompted_action_demographics_message
        PromptedActionType.VerifyAccountEmail -> MHCStrings.prompted_action_verify_email_message
    }

    private fun buttonTitle(type: PromptedActionType): Int = when (type) {
        PromptedActionType.CompleteDemographics -> MHCStrings.prompted_action_demographics_button
        PromptedActionType.VerifyAccountEmail -> MHCStrings.prompted_action_verify_email_button
    }

    private companion object {
        const val PENDING_KEY = "pending"
        const val COMPLETED_KEY = "completed"
    }
}
