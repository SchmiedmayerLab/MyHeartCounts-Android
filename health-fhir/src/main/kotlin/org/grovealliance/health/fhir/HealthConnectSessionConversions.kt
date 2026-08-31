//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:OptIn(androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi::class)

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit

internal fun HealthConnectConverter.convertMindfulnessSession(
    record: MindfulnessSessionRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val minutes = mindfulnessDurationMinutes(record.startTime, record.endTime)
    val source = sourceIdentity(record.metadata, RecordType.mindfulnessSession.identifier)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    val observation = baseObservation(
        record.metadata,
        source,
        null,
        resolvedContext,
        "mindfulness-session",
    ).apply {
        claimMeasurementProfile(HealthConnectContract.MOBILE_MINDFULNESS_SESSION_PROFILE)
        addCategory(category(HealthConnectConverter.ACTIVITY_CATEGORY))
        code = concept(
            HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
            "mindfulness-session-duration",
            "Mindfulness session duration",
        )
        effective = Period().apply {
            startElement = DateTimeType(
                record.startTime.fhirDateTime(record.startZoneOffset, "Mindfulness start time"),
            )
            endElement = DateTimeType(
                record.endTime.fhirDateTime(record.endZoneOffset, "Mindfulness end time"),
            )
        }
        value = quantity(minutes, "min", "min")
        method = CodeableConcept(mindfulnessSessionTypeCoding(record.mindfulnessSessionType))
        retainSessionText(
            record.title,
            record.notes,
            RecordType.mindfulnessSession.identifier,
        )
    }
    return conversion(
        record.metadata,
        RecordType.mindfulnessSession.identifier,
        source,
        listOf(observation),
        convertedAt,
        eventSequence,
        resolvedContext,
    )
}

@Suppress("LongMethod")
internal fun HealthConnectConverter.convertSleepDuration(
    record: SleepSessionRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val nanos = ChronoUnit.NANOS.between(record.startTime, record.endTime)
    if (nanos <= 0L) throw InvalidHealthConnectRecord("SleepSessionRecord must have a positive interval.")
    val hours = BigDecimal.valueOf(nanos)
        .divide(HealthConnectConverter.NANOSECONDS_PER_HOUR, HealthConnectConverter.SESSION_DURATION_SCALE, RoundingMode.HALF_EVEN)
        .stripTrailingZeros()
        .withPlainScale()
    val source = sourceIdentity(record.metadata, RecordType.sleepSession.identifier)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    // Health Connect's platform list order is the only available stable slot identity for
    // otherwise-identical stages. Assign duplicate occurrences before presentation sorting;
    // sorting by a mutable value or iterating an unordered collection would change identities.
    val identifiedStages = assignSourceListOccurrences(record.stages) { stage ->
        Triple(stage.startTime, stage.endTime, stage.stage)
    }
    val stages = identifiedStages
        .sortedWith(
            compareBy(
                { it.first.startTime },
                { it.first.endTime },
                { it.first.stage },
                { it.second },
            ),
        )
        .map { (stage, occurrence) ->
            if (
                !stage.startTime.isBefore(stage.endTime) ||
                stage.startTime < record.startTime ||
                stage.endTime > record.endTime
            ) {
                throw InvalidHealthConnectRecord(
                    "Every SleepSessionRecord stage must be a positive interval inside its session.",
                )
            }
            val stageIdentity = sleepStageIdentifier(source, stage, occurrence)
            val stageCoding = sleepStageCoding(stage.stage)
            baseObservation(record.metadata, source, stageIdentity, resolvedContext).apply {
                claimMeasurementProfile(HealthConnectContract.MOBILE_SLEEP_STAGE_PROFILE)
                addCategory(category(HealthConnectConverter.ACTIVITY_CATEGORY))
                code = concept(
                    HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
                    "sleep-stage",
                    "Sleep stage",
                )
                effective = Period().apply {
                    startElement = DateTimeType(
                        stage.startTime.fhirDateTime(null, "Sleep-stage start time"),
                    )
                    endElement = DateTimeType(
                        stage.endTime.fhirDateTime(null, "Sleep-stage end time"),
                    )
                }
                value = CodeableConcept().apply {
                    addCoding(
                        Coding(
                            HealthConnectContract.GROVE_SLEEP_STAGE,
                            stageCoding.sharedCode,
                            stageCoding.sharedDisplay,
                        ),
                    )
                    addCoding(
                        Coding(
                            HealthConnectContract.HEALTH_CONNECT_SLEEP_STAGE,
                            stageCoding.sourceCode,
                            stageCoding.sourceDisplay,
                        ),
                    )
                }
            }
        }
    val summary = baseObservation(
        record.metadata,
        source,
        null,
        resolvedContext,
        "sleep-duration",
    ).apply {
        claimMeasurementProfile(HealthConnectContract.MOBILE_SLEEP_DURATION_PROFILE)
        addCategory(category(HealthConnectConverter.ACTIVITY_CATEGORY))
        code = concept(HealthConnectContract.LOINC, "93832-4", "Sleep duration")
        effective = Period().apply {
            startElement = DateTimeType(record.startTime.fhirDateTime(record.startZoneOffset, "Sleep start time"))
            endElement = DateTimeType(record.endTime.fhirDateTime(record.endZoneOffset, "Sleep end time"))
        }
        value = quantity(hours, "h", "h")
        stages.forEach { stage ->
            addHasMember(Reference(GroveExchangeIdentity.fullUrl(observationIdentity(stage))))
        }
        retainSessionText(
            record.title,
            record.notes,
            RecordType.sleepSession.identifier,
        )
    }
    return conversion(
        record.metadata,
        RecordType.sleepSession.identifier,
        source,
        listOf(summary) + stages,
        convertedAt,
        eventSequence,
        resolvedContext,
    )
}

internal fun HealthConnectConverter.convertExerciseSession(
    record: ExerciseSessionRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    if (!record.startTime.isBefore(record.endTime)) {
        throw InvalidHealthConnectRecord("ExerciseSessionRecord must have a positive interval.")
    }
    val source = sourceIdentity(record.metadata, RecordType.exerciseSession.identifier)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    val children = workoutSegments(record, source, resolvedContext) +
        workoutLaps(record, source, resolvedContext)
    val summary = baseObservation(
        record.metadata,
        source,
        null,
        resolvedContext,
        "workout",
    ).apply {
        claimMeasurementProfile(HealthConnectContract.MOBILE_WORKOUT_PROFILE)
        addCategory(category(HealthConnectConverter.ACTIVITY_CATEGORY))
        code = concept(HealthConnectContract.GROVE_MOBILE_MEASUREMENT, "workout", "Workout session")
        effective = Period().apply {
            startElement = DateTimeType(
                record.startTime.fhirDateTime(record.startZoneOffset, "Workout start time"),
            )
            endElement = DateTimeType(record.endTime.fhirDateTime(record.endZoneOffset, "Workout end time"))
        }
        value = codedValue(
            HealthConnectContract.GROVE_WORKOUT_ACTIVITY,
            HealthConnectContract.HEALTH_CONNECT_EXERCISE_TYPE,
            HealthConnectWorkoutVocabulary.activity(record.exerciseType),
        )
        children.forEach { child ->
            addHasMember(Reference(GroveExchangeIdentity.fullUrl(observationIdentity(child))))
        }
        retainSessionText(
            record.title,
            record.notes,
            RecordType.exerciseSession.identifier,
        )
    }
    return conversion(
        record.metadata,
        RecordType.exerciseSession.identifier,
        source,
        listOf(summary) + children,
        convertedAt,
        eventSequence,
        resolvedContext,
    )
}

internal fun HealthConnectConverter.workoutSegments(
    record: ExerciseSessionRecord,
    source: HealthConnectSourceIdentity,
    resolvedContext: ResolvedFhirContext,
): List<Observation> {
    val identifiedSegments = assignSourceListOccurrences(record.segments) { segment ->
        Triple(segment.startTime, segment.endTime, segment.segmentType)
    }
    return identifiedSegments
        .sortedWith(compareBy({ it.first.startTime }, { it.first.endTime }, { it.first.segmentType }, { it.second }))
        .map { (segment, occurrence) ->
            requireWorkoutInterval(record, segment.startTime, segment.endTime)
            val classification = HealthConnectWorkoutVocabulary.segment(segment.segmentType)
            workoutSegmentObservation(
                record.metadata,
                source,
                resolvedContext,
                segment.startTime,
                segment.endTime,
                occurrence,
                classification,
            ).apply {
                if (segment.repetitions > 0) {
                    addComponent().apply {
                        code = workoutStatistic("repetitions", "Repetitions")
                        value = quantity(segment.repetitions.toBigDecimal(), "{count}", "repetitions")
                    }
                }
            }
        }
}

internal fun HealthConnectConverter.workoutLaps(
    record: ExerciseSessionRecord,
    source: HealthConnectSourceIdentity,
    resolvedContext: ResolvedFhirContext,
): List<Observation> {
    val identifiedLaps = assignSourceListOccurrences(record.laps) { lap ->
        lap.startTime to lap.endTime
    }
    return identifiedLaps
        .sortedWith(compareBy({ it.first.startTime }, { it.first.endTime }, { it.second }))
        .map { (lap, occurrence) ->
            requireWorkoutInterval(record, lap.startTime, lap.endTime)
            workoutSegmentObservation(
                record.metadata,
                source,
                resolvedContext,
                lap.startTime,
                lap.endTime,
                occurrence,
                HealthConnectWorkoutVocabulary.lap(),
            ).apply {
                lap.length?.let { length ->
                    addComponent().apply {
                        code = workoutStatistic("lap-length", "Lap length")
                        value = quantity(length.inMeters.fhirDecimal("Workout lap length"), "m", "m")
                    }
                }
            }
        }
}

@Suppress("LongParameterList")
internal fun HealthConnectConverter.workoutSegmentObservation(
    metadata: Metadata,
    source: HealthConnectSourceIdentity,
    resolvedContext: ResolvedFhirContext,
    start: Instant,
    end: Instant,
    occurrence: Int,
    classification: WorkoutClassification,
): Observation {
    val identity = HealthConnectIdentity.segmentOutput(
        synchronizationScope.identityKey,
        source,
        start,
        end,
        classification.value.sourceCode,
        occurrence,
    )
    return baseObservation(metadata, source, identity, resolvedContext).apply {
        claimMeasurementProfile(HealthConnectContract.MOBILE_WORKOUT_SEGMENT_PROFILE)
        addCategory(category(HealthConnectConverter.ACTIVITY_CATEGORY))
        code = concept(
            HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
            "workout-segment",
            "Workout segment",
        )
        // Health Connect supplies offsets for the containing session, not for each segment or lap.
        effective = Period().apply {
            startElement = DateTimeType(start.fhirDateTime(null, "Workout-segment start time"))
            endElement = DateTimeType(end.fhirDateTime(null, "Workout-segment end time"))
        }
        value = codedValue(
            classification.sharedSystem,
            HealthConnectContract.HEALTH_CONNECT_EXERCISE_SEGMENT_TYPE,
            classification.value,
        )
    }
}

internal fun HealthConnectConverter.requireWorkoutInterval(record: ExerciseSessionRecord, start: Instant, end: Instant) {
    if (!start.isBefore(end) || start < record.startTime || end > record.endTime) {
        throw InvalidHealthConnectRecord(
            "Every ExerciseSessionRecord segment and lap must be a positive interval inside its session.",
        )
    }
}

internal fun HealthConnectConverter.workoutStatistic(code: String, display: String): CodeableConcept =
    concept(HealthConnectContract.GROVE_WORKOUT_STATISTIC, code, display)
