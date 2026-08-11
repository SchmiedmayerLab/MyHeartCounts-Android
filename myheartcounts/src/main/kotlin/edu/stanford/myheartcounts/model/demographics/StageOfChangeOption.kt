//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("SpacingBetweenDeclarationsWithAnnotations")

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A participant's behaviour-change stage.
 */
@Serializable
enum class StageOfChangeOption {
    @SerialName("0") NOT_SET,
    @SerialName("a") A,
    @SerialName("b") B,
    @SerialName("c") C,
    @SerialName("d") D,
    @SerialName("e") E,
    @SerialName("f") F,
    @SerialName("g") G,
    @SerialName("h") H,
    @SerialName("i") I,
}
