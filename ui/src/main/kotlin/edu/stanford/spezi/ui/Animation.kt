//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/**
 * Horizontal slide for forward navigation: the incoming content enters from the right while the
 * outgoing content exits to the left.
 */
val horizontalSlideForward: ContentTransform =
    slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }

/**
 * Horizontal slide for backward navigation: the incoming content enters from the left while the
 * outgoing content exits to the right.
 */
val horizontalSlideBackward: ContentTransform =
    slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }

/**
 * Cross-fade animation when switching between contents / navigation routes
 */
val crossFade: ContentTransform = fadeIn() togetherWith fadeOut()

/**
 * Modal present: the incoming content slides up from the bottom and is drawn above the outgoing
 * content, which is held in place underneath.
 */
val verticalModalEnter: ContentTransform =
    (slideInVertically { height -> height } togetherWith fadeOut(targetAlpha = 0f))
        .apply { targetContentZIndex = 1f }

/**
 * Modal dismiss: the outgoing content slides down to the bottom while staying drawn above the
 * incoming content, which is revealed in place underneath. The inverse of [verticalModalEnter].
 */
val verticalModalExit: ContentTransform =
    (fadeIn(initialAlpha = 1f) togetherWith slideOutVertically { height -> height })
        .apply { targetContentZIndex = -1f }
