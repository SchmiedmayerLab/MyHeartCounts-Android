//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("SpacingBetweenDeclarationsWithAnnotations")

package edu.stanford.myheartcounts.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The regional edition of the study a participant takes part in.
 *
 * A variant decides the localized study content, the regional forms and the public web pages. It
 * is independent of the Firebase project the participant's data is stored in.
 *
 * @property regionCode the ISO 3166-1 alpha-2 code of the country the variant runs in
 * @property homepage the variant's public homepage
 * @property privacyPolicy the variant's privacy policy
 */
@Serializable
enum class StudyVariant(
    val regionCode: String,
    val homepage: String,
    val privacyPolicy: String,
) {
    @SerialName("stanford") Stanford(
        regionCode = "US",
        homepage = "https://myheartcounts.stanford.edu",
        privacyPolicy = "https://myheartcounts.stanford.edu/privacy",
    ),
    @SerialName("imperial") Imperial(
        regionCode = "GB",
        homepage = "https://www.imperial.ac.uk/nhli/research/my-heart-counts/",
        privacyPolicy = "https://myheartcounts.stanford.edu/privacy",
    ),
}
