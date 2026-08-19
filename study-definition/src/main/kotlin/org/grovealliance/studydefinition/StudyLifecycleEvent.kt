//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.grovealliance.foundation.UUIDSerializer
import org.grovealliance.studydefinition.internal.StudyLifecycleEventSerializer
import java.util.UUID

/**
 * An event that can happen over the course of a study's lifecycle, used to anchor event-based
 * component schedules.
 */
@Serializable(with = StudyLifecycleEventSerializer::class)
sealed interface StudyLifecycleEvent {
    /**
     * The participant's first enrollment into the study. Fired once, and only when enrolling for the
     * current day.
     */
    @Serializable
    @SerialName("enrollment")
    data object Enrollment : StudyLifecycleEvent

    /**
     * The study being set up on the participant's device, occurring on the enrollment date so that
     * setting up again resumes the participant's timeline rather than restarting it.
     */
    @Serializable
    @SerialName("activation")
    data object Activation : StudyLifecycleEvent

    /**
     * The participant's unenrollment from the study.
     */
    @Serializable
    @SerialName("unenrollment")
    data object Unenrollment : StudyLifecycleEvent

    /**
     * The official end of the study.
     */
    @Serializable
    @SerialName("studyEnd")
    data object StudyEnd : StudyLifecycleEvent

    /**
     * The completion of a scheduled occurrence of the component identified by [componentId].
     */
    @Serializable
    @SerialName("completedTask")
    data class CompletedTask(
        @Serializable(with = UUIDSerializer::class)
        val componentId: UUID,
    ) : StudyLifecycleEvent
}
