//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import java.io.File

/**
 * A [PdfPageRenderer] opening every file as the PDF set through [setResult], an empty
 * [FakeRenderablePdf] by default.
 */
class FakePdfPageRenderer : PdfPageRenderer {
    private var result: Result<RenderablePdf> = Result.success(FakeRenderablePdf())

    /**
     * Makes every following open return [result].
     */
    fun setResult(result: Result<RenderablePdf>) {
        this.result = result
    }

    override suspend fun open(file: File): Result<RenderablePdf> = result
}
