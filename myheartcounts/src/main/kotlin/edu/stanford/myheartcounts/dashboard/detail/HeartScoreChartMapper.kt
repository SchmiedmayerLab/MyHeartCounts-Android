//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.chart.CalendarTimeAxisFormat
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart
import edu.stanford.myheartcounts.dashboard.ui.chart.ChartCard
import edu.stanford.myheartcounts.dashboard.ui.chart.Highlight
import edu.stanford.myheartcounts.dashboard.ui.chart.HighlightFormat
import edu.stanford.myheartcounts.dashboard.ui.chart.MenuPicker
import edu.stanford.myheartcounts.dashboard.ui.chart.NumberAxisFormat
import edu.stanford.myheartcounts.dashboard.ui.chart.TimeCoordinates
import edu.stanford.myheartcounts.dashboard.ui.chart.cartesianChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.StringResource
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * What a factor's chart shows.
 *
 * @property data the history over the chosen time range, or `null` while there is none to show
 * @property selection the time range the chart covers, chosen from its menu
 * @property scope the scope keeping the chart current, for as long as it is shown
 */
data class HeartScoreChartInput(
    val component: HeartScoreComponent,
    val data: StateFlow<HeartScoreChartData?>,
    val selection: StateFlow<HeartScoreChartRange>,
    val scope: CoroutineScope,
    val actionSink: ActionSink<HeartScoreDetailAction>,
)

/**
 * Lays out a factor's history chart.
 */
interface HeartScoreChartMapper {

    /**
     * The chart [input] describes, with a menu to change its time range, following [input]'s
     * history and selection; without a history, the chart is empty over the chosen time range.
     */
    fun map(input: HeartScoreChartInput): ChartCard
}

/**
 * Draws steps and sleep as bars from zero and every other factor as a line, blood pressure as one
 * line per reading, with the dates in the participant's time zone.
 */
class HeartScoreChartMapperImpl(
    private val timeProvider: TimeProvider,
    private val ranges: HeartScoreDetailRanges,
) : HeartScoreChartMapper {
    override fun map(input: HeartScoreChartInput): ChartCard {
        return ChartCard(
            title = StringResource(input.component.title),
            picker = input.selection
                .map { selection -> picker(input = input, selection = selection) }
                .stateIn(
                    scope = input.scope,
                    started = SharingStarted.Eagerly,
                    initialValue = picker(input = input, selection = input.selection.value),
                ),
            chart = combine(input.data, input.selection) { data, selection ->
                chart(component = input.component, data = data ?: empty(selection = selection))
            }.stateIn(
                scope = input.scope,
                started = SharingStarted.Eagerly,
                initialValue = chart(
                    component = input.component,
                    data = input.data.value ?: empty(selection = input.selection.value),
                ),
            ),
        )
    }

    private fun empty(selection: HeartScoreChartRange) = HeartScoreChartData(
        timeRange = ranges.range(selection = selection),
        series = emptyList(),
    )

    private fun picker(
        input: HeartScoreChartInput,
        selection: HeartScoreChartRange,
    ) = MenuPicker(
        label = StringResource(MHCStrings.heart_detail_time_range),
        options = HeartScoreChartRange.entries.map { StringResource(it.title) },
        selectedIndex = selection.ordinal,
        onSelected = { input.actionSink.push(HeartScoreDetailAction.RangeSelected(range = HeartScoreChartRange.entries[it])) },
    )

    private fun chart(
        component: HeartScoreComponent,
        data: HeartScoreChartData,
    ): CartesianChart {
        val coordinates = TimeCoordinates(origin = data.timeRange.start)
        val bars = component == HeartScoreComponent.StepCount || component == HeartScoreComponent.Sleep
        val locale = Locale.getDefault()
        return cartesianChart(
            emptyText = StringResource(MHCStrings.heart_detail_no_data),
            description = StringResource(MHCStrings.heart_detail_chart_description, StringResource(component.title)),
            accessibilityLabels = CartesianChart.AccessibilityLabels(
                nextPoint = StringResource(MHCStrings.heart_detail_next_point),
                previousPoint = StringResource(MHCStrings.heart_detail_previous_point),
            ),
        ) {
            xAxis(
                range = coordinates.range(start = data.timeRange.start, end = data.timeRange.endExclusive),
                format = CalendarTimeAxisFormat(
                    coordinates = coordinates,
                    zone = timeProvider.currentZone(),
                    locale = locale,
                ),
            )
            yAxis(
                range = CartesianChart.Range.fitting(
                    values = data.series.flatMap { it.points }.map { it.value }.filter { it.isFinite() },
                    includeZero = bars,
                ),
                format = NumberAxisFormat(locale = locale),
            )
            data.series.forEach { values ->
                series(
                    style = if (bars) CartesianChart.Style.Bar else CartesianChart.Style.Line,
                    color = when {
                        values.kind == HeartScoreChartSeries.Kind.Systolic || component == HeartScoreComponent.BloodLipids ->
                            DashboardColorPalette.red
                        component == HeartScoreComponent.StepCount -> DashboardColorPalette.orange
                        else -> DashboardColorPalette.blue
                    },
                    highlightFormat = HeartScoreHighlightFormat(
                        coordinates = coordinates,
                        zone = timeProvider.currentZone(),
                        locale = locale,
                        component = component,
                        kind = values.kind,
                    ),
                ) {
                    values.points.filter { it.value.isFinite() }.sortedBy { it.timeRange.start }.forEach {
                        point(point = coordinates.point(start = it.timeRange.start, end = it.timeRange.endExclusive, value = it.value))
                    }
                }
            }
        }
    }
}

/**
 * Labels a highlighted point with its value and unit, and titles it with its date.
 */
data class HeartScoreHighlightFormat(
    val coordinates: TimeCoordinates,
    val zone: ZoneId,
    val locale: Locale,
    val component: HeartScoreComponent,
    val kind: HeartScoreChartSeries.Kind,
) : HighlightFormat {
    override fun label(point: CartesianChart.Point): Highlight {
        val number = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = when (component) {
                HeartScoreComponent.BodyMassIndex -> 1
                HeartScoreComponent.StepCount, HeartScoreComponent.MentalWellbeing -> 0
                else -> 2
            }
            minimumFractionDigits = if (component == HeartScoreComponent.BodyMassIndex) 1 else 0
        }.format(point.y)
        val value = component.displayUnit?.let { "$number $it" } ?: number
        val formattedValue = if (component == HeartScoreComponent.Sleep) {
            val minutes = (point.y * MINUTES_PER_HOUR).toInt()
            StringResource(MHCStrings.heart_detail_sleep_duration, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
        } else {
            StringResource(value)
        }
        val label = when (kind) {
            HeartScoreChartSeries.Kind.Systolic -> StringResource(MHCStrings.heart_detail_systolic_value, value)
            HeartScoreChartSeries.Kind.Diastolic -> StringResource(MHCStrings.heart_detail_diastolic_value, value)
            HeartScoreChartSeries.Kind.Value -> formattedValue
        }
        return Highlight(
            value = label,
            title = title(point = point),
        )
    }

    private fun title(point: CartesianChart.Point): StringResource {
        val date = coordinates.instant(coordinate = point.x).atZone(zone)
        val end = point.xEnd?.let { coordinates.instant(coordinate = it).atZone(zone) }
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return if (component != HeartScoreComponent.Sleep && end != null && end.toLocalDate() != date.toLocalDate()) {
            StringResource(MHCStrings.heart_detail_date_range, formatter.format(date), formatter.format(end))
        } else {
            StringResource(formatter.format(date))
        }
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}

/**
 * The unit a factor's values are shown in, or `null` for a count or score without one.
 */
val HeartScoreComponent.displayUnit: String?
    get() = when (this) {
        HeartScoreComponent.ExerciseMinutes -> "min"
        HeartScoreComponent.Sleep -> "hr"
        HeartScoreComponent.BloodPressure -> "mmHg"
        HeartScoreComponent.BloodGlucose, HeartScoreComponent.BloodLipids -> "mg/dL"
        else -> null
    }
