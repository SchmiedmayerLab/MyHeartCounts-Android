//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCPlurals
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveAlertDialog
import org.grovealliance.ui.StringResource
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PromptedActionsMapperTest {

    private val pushedActions = mutableListOf<PromptedActionsAction>()
    private val actionSink = ActionSource<PromptedActionsAction> { pushedActions.add(it) }.sink<PromptedActionsAction>()
    private val actions = MutableStateFlow<List<PromptedAction>>(emptyList())
    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private val mapper = PromptedActionsMapperImpl()
    private val demographics = PromptedActionFixtures.create(type = PromptedActionType.CompleteDemographics)
    private val verifyEmail = PromptedActionFixtures.create(type = PromptedActionType.VerifyAccountEmail)

    @Test
    fun `summarises the pending actions that weren't rejected`() {
        // when
        val card = mapper.mapCard(
            actions = listOf(demographics, verifyEmail.copy(isRejected = true)),
            mode = PromptedActionsMode.Pending,
            onClick = {},
        )

        // then
        assertThat(card?.icons).hasSize(1)
        assertThat(card?.title).isEqualTo(StringResource(MHCStrings.prompted_actions_card_title))
        assertThat(card?.subtitle).isEqualTo(StringResource.quantity(MHCPlurals.prompted_actions_card_subtitle, 1))
    }

    @Test
    fun `offers no card for pending actions while none is pending`() {
        // when
        val card = mapper.mapCard(
            actions = listOf(demographics.copy(state = PromptedActionState.Completed)),
            mode = PromptedActionsMode.Pending,
            onClick = {},
        )

        // then
        assertThat(card).isNull()
    }

    @Test
    fun `counts rejected actions when presenting all of them`() {
        // when
        val card = mapper.mapCard(
            actions = listOf(demographics, verifyEmail.copy(isRejected = true)),
            mode = PromptedActionsMode.All,
            onClick = {},
        )

        // then
        assertThat(card?.icons).hasSize(2)
    }

    @Test
    fun `reports everything as done when presenting all actions and none is pending`() {
        // when
        val card = mapper.mapCard(
            actions = listOf(demographics.copy(state = PromptedActionState.Completed)),
            mode = PromptedActionsMode.All,
            onClick = {},
        )

        // then
        assertThat(card?.icons).isEmpty()
        assertThat(card?.title).isEqualTo(StringResource(MHCStrings.prompted_actions_all_set))
        assertThat(card?.subtitle).isEqualTo(StringResource(MHCStrings.prompted_actions_all_completed))
    }

    @Test
    fun `lists the pending actions with a way to stop suggesting them`() {
        // given
        actions.value = listOf(demographics, verifyEmail.copy(state = PromptedActionState.Completed))

        // when
        val sections = mapper.map(input = input(mode = PromptedActionsMode.Pending)).value.sections

        // then
        assertThat(sections.map { it.key }).containsExactly("pending")
        val row = sections.single().actionRows().single()
        assertThat(row.title).isEqualTo(StringResource(MHCStrings.prompted_action_demographics_title))
        assertThat(row.onStopSuggesting).isNotNull()
    }

    @Test
    fun `lists every pending and completed action without rejecting them when presenting all`() {
        // given
        actions.value = listOf(demographics.copy(isRejected = true), verifyEmail.copy(state = PromptedActionState.Completed))

        // when
        val sections = mapper.map(input = input(mode = PromptedActionsMode.All)).value.sections

        // then
        assertThat(sections.map { it.key }).containsExactly("pending", "completed").inOrder()
        assertThat(sections.first().actionRows().single().onStopSuggesting).isNull()
        assertThat(sections.last().title).isEqualTo(StringResource(MHCStrings.prompted_actions_completed))
        assertThat(sections.last().actionRows().single().isCompleted).isTrue()
    }

    @Test
    fun `takes an action from its button`() = runTest {
        // given
        actions.value = listOf(verifyEmail)
        val row = mapper.map(input = input(mode = PromptedActionsMode.Pending)).value.sections.single().actionRows().single()

        // when
        requireNotNull(row.action).action()

        // then
        assertThat(pushedActions).containsExactly(
            PromptedActionsAction.PerformSelected(type = PromptedActionType.VerifyAccountEmail),
        )
    }

    @Test
    fun `asks to stop suggesting an action`() {
        // given
        actions.value = listOf(demographics)
        val sections = mapper.map(input = input(mode = PromptedActionsMode.Pending))

        // when
        requireNotNull(sections.value.sections.single().actionRows().single().onStopSuggesting).invoke()

        // then
        assertThat(pushedActions).containsExactly(
            PromptedActionsAction.StopSuggestingSelected(type = PromptedActionType.CompleteDemographics),
        )
    }

    @Test
    fun `confirms stopping suggesting an action from the dialog`() {
        // given
        val dialog = mapper.mapStopSuggestingDialog(
            type = PromptedActionType.VerifyAccountEmail,
            actionSink = actionSink,
        )

        // when
        dialog.confirmButton.action()

        // then
        assertThat(dialog.confirmButton.style).isEqualTo(GroveAlertDialog.Button.Style.Destructive)
        assertThat(pushedActions).containsExactly(
            PromptedActionsAction.StopSuggestingConfirmed(type = PromptedActionType.VerifyAccountEmail),
        )
    }

    private fun input(mode: PromptedActionsMode) = PromptedActionsInput(
        actions = actions,
        mode = mode,
        scope = scope,
        actionSink = actionSink,
    )
}
