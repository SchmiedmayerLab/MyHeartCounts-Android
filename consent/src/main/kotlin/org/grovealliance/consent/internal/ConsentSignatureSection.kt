//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.consent.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.grovealliance.consent.SignatureMetadata
import org.grovealliance.consent.SignaturePoint
import org.grovealliance.consent.SignatureStroke
import org.grovealliance.resources.Strings
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.GroveIconButtonComposable
import org.grovealliance.ui.GroveInputFieldComposable
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.medium

internal data class ConsentSignatureSection(
    val id: String,
    val dateText: String,
    val metadata: Flow<SignatureMetadata>,
    val onFirstNameChanged: (String) -> Unit,
    val onLastNameChanged: (String) -> Unit,
    val onSignatureStrokesChanged: (List<SignatureStroke>) -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val metadata by metadata.collectAsStateWithLifecycle(SignatureMetadata.Empty)

        GroveCard(modifier = modifier) {
            Column(modifier = Modifier.fillMaxWidth()) {
                NameRow(
                    label = stringResource(Strings.consent_given_name_label),
                    value = metadata.givenName,
                    onValueChange = onFirstNameChanged,
                    placeholder = stringResource(Strings.consent_given_name_placeholder),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                NameRow(
                    label = stringResource(Strings.consent_family_name_label),
                    value = metadata.familyName,
                    onValueChange = onLastNameChanged,
                    placeholder = stringResource(Strings.consent_family_name_placeholder),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                Text(
                    text = stringResource(Strings.consent_signature_section_title).uppercase(),
                    modifier = Modifier
                        .padding(horizontal = Spacings.medium)
                        .padding(top = Spacings.medium, bottom = Spacings.extraSmall),
                    style = TextStyles.labelMedium.medium(),
                    color = Colors.onSurfaceVariant,
                )
                SignaturePad(
                    metadata = metadata,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacings.medium)
                        .padding(bottom = Spacings.medium),
                )
            }
        }
    }

    @Composable
    private fun SignaturePad(
        metadata: SignatureMetadata,
        modifier: Modifier = Modifier,
    ) {
        val strokeColor = Colors.onSurface
        val pendingPoints = remember { mutableStateListOf<SignaturePoint>() }

        Box(
            modifier = modifier
                .height(SIGNATURE_PAD_HEIGHT_DP.dp)
                .clip(GroveShapes.large)
                .background(Colors.surfaceVariant)
                .border(Sizes.Border.small, Colors.outline, GroveShapes.large),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(metadata.strokes) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                pendingPoints.clear()
                                val signaturePoint = offset.toSignaturePointOrNull(size) ?: return@detectDragGestures
                                pendingPoints.add(signaturePoint)
                            },
                            onDrag = { change, _ ->
                                val signaturePoint = change.position.toSignaturePointOrNull(size) ?: return@detectDragGestures
                                pendingPoints.add(signaturePoint)
                            },
                            onDragEnd = {
                                if (pendingPoints.isNotEmpty()) {
                                    val stroke = SignatureStroke(pendingPoints.toList())
                                    pendingPoints.clear()
                                    onSignatureStrokesChanged(metadata.strokes + stroke)
                                }
                            },
                            onDragCancel = { pendingPoints.clear() },
                        )
                    },
            ) {
                val allStrokes = metadata.strokes + listOf(SignatureStroke(pendingPoints.toList()))

                for (stroke in allStrokes) {
                    if (stroke.points.size < 2) continue

                    val path = Path()
                    path.moveTo(stroke.points.first().x, stroke.points.first().y)

                    for (point in stroke.points.drop(1)) {
                        path.lineTo(point.x, point.y)
                    }

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = STROKE_WIDTH_DP.dp.toPx()),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = Spacings.medium)
                    .padding(bottom = Spacings.small),
                verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
            ) {
                Text(text = "X", color = Colors.onSurfaceVariant)

                HorizontalDivider(color = Colors.onSurfaceVariant)

                val fullName = listOf(metadata.givenName, metadata.familyName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = fullName,
                        modifier = Modifier.weight(1f),
                        style = TextStyles.labelMedium,
                        color = Colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = dateText,
                        style = TextStyles.labelMedium,
                        color = Colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            AnimatedVisibility(
                visible = metadata.strokes.isNotEmpty(),
                modifier = Modifier.align(Alignment.TopEnd),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                GroveIconButtonComposable(
                    image = ImageResource(Icons.AutoMirrored.Filled.Undo),
                    onClick = {
                        pendingPoints.clear()
                        onSignatureStrokesChanged(metadata.strokes.dropLast(1))
                    },
                )
            }
        }
    }

    @Composable
    private fun NameRow(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            Text(
                text = label,
                modifier = Modifier.width(NAME_LABEL_WIDTH_DP.dp),
                style = TextStyles.bodyMedium.medium(),
            )
            GroveInputFieldComposable(
                modifier = Modifier.weight(1f),
                value = value,
                placeholder = placeholder,
                showBorder = false,
                onValueChanged = onValueChange,
            )
        }
    }
}

private const val NAME_LABEL_WIDTH_DP = 80
private const val SIGNATURE_PAD_HEIGHT_DP = 160
private const val STROKE_WIDTH_DP = 2

private fun Offset.toSignaturePointOrNull(bounds: IntSize): SignaturePoint? {
    if (x !in 0f..bounds.width.toFloat()) return null
    if (y !in 0f..bounds.height.toFloat()) return null

    return SignaturePoint(x = x, y = y)
}

@ThemePreviews
@Composable
private fun ConsentSignatureContentFilledPreview() {
    GroveTheme {
        ConsentSignatureSection(
            id = "sig",
            dateText = "01.01.2026",
            metadata = flowOf(SignatureMetadata(givenName = "Leland", familyName = "Stanford", strokes = emptyList())),
            onFirstNameChanged = {},
            onLastNameChanged = {},
            onSignatureStrokesChanged = {},
        ).Content(modifier = Modifier.fillMaxWidth())
    }
}

@ThemePreviews
@Composable
private fun ConsentSignatureContentEmptyPreview() {
    GroveTheme {
        ConsentSignatureSection(
            id = "sig",
            dateText = "01.01.2026",
            metadata = flowOf(SignatureMetadata.Empty),
            onFirstNameChanged = {},
            onLastNameChanged = {},
            onSignatureStrokesChanged = {},
        ).Content(modifier = Modifier.fillMaxWidth())
    }
}
