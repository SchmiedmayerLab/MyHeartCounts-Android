//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.grovealliance.core.coroutines.runCatchingAsync
import java.io.File

/**
 * Draws the pages of PDF files as images.
 */
interface PdfPageRenderer {

    /**
     * Opens the PDF [file], whose pages can then be drawn until the returned document is closed.
     */
    suspend fun open(file: File): Result<RenderablePdf>
}

/**
 * An open PDF whose pages can be drawn.
 */
interface RenderablePdf {

    /**
     * The width-to-height ratio of each page, in page order.
     */
    val pageAspectRatios: List<Float>

    /**
     * The page at [index] drawn on white, [width] pixels wide.
     */
    suspend fun page(index: Int, width: Int): Result<Bitmap>

    /**
     * Releases the file; no page can be drawn afterwards.
     */
    fun close()
}

/**
 * [PdfPageRenderer] drawing with the platform's [PdfRenderer], one page at a time. Closing waits
 * for a page being drawn.
 */
class PdfPageRendererImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : PdfPageRenderer {

    override suspend fun open(file: File): Result<RenderablePdf> = runCatchingAsync {
        withContext(ioDispatcher) {
            val renderer = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
            PlatformRenderablePdf(
                renderer = renderer,
                pageAspectRatios = (0 until renderer.pageCount).map { index ->
                    renderer.openPage(index).use { it.width.toFloat() / it.height }
                },
                ioDispatcher = ioDispatcher,
            )
        }
    }

    private class PlatformRenderablePdf(
        private val renderer: PdfRenderer,
        override val pageAspectRatios: List<Float>,
        private val ioDispatcher: CoroutineDispatcher,
    ) : RenderablePdf {
        private val lock = Any()
        private var isClosed = false

        override suspend fun page(index: Int, width: Int): Result<Bitmap> = runCatchingAsync {
            withContext(ioDispatcher) {
                synchronized(lock) {
                    check(!isClosed) { "The PDF is closed." }
                    renderer.openPage(index).use { page ->
                        val height = (width / pageAspectRatios[index]).toInt().coerceAtLeast(1)
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(Color.WHITE)
                            page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }
        }

        override fun close() {
            synchronized(lock) {
                if (!isClosed) {
                    isClosed = true
                    renderer.close()
                }
            }
        }
    }
}
