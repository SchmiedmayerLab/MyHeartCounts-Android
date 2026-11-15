//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import androidx.annotation.StringRes
import edu.stanford.myheartcounts.demographics.ui.DemographicsChoiceSheet
import edu.stanford.myheartcounts.ui.TextRow
import edu.stanford.myheartcounts.ui.choice.ChoiceFollowUpRow
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSectionList

/**
 * Maps groups of answers into a [DemographicsChoiceSheet]. Every sheet marks the current answers
 * in [DemographicsLayoutInput.response] and reports through [DemographicsLayoutInput.actionSink].
 */
interface DemographicsChoiceGroupMapper {

    /**
     * The options of [field] in the [DemographicsCatalog], each paired with the answer [value] reads
     * from its id; options [value] reads no answer from are left out.
     */
    fun <T : Any> mapChoices(field: DemographicsField, value: (String) -> T?): List<Choice<T>>

    /**
     * A sheet allowing a single answer, the [declined] answer on a card of its own after the others.
     */
    fun <T : Any> mapDeclinableSheet(
        @StringRes title: Int,
        choices: List<Choice<T>>,
        declined: T,
        current: (DemographicsResponse) -> T?,
        change: (T) -> DemographicsAction.ResponseChanged,
        input: DemographicsLayoutInput,
    ): DemographicsChoiceSheet

    /**
     * A sheet allowing a single answer, marking [current] and closing once an answer is chosen.
     */
    @Suppress("detekt:LongParameterList")
    fun <T : Any> mapSingleSheet(
        @StringRes title: Int,
        groups: List<Group<T>>,
        current: (DemographicsResponse) -> T?,
        change: (T) -> DemographicsAction.ResponseChanged,
        input: DemographicsLayoutInput,
        prompt: StringResource? = null,
    ): DemographicsChoiceSheet

    /**
     * A sheet of [groups], marking the answers [isSelected] holds for and reporting the [change] of a
     * chosen one. [followUp] and [detail] add to an answer, [isOffered] hides one, and [onSelect] is
     * told of every choice.
     */
    @Suppress("detekt:LongParameterList")
    fun <T : Any> mapSheet(
        @StringRes title: Int,
        groups: List<Group<T>>,
        isSelected: (DemographicsResponse, T) -> Boolean,
        change: ((DemographicsResponse, T) -> DemographicsAction.ResponseChanged)?,
        closesOnSelect: Boolean,
        input: DemographicsLayoutInput,
        prompt: StringResource? = null,
        followUp: ((DemographicsResponse, T) -> ChoiceFollowUpRow?)? = null,
        detail: ((DemographicsResponse, T) -> StringResource?)? = null,
        isOffered: (DemographicsResponse, T) -> Boolean = { _, _ -> true },
        onSelect: ((T) -> Unit)? = null,
    ): DemographicsChoiceSheet

    /**
     * An answer with how it is presented.
     */
    data class Choice<T : Any>(
        val value: T,
        val option: DemographicsOption,
        val detail: StringResource? = null,
    )

    /**
     * Answers shown together on one card.
     */
    data class Group<T : Any>(
        val choices: List<Choice<T>>,
        val header: StringResource? = null,
        val footer: StringResource? = null,
    )
}

/**
 * Default [DemographicsChoiceGroupMapper] implementation, reading the options of [catalog].
 */
class DemographicsChoiceGroupMapperImpl(
    private val catalog: DemographicsCatalog,
) : DemographicsChoiceGroupMapper {

    override fun <T : Any> mapChoices(
        field: DemographicsField,
        value: (String) -> T?,
    ): List<DemographicsChoiceGroupMapper.Choice<T>> = catalog.options(field).mapNotNull { option ->
        value(option.id)?.let {
            DemographicsChoiceGroupMapper.Choice(
                value = it,
                option = option,
            )
        }
    }

    override fun <T : Any> mapDeclinableSheet(
        @StringRes title: Int,
        choices: List<DemographicsChoiceGroupMapper.Choice<T>>,
        declined: T,
        current: (DemographicsResponse) -> T?,
        change: (T) -> DemographicsAction.ResponseChanged,
        input: DemographicsLayoutInput,
    ): DemographicsChoiceSheet {
        val (declining, answers) = choices.partition { it.value == declined }
        return mapSingleSheet(
            title = title,
            groups = listOf(
                DemographicsChoiceGroupMapper.Group(choices = answers),
                DemographicsChoiceGroupMapper.Group(choices = declining),
            ).filter { it.choices.isNotEmpty() },
            current = current,
            change = change,
            input = input,
        )
    }

    override fun <T : Any> mapSingleSheet(
        @StringRes title: Int,
        groups: List<DemographicsChoiceGroupMapper.Group<T>>,
        current: (DemographicsResponse) -> T?,
        change: (T) -> DemographicsAction.ResponseChanged,
        input: DemographicsLayoutInput,
        prompt: StringResource?,
    ) = mapSheet(
        title = title,
        prompt = prompt,
        groups = groups,
        isSelected = { response, value -> current(response) == value },
        change = { _, value -> change(value) },
        closesOnSelect = true,
        input = input,
    )

    override fun <T : Any> mapSheet(
        @StringRes title: Int,
        groups: List<DemographicsChoiceGroupMapper.Group<T>>,
        isSelected: (DemographicsResponse, T) -> Boolean,
        change: ((DemographicsResponse, T) -> DemographicsAction.ResponseChanged)?,
        closesOnSelect: Boolean,
        input: DemographicsLayoutInput,
        prompt: StringResource?,
        followUp: ((DemographicsResponse, T) -> ChoiceFollowUpRow?)?,
        detail: ((DemographicsResponse, T) -> StringResource?)?,
        isOffered: (DemographicsResponse, T) -> Boolean,
        onSelect: ((T) -> Unit)?,
    ): DemographicsChoiceSheet {
        fun select(value: T) {
            change?.let { input.actionSink.push(it(input.response.value, value)) }
            onSelect?.invoke(value)
            if (closesOnSelect) input.actionSink.push(DemographicsAction.SheetClosed)
        }

        fun sections(response: DemographicsResponse): GroveSectionList = groveSectionList {
            prompt?.let {
                section(key = PROMPT_KEY) {
                    card(key = PROMPT_KEY) {
                        row(
                            key = PROMPT_KEY,
                            content = TextRow(text = it),
                        )
                    }
                }
            }
            groups.forEachIndexed { index, group ->
                val choices = group.choices.filter { isOffered(response, it.value) }
                if (choices.isNotEmpty()) {
                    section(key = "$GROUP_KEY-$index") {
                        group.header?.let { title(stringResource = it) }
                        card(key = OPTIONS_KEY) {
                            choices.forEach { choice ->
                                row(
                                    key = choice.option.id,
                                    content = ChoiceRow(
                                        icon = null,
                                        title = choice.option.title,
                                        subtitle = choice.option.subtitle,
                                        detail = detail?.invoke(response, choice.value) ?: choice.detail,
                                        isSelected = isSelected(response, choice.value),
                                        followUp = followUp?.invoke(response, choice.value),
                                        onClick = { select(value = choice.value) },
                                    ),
                                )
                            }
                        }
                        group.footer?.let { footer(stringResource = it) }
                    }
                }
            }
        }

        return DemographicsChoiceSheet(
            appBar = groveAppBar {
                title(title)
                close { input.actionSink.push(DemographicsAction.SheetClosed) }
            },
            monthYearPicker = input.monthYearPicker,
            sections = input.response
                .map { sections(response = it) }
                .stateIn(
                    scope = input.scope,
                    started = SharingStarted.WhileSubscribed(),
                    initialValue = sections(response = input.response.value),
                ),
        )
    }

    private companion object {
        const val PROMPT_KEY = "prompt"
        const val GROUP_KEY = "group"
        const val OPTIONS_KEY = "options"
    }
}
