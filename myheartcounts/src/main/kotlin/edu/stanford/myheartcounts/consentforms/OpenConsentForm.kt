//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

/**
 * A signed form [download]ed onto the device and opened as a [pdf] whose pages can be drawn.
 */
data class OpenConsentForm(
    val download: DownloadedConsentForm,
    val pdf: RenderablePdf,
)
