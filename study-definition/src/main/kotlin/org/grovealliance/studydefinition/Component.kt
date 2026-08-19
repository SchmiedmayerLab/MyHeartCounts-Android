//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.grovealliance.foundation.InstantSerializer
import org.grovealliance.foundation.UUIDSerializer
import org.grovealliance.studydefinition.internal.AttosecondDurationSerializer
import org.grovealliance.studydefinition.internal.ComponentSerializer
import org.grovealliance.studydefinition.internal.DisplayTextSerializer
import org.grovealliance.studydefinition.internal.HistoricalDataCollectionSerializer
import org.grovealliance.studydefinition.internal.HistoricalStartDateSerializer
import org.grovealliance.studydefinition.internal.SampleTypesSerializer
import org.grovealliance.studydefinition.internal.TimedWalkingTestKindSerializer
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A schedulable or background unit of a study.
 *
 * [Kind.USER_INTERACTIVE] components are surfaced to the participant and become scheduler tasks;
 * [Kind.INTERNAL] components (health data collection) run in the background and are implicitly
 * active for the whole enrollment.
 */
@Serializable(with = ComponentSerializer::class)
sealed interface Component {
    /**
     * The component's stable identity within the study.
     */
    val id: UUID

    /**
     * Whether the component is surfaced to the participant or runs internally.
     */
    val kind: Kind
        get() = when (this) {
            is Informational, is Questionnaire, is TimedWalkingTest, is CustomActiveTask -> Kind.USER_INTERACTIVE
            is HealthDataCollection -> Kind.INTERNAL
        }

    /**
     * Whether a component is participant-facing or internal.
     */
    enum class Kind {
        USER_INTERACTIVE,
        INTERNAL,
    }

    /**
     * Prompts the participant to read an informational article resolved from [fileRef].
     */
    @Serializable
    @SerialName("informational")
    data class Informational(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        val fileRef: FileReference,
    ) : Component

    /**
     * Prompts the participant to answer the questionnaire resolved from [fileRef].
     */
    @Serializable
    @SerialName("questionnaire")
    data class Questionnaire(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        val fileRef: FileReference,
    ) : Component

    /**
     * Configures background collection of health data.
     *
     * [optionalSampleTypes] are collected only when already authorized; [sampleTypes] drive an
     * authorization request on enrollment.
     */
    @Serializable
    @SerialName("healthDataCollection")
    data class HealthDataCollection(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = SampleTypesSerializer::class)
        val sampleTypes: List<String>,
        @Serializable(with = SampleTypesSerializer::class)
        val optionalSampleTypes: List<String>,
        val historicalDataCollection: HistoricalDataCollection,
    ) : Component {
        /**
         * Whether, and from when, historical data is collected in addition to new data.
         */
        @Serializable(with = HistoricalDataCollectionSerializer::class)
        sealed interface HistoricalDataCollection {
            /**
             * Only newly added data is collected.
             */
            @Serializable
            @SerialName("disabled")
            data object Disabled : HistoricalDataCollection

            /**
             * Historical data is collected from [startDate] onwards, in addition to new data.
             */
            @Serializable
            @SerialName("enabled")
            data class Enabled(
                val startDate: StartDate,
            ) : HistoricalDataCollection

            /**
             * The point from which historical data is collected.
             */
            @Serializable(with = HistoricalStartDateSerializer::class)
            sealed interface StartDate {
                /**
                 * A start that is relative to the moment collection begins, reaching back by
                 * [duration] (for example the last ten years).
                 */
                @Serializable
                @SerialName("last")
                data class Last(val duration: DateComponents) : StartDate

                /**
                 * A start fixed at [instant], independent of when collection begins.
                 */
                @Serializable
                @SerialName("absolute")
                data class Absolute(
                    @Serializable(with = InstantSerializer::class)
                    val instant: Instant,
                ) : StartDate
            }
        }
    }

    /**
     * Prompts the participant to perform a timed walking or running [test].
     */
    @Serializable
    @SerialName("timedWalkingTest")
    data class TimedWalkingTest(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        val test: TimedWalkingTestConfiguration,
    ) : Component

    /**
     * Prompts the participant to perform a custom [activeTask].
     */
    @Serializable
    @SerialName("customActiveTask")
    data class CustomActiveTask(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        val activeTask: ActiveTask,
    ) : Component {
        /**
         * A custom active task, identified by [identifier] and displayed with [title] and [subtitle].
         */
        @Serializable
        data class ActiveTask(
            val identifier: String,
            @Serializable(with = DisplayTextSerializer::class)
            val title: String,
            @Serializable(with = DisplayTextSerializer::class)
            val subtitle: String?,
        )
    }
}

/**
 * A timed walking or running test of a given [duration] and [kind].
 */
@Serializable
data class TimedWalkingTestConfiguration(
    @Serializable(with = AttosecondDurationSerializer::class)
    val duration: Duration,
    val kind: Kind,
) {
    /**
     * Whether the test observes walking or running.
     */
    @Serializable(with = TimedWalkingTestKindSerializer::class)
    enum class Kind {
        WALKING,
        RUNNING,
    }

    /**
     * A generated display title such as "6-Minute Walk Test".
     */
    val displayTitle: String
        get() {
            val minutes = duration.toMinutes()
            val activity = when (kind) {
                Kind.WALKING -> "Walk"
                Kind.RUNNING -> "Run"
            }
            return "$minutes-Minute $activity Test"
        }
}
