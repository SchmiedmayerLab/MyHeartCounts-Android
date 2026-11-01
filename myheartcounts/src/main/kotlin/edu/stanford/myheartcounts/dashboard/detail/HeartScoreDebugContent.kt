//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.customsamples.CustomQuantitySample
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles

/**
 * Expandable observation fields for participants with debug mode enabled.
 */
data class HeartScoreDebugContent(
    val samples: List<CustomQuantitySample>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                if (samples.isEmpty()) Text(text = StringResource(MHCStrings.heart_detail_no_data).text())
                samples.forEach { sample ->
                    var expanded by rememberSaveable(sample.id.toString()) { mutableStateOf(false) }
                    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "${sample.timeRange.start}: ${sample.value} ${sample.type.unit.symbols.first()}")
                    }
                    if (expanded) {
                        Text(
                            text = StringResource(
                                MHCStrings.heart_detail_debug_fields,
                                sample.id.toString(), sample.type.id, sample.type.unit.symbols.first(),
                                sample.value.toString(), sample.timeRange.start.toString(), sample.timeRange.endExclusive.toString(),
                            ).text(),
                            style = TextStyles.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
