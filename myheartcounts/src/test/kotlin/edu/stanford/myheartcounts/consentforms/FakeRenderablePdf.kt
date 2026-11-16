//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import android.graphics.Bitmap

/**
 * A [RenderablePdf] with the pages set through [setPageAspectRatios] (none by default), which
 * draws none of them and records whether it was closed.
 */
class FakeRenderablePdf : RenderablePdf {
    private var aspectRatios: List<Float> = emptyList()

    /**
     * Whether [close] was called.
     */
    var isClosed = false
        private set

    override val pageAspectRatios: List<Float> get() = aspectRatios

    /**
     * Makes the PDF have pages of [aspectRatios].
     */
    fun setPageAspectRatios(aspectRatios: List<Float>) {
        this.aspectRatios = aspectRatios
    }

    override suspend fun page(index: Int, width: Int): Result<Bitmap> =
        Result.failure(UnsupportedOperationException("Pages aren't drawn in unit tests."))

    override fun close() {
        isClosed = true
    }
}
