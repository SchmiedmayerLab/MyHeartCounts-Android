//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreSource
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.dashboard.HeartScoreResult
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.study.StudyQuestionnaireSource
import edu.stanford.myheartcounts.task.PerformableTask
import edu.stanford.myheartcounts.task.TaskPerformer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.mutableScaffoldState

/**
 * Backs the details of one factor of the participant's heart health score, read from
 * [savedStateHandle] under [KEY_COMPONENT]: its most recent value and score, its history over the
 * chosen time range (kept under [KEY_RANGE]), the bands explaining its score, the study's explanation
 * and, in debug mode, its stored values, all kept current.
 */
@Suppress("detekt:LongParameterList")
class HeartScoreDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val navigator: Navigator,
    private val scoreSource: HeartHealthScoreSource,
    private val chartSource: HeartScoreChartSource,
    private val explainerSource: HeartScoreExplainerSource,
    private val mapper: HeartScoreDetailContentMapper,
    private val chartMapper: HeartScoreChartMapper,
    private val debugSource: HeartScoreDebugSource,
    private val questionnaires: StudyQuestionnaireSource,
    private val taskPerformer: TaskPerformer,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val logger by groveLogger()
    private val component = HeartScoreComponent.valueOf(requireNotNull(savedStateHandle.get<String>(KEY_COMPONENT)))
    private val selection = MutableStateFlow(
        savedStateHandle.get<String>(KEY_RANGE)?.let { value -> HeartScoreChartRange.entries.firstOrNull { it.name == value } }
            ?: HeartScoreChartRange.Fortnight,
    )
    private val actions = ActionSource(::onAction)
    private val actionSink = actions.sink<HeartScoreDetailAction>()
    private val scaffoldState = mutableScaffoldState(
        appBar = groveAppBar {
            close { actionSink.push(HeartScoreDetailAction.Close) }
            if (component !in setOf(
                    HeartScoreComponent.ExerciseMinutes,
                    HeartScoreComponent.StepCount,
                    HeartScoreComponent.Sleep,
                )
            ) {
                action(
                    imageResource = ImageResource(
                        image = Icons.Default.Add,
                        contentDescription = StringResource(MHCStrings.heart_entry_add_data),
                    ),
                ) {
                    actionSink.push(HeartScoreDetailAction.AddData)
                }
            }
        },
    )
    private val layoutState = MutableStateFlow<HeartScoreDetailLayoutState>(HeartScoreDetailLayoutState.Loading)
    private var observation: Job? = null

    /**
     * The screen's content.
     */
    val screen = HeartScoreDetailScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        state = layoutState.asStateFlow(),
    )

    init {
        load()
    }

    private fun load() {
        observation?.cancel()
        layoutState.value = HeartScoreDetailLayoutState.Loading
        observation = viewModelScope.launch(ioDispatcher) {
            val scope = this
            runCatchingAsync { observe(scope = scope) }.onFailure {
                logger.e(it) { "Failed to load the score details" }
                layoutState.value = HeartScoreDetailLayoutState.Error(layout = errorLayout())
            }
        }
    }

    /**
     * Reads the explanation once, then rebuilds the details whenever the score or the stored values
     * change. The chart, built once, follows the chosen range and the history by itself.
     */
    private suspend fun observe(scope: CoroutineScope) {
        val explainer = explainerSource.document(component = component)
        val chart = if (component == HeartScoreComponent.Diet || component == HeartScoreComponent.NicotineExposure) {
            null
        } else {
            chartMapper.map(
                input = HeartScoreChartInput(
                    component = component,
                    data = history(scope = scope),
                    selection = selection.asStateFlow(),
                    scope = scope,
                    actionSink = actionSink,
                ),
            )
        }
        combine(
            scoreSource.scores,
            debugSource.samples(component = component),
        ) { scores, debugSamples ->
            mapper.map(
                input = HeartScoreDetailContentInput(
                    result = scores.components.firstOrNull { it.component == component } ?: unscored(),
                    chart = chart,
                    explainer = explainer,
                    debugSamples = debugSamples,
                    actionSink = actionSink,
                ),
            )
        }.collect { layoutState.value = HeartScoreDetailLayoutState.Content(layout = it) }
    }

    /**
     * The history over the chosen range, read again whenever the range changes; the previous range's
     * history stays until the new one is read. `null` before the first read and after a failed one.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun history(scope: CoroutineScope): StateFlow<HeartScoreChartData?> = selection
        .flatMapLatest { range ->
            chartSource.updates(component = component, selection = range)
                .map { result ->
                    result.onFailure { error -> logger.w(error) { "The score history is unavailable" } }.getOrNull()
                }
                .catch { error ->
                    logger.w(error) { "The score history is unavailable" }
                    emit(null)
                }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    private fun unscored() = HeartScoreResult(
        component = component,
        score = null,
        measuredAt = null,
        measurement = null,
        usesAsianBodyMassIndex = false,
    )

    private fun onAction(action: HeartScoreDetailAction) {
        when (action) {
            HeartScoreDetailAction.Close -> navigator.push(
                event = NavigationEvent.Close(route = MHCRoute.HeartScoreDetail(component = component)),
            )
            HeartScoreDetailAction.Retry -> load()
            HeartScoreDetailAction.AddData -> addData()
            is HeartScoreDetailAction.RangeSelected -> {
                savedStateHandle[KEY_RANGE] = action.range.name
                selection.value = action.range
            }
            is HeartScoreDetailAction.LinkSelected -> navigator.push(event = NavigationEvent.OpenUrl(url = action.url))
        }
    }

    private fun addData() {
        val questionnaireName = when (component) {
            HeartScoreComponent.Diet -> "Diet"
            HeartScoreComponent.MentalWellbeing -> "WHO5"
            HeartScoreComponent.NicotineExposure -> "NicotineExposure"
            else -> null
        }
        if (questionnaireName == null) {
            navigator.push(event = NavigationEvent.NavigateTo(route = MHCRoute.HeartScoreEntry(component = component)))
        } else {
            viewModelScope.launch {
                questionnaires.questionnaire(name = questionnaireName).fold(
                    onSuccess = { taskPerformer.perform(task = PerformableTask.AnswerQuestionnaire(questionnaire = it)) },
                    onFailure = { layoutState.value = HeartScoreDetailLayoutState.Error(layout = errorLayout()) },
                )
            }
        }
    }

    private fun errorLayout() = GroveErrorLayout(
        title = StringResource(MHCStrings.heart_detail_error_title),
        message = StringResource(MHCStrings.heart_detail_error_message),
        primaryButton = AsyncTextButton(
            title = StringResource(MHCStrings.heart_health_error_retry),
            action = { actionSink.push(HeartScoreDetailAction.Retry) },
        ),
    )

    companion object {
        /**
         * The key the factor shown is passed under, as the name of a [HeartScoreComponent].
         */
        const val KEY_COMPONENT = "component"

        /**
         * The key the chosen chart range is kept under, as the name of a [HeartScoreChartRange].
         */
        const val KEY_RANGE = "chartRange"
    }
}
