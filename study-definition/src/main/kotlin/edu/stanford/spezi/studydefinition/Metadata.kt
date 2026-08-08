//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import edu.stanford.spezi.foundation.UUIDSerializer
import edu.stanford.spezi.studydefinition.internal.IconSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Non-content information about a study: identity, display text, participation gating, and the
 * consent document reference.
 *
 * Display text is localized; resolve it against a locale with [LocalizedText.resolve].
 * [enrollmentConditions] is absent when a study gates enrollment on nothing.
 */
@Serializable
data class Metadata(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val title: LocalizedText,
    val shortTitle: LocalizedText,
    val icon: Icon?,
    val explanationText: LocalizedText,
    val shortExplanationText: LocalizedText,
    @Serializable(with = UUIDSerializer::class)
    val studyDependency: UUID? = null,
    val participationCriterion: ParticipationCriterion,
    val enrollmentConditions: EnrollmentConditions? = null,
    val consentFileRef: FileReference?,
) {
    /**
     * An icon representing the study.
     */
    @Serializable(with = IconSerializer::class)
    sealed interface Icon {
        /**
         * A platform-provided symbol identified by [name].
         *
         * The name references an external symbol set and carries no meaning on its own; a UI layer
         * decides how, or whether, to render it.
         */
        @Serializable
        @SerialName("systemSymbol")
        // TODO: Clarify how to handle system symbols across platforms. For example, iOS has SF Symbols, but Android
        //  does not have a direct equivalent and we would require a mapping to Material Design icons or custom assets.
        //  We may need to define a cross-platform symbol set or provide guidelines for icon selection.
        data class SystemSymbol(val name: String) : Icon

        /**
         * A custom icon located at [url].
         */
        @Serializable
        @SerialName("custom")
        data class Custom(val url: String) : Icon
    }
}
