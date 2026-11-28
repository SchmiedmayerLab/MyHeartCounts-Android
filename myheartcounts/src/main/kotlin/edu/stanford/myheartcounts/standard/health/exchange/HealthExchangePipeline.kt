//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

/**
 * The record-centric upload path: [staging] converts and stages Records and deletions, [drain]
 * uploads what is staged.
 */
data class HealthExchangePipeline(
    val staging: HealthExchangeStaging,
    val drain: HealthExchangeDrain,
)
