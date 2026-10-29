//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.internal.TileHeader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/**
 * A titled card containing an X/Y chart and an optional picker beside the title.
 *
 * The card keeps its place and size while the picker and the chart change.
 *
 * @param title the heading above the chart
 * @param picker the choice shown beside the heading; omitted while it is null
 * @param chart the chart below the heading's divider
 */
@Immutable
data class ChartCard(
    val title: StringResource,
    val picker: StateFlow<MenuPicker?>,
    val chart: StateFlow<CartesianChart>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val currentPicker by picker.collectAsStateWithLifecycle()
        val currentChart by chart.collectAsStateWithLifecycle()
        GroveCard(modifier = modifier, shape = GroveShapes.large) {
            Column(
                modifier = Modifier.padding(
                    start = Spacings.small,
                    end = Spacings.small,
                    bottom = Spacings.small,
                ),
            ) {
                TileHeader(
                    title = title,
                    trailing = currentPicker?.let { { it.Content() } },
                )
                currentChart.Content(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacings.small)
                        .height(CHART_HEIGHT),
                )
            }
        }
    }

    private companion object {
        /**
         * The height reserved for the chart beneath the heading.
         */
        val CHART_HEIGHT = 163.dp
    }
}

private class ChartCardPreviewProvider : PreviewParameterProvider<ChartCard> {
    private val start = Instant.parse("2026-10-11T00:00:00Z")
    private val coordinates = TimeCoordinates(origin = start)
    private val picker = MenuPicker(
        label = StringResource("Time range"),
        options = listOf(StringResource("Last 7 days"), StringResource("Last 14 days")),
        selectedIndex = 1,
        onSelected = {},
    )
    private val chart = CartesianChart(
        series = listOf(
            CartesianChart.Series(
                style = CartesianChart.Style.Bar,
                color = DashboardColorPalette.blue,
                points = listOf(
                    coordinates.point(start = start, end = start.plusSeconds(SECONDS_PER_DAY), value = 8.75),
                ),
                highlightFormat = null,
            ),
        ),
        xAxis = CartesianChart.Axis(
            range = coordinates.range(
                start = start,
                end = start.plusSeconds(SECONDS_PER_DAY * PREVIEW_DAYS),
            ),
            format = CalendarTimeAxisFormat(
                coordinates = coordinates,
                zone = ZoneOffset.UTC,
                locale = Locale.US,
            ),
        ),
        yAxis = CartesianChart.Axis(
            range = CartesianChart.Range(
                start = 0.0,
                end = 10.0,
            ),
            format = NumberAxisFormat(locale = Locale.US),
        ),
        emptyText = StringResource("No Data"),
        description = StringResource("Hours asleep over the last two weeks"),
        accessibilityLabels = CartesianChart.AccessibilityLabels(
            nextPoint = StringResource("Next point"),
            previousPoint = StringResource("Previous point"),
        ),
    )
    private val base = ChartCard(
        title = StringResource("Sleep"),
        picker = MutableStateFlow(picker),
        chart = MutableStateFlow(chart),
    )

    override val values: Sequence<ChartCard> = sequenceOf(
        base,
        base.copy(picker = MutableStateFlow(null)),
        base.copy(chart = MutableStateFlow(chart.copy(series = emptyList()))),
    )
}

/**
 * The number of seconds in a day of preview data.
 */
private const val SECONDS_PER_DAY = 86_400L

/**
 * The days spanned by the preview chart.
 */
private const val PREVIEW_DAYS = 15L

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(ChartCardPreviewProvider::class) content: ChartCard) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium).fillMaxWidth())
    }
}
