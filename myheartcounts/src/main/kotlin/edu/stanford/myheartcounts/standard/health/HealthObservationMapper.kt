//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Quantity
import java.time.Duration
import java.time.Instant
import java.util.Date

/**
 * Turns Health Connect records into the FHIR `Observation`s the study dataset is made of.
 *
 * The output has to match what iOS produces for the equivalent HealthKit sample closely enough that
 * an analyst can treat the two as one series: same code, same unit, same timing fields. Which
 * records may share a collection with iOS at all is decided by [HealthSampleType]; this only decides
 * what the resource looks like once they do.
 */
class HealthObservationMapper {

    /**
     * The `Observation`s [record] produces.
     *
     * Usually one, but a record that carries a series of readings — a [HeartRateRecord] holds every
     * beat measured over its interval — produces one per reading, matching HealthKit, which delivers
     * those as individual samples.
     *
     * @param record The record to convert.
     * @param sampleType How the record is named in the study dataset.
     * @param issuedAt When the app is uploading this, recorded on every resource as FHIR `issued`.
     * @return The resources, or an empty list when the record carries nothing to report.
     */
    fun map(record: Record, sampleType: HealthSampleType, issuedAt: Instant): List<Observation> =
        readingsOf(record = record).map { reading ->
            observation(
                record = record,
                sampleType = sampleType,
                issuedAt = issuedAt,
                reading = reading,
            ).also { observation ->
                if (record is BloodPressureRecord) addBloodPressure(observation, record)
            }
        }

    /**
     * Every measurement [record] carries.
     *
     * Most records hold one. A record that holds a *series* — Health Connect batches heart-rate and
     * speed readings into one record covering an interval — produces one per reading, matching
     * HealthKit, which delivers each of those as its own sample.
     */
    private fun readingsOf(record: Record): List<Reading> = when (record) {
        is HeartRateRecord -> record.samples.map { sample ->
            Reading(
                value = sample.beatsPerMinute.toDouble(),
                start = sample.time,
                end = sample.time,
                // The reading's own id has to stay stable across re-uploads and unique within the
                // series, and Health Connect only identifies the record as a whole.
                idSuffix = sample.time.toEpochMilli().toString(),
            )
        }

        is SpeedRecord -> record.samples.map { sample ->
            Reading(
                value = sample.speed.inMetersPerSecond,
                start = sample.time,
                end = sample.time,
                idSuffix = sample.time.toEpochMilli().toString(),
            )
        }

        else -> listOfNotNull(readingOf(record = record))
    }

    /**
     * What [record] measured and when, or `null` for a record type this app does not read.
     *
     * The per-type branches are unavoidable: Health Connect's `IntervalRecord` and
     * `InstantaneousRecord` are internal to the library, so neither the value nor the timing can be
     * read off a `Record` generically.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @OptIn(ExperimentalMindfulnessSessionApi::class)
    private fun readingOf(record: Record): Reading? = when (record) {
        is ActiveCaloriesBurnedRecord ->
            Reading(record.energy.inKilocalories, record.startTime, record.endTime)
        is BasalBodyTemperatureRecord ->
            Reading(record.temperature.inCelsius, record.time, record.time)
        is BasalMetabolicRateRecord ->
            Reading(record.basalMetabolicRate.inKilocaloriesPerDay, record.time, record.time)
        is HeartRateVariabilityRmssdRecord ->
            Reading(record.heartRateVariabilityMillis, record.time, record.time)
        is TotalCaloriesBurnedRecord ->
            Reading(record.energy.inKilocalories, record.startTime, record.endTime)
        is BloodGlucoseRecord ->
            Reading(record.level.inMilligramsPerDeciliter, record.time, record.time)
        // A blood pressure has no single reading; both go on as components below.
        is BloodPressureRecord -> Reading(null, record.time, record.time)
        is BodyFatRecord -> Reading(record.percentage.value, record.time, record.time)
        is BodyTemperatureRecord -> Reading(record.temperature.inCelsius, record.time, record.time)
        is DistanceRecord -> Reading(record.distance.inMeters, record.startTime, record.endTime)
        is FloorsClimbedRecord -> Reading(record.floors, record.startTime, record.endTime)
        // The backend's table expresses body height in centimeters; Health Connect reports meters.
        is HeightRecord ->
            Reading(record.height.inMeters * CENTIMETERS_PER_METER, record.time, record.time)
        is LeanBodyMassRecord -> Reading(record.mass.inKilograms, record.time, record.time)
        is OxygenSaturationRecord -> Reading(record.percentage.value, record.time, record.time)
        is RespiratoryRateRecord -> Reading(record.rate, record.time, record.time)
        is RestingHeartRateRecord ->
            Reading(record.beatsPerMinute.toDouble(), record.time, record.time)
        is StepsRecord -> Reading(record.count.toDouble(), record.startTime, record.endTime)
        is Vo2MaxRecord ->
            Reading(record.vo2MillilitersPerMinuteKilogram, record.time, record.time)
        is WeightRecord -> Reading(record.weight.inKilograms, record.time, record.time)
        is WheelchairPushesRecord ->
            Reading(record.count.toDouble(), record.startTime, record.endTime)
        // A session carries no reading of its own; its duration is the measurement.
        is ExerciseSessionRecord -> Reading(
            value = durationMinutes(start = record.startTime, end = record.endTime),
            start = record.startTime,
            end = record.endTime,
        )
        is MindfulnessSessionRecord -> Reading(
            value = durationMinutes(start = record.startTime, end = record.endTime),
            start = record.startTime,
            end = record.endTime,
        )
        is SleepSessionRecord -> Reading(
            value = durationMinutes(start = record.startTime, end = record.endTime),
            start = record.startTime,
            end = record.endTime,
        )
        else -> null
    }

    /**
     * One measurement: its value, if it has a single one, and the interval it covers.
     *
     * @param idSuffix Distinguishes this reading from the others in the same record, for a record
     * type that carries a series. `null` when the record is one measurement.
     */
    private data class Reading(
        val value: Double?,
        val start: Instant,
        val end: Instant,
        val idSuffix: String? = null,
    )

    private fun observation(
        record: Record,
        sampleType: HealthSampleType,
        issuedAt: Instant,
        reading: Reading,
    ): Observation = Observation().apply {
        id = listOfNotNull(record.metadata.id.ifEmpty { null }, reading.idSuffix)
            .joinToString(separator = "_")
            .ifEmpty { "${sampleType.identifier}_${reading.start.toEpochMilli()}" }
        status = Observation.ObservationStatus.FINAL
        issued = Date.from(issuedAt)

        val coding = codingFor(record = record, sampleType = sampleType)
        code = CodeableConcept().addCoding(
            Coding(coding.system, coding.code, coding.display),
        )

        // A zero-length interval is a point measurement, and FHIR expresses those as a dateTime
        // rather than a period of no length.
        effective = if (reading.start == reading.end) {
            DateTimeType(Date.from(reading.start))
        } else {
            Period().setStart(Date.from(reading.start)).setEnd(Date.from(reading.end))
        }

        if (reading.value != null && coding.unit != null) {
            setValue(
                Quantity()
                    .setValue(reading.value)
                    .setUnit(coding.unitDisplay)
                    .setSystem(HealthObservationCoding.UCUM_SYSTEM)
                    .setCode(coding.unit),
            )
        }
    }

    /**
     * Adds the systolic and diastolic readings, which FHIR models as components of one observation
     * rather than as two.
     */
    private fun addBloodPressure(observation: Observation, record: BloodPressureRecord) {
        listOf(
            SYSTOLIC_COMPONENT to record.systolic.inMillimetersOfMercury,
            DIASTOLIC_COMPONENT to record.diastolic.inMillimetersOfMercury,
        ).forEach { (key, reading) ->
            val coding = HealthObservationCoding.forSampleType(sampleTypeIdentifier = key) ?: return@forEach
            observation.addComponent(
                Observation.ObservationComponentComponent()
                    .setCode(CodeableConcept().addCoding(Coding(coding.system, coding.code, coding.display)))
                    .setValue(
                        Quantity()
                            .setValue(reading)
                            .setUnit(coding.unitDisplay)
                            .setSystem(HealthObservationCoding.UCUM_SYSTEM)
                            .setCode(coding.unit),
                    ),
            )
        }
    }

    /**
     * The coding for [sampleType], falling back to an Android-native one when the backend defines no
     * code for it — see [HealthObservationCoding.forHealthConnectRecord].
     */
    private fun codingFor(record: Record, sampleType: HealthSampleType): HealthObservationCoding =
        HealthObservationCoding.forSampleType(sampleTypeIdentifier = sampleType.identifier)
            ?: HealthObservationCoding.forHealthConnectRecord(
                recordTypeIdentifier = record::class.simpleName.orEmpty(),
            )

    private fun durationMinutes(start: Instant, end: Instant): Double =
        Duration.between(start, end).toMillis().toDouble() / MILLIS_PER_MINUTE

    private companion object {
        const val CENTIMETERS_PER_METER = 100.0
        const val MILLIS_PER_MINUTE = 60_000.0
    }
}
