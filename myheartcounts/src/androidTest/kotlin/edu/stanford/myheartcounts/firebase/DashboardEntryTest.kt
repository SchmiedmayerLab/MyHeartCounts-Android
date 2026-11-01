//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.customsamples.CustomQuantitySample
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.customsamples.CustomSampleSource
import edu.stanford.myheartcounts.customsamples.CustomSampleUploader
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreBmiStore
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreEntry
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreRecordStore
import edu.stanford.myheartcounts.healthstats.HealthStatsAggregation
import edu.stanford.myheartcounts.healthstats.HealthStatsMetric
import edu.stanford.myheartcounts.healthstats.HealthStatsReadPolicy
import edu.stanford.myheartcounts.healthstats.HealthStatsRequest
import edu.stanford.myheartcounts.healthstats.HealthStatsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.grovealliance.core.requireDependency
import org.grovealliance.health.Health
import org.grovealliance.health.RecordType
import org.grovealliance.scheduler.InstantRange
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DashboardEntryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val date = Instant.now().minusSeconds(60)
    private val range = InstantRange(start = date.minusSeconds(7200), endExclusive = date.plusSeconds(120))

    @Before
    fun setup(): Unit = runBlocking {
        assumeTrue("Needs Firebase emulator build flag", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
        FirebaseTestSession.signUpFreshParticipant()
    }

    @After
    fun teardown(): Unit = runBlocking {
        if (FirebaseTestSession.isEmulatorConfigured) FirebaseTestSession.signOut()
    }

    @Test
    fun customUploadAndDirectBmiReachDashboardReaders(): Unit = runBlocking {
        // given
        val sample = CustomQuantitySample(
            id = UUID.randomUUID(), type = CustomQuantitySampleType.BloodLipids, value = 110.0,
            timeRange = InstantRange(start = date, endExclusive = date),
        )
        val uploader = requireDependency<CustomSampleUploader>()
        val store = requireDependency<HealthStatsStore>()
        val bmi = HeartScoreEntry.BodyMassIndex(
            id = UUID.randomUUID().toString(),
            date = date,
            value = 23.5,
            weightKg = null,
            heightMeters = null,
        )
        val month = YearMonth.from(date.atZone(ZoneId.systemDefault()))
        val document = FirebaseTestSession.firestore.healthStatsMonths(metricId = "bmi").document(month.toString())
        document.set(mapOf(
            "version" to 0, "metric" to "bmi", "samples" to mapOf("independent.source" to listOf(mapOf(
                "date" to date.minusSeconds(3600).toString(), "unit" to "count", "value" to 22.0,
            ))),
        )).await()

        // when
        uploader.upload(samples = listOf(sample)).getOrThrow()
        uploader.upload(samples = listOf(sample)).getOrThrow()
        requireDependency<HeartScoreBmiStore>().save(entry = bmi).getOrThrow()
        requireDependency<HeartScoreBmiStore>().save(entry = bmi).getOrThrow()

        // then
        val observations = withTimeout(30_000) {
            requireDependency<CustomSampleSource>().samples(type = sample.type, timeRange = range)
                .first { it.getOrThrow().any { value -> value.id == sample.id } }.getOrThrow()
        }
        assertThat(observations.single().value).isEqualTo(110.0)
        val snapshot = store.fetch(
            request = HealthStatsRequest.Quantity(
                metric = HealthStatsMetric.BodyMassIndex,
                timeRange = range,
                aggregation = HealthStatsAggregation.Avg,
            ),
            readPolicy = HealthStatsReadPolicy.Server,
        ).getOrThrow()
        assertThat(snapshot.elements.map { it.value }).containsExactly(22.0, 23.5)
        val raw = FirebaseTestSession.firestore.healthObservations(sampleTypeIdentifier = sample.type.id)
            .document(sample.id.toString()).get().await()
        assertThat(raw.getString("resourceType")).isEqualTo("Observation")
        assertThat(raw.get("extension")).isNotNull()
    }

    @Test
    fun namedDashboardQuestionnairesResolveFromTheShippedBundle(): Unit = runBlocking {
        // when / then
        val source = requireDependency<edu.stanford.myheartcounts.study.StudyQuestionnaireSource>()
        for (name in listOf("Diet", "WHO5", "NicotineExposure")) {
            val questionnaire = source.questionnaire(name = name).getOrThrow().questionnaire
            assertThat(questionnaire.item).isNotEmpty()
            assertThat(questionnaire.url).isNotEmpty()
        }
    }

    @Test
    fun registeredWritePermissionsSurviveRuntimeStudyUpdates(): Unit = runBlocking {
        // given
        val health = requireDependency<Health>()
        val expected = listOf(RecordType.bloodPressure, RecordType.bloodGlucose, RecordType.height, RecordType.weight)

        // when
        health.updateConfiguration { requestReadAccess(RecordType.steps) }

        // then
        assertThat(health.dataAccessRequirements.write).containsAtLeastElementsIn(expected)

        // when
        health.updateConfiguration { }

        // then
        assertThat(health.dataAccessRequirements.write).containsAtLeastElementsIn(expected)
    }

    @Test
    fun nativeEntriesSaveWithTheirValues(): Unit = runBlocking {
        assumeTrue(HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE)
        val client = HealthConnectClient.getOrCreate(context)
        val recordTypes = listOf(BloodGlucoseRecord::class, BloodPressureRecord::class, HeightRecord::class, WeightRecord::class)
        val permissions = recordTypes.flatMap { listOf(
            HealthPermission.getReadPermission(it),
            HealthPermission.getWritePermission(it),
        ) }.toSet()
        val newlyGranted = permissions - client.permissionController.getGrantedPermissions()
        val id = UUID.randomUUID().toString()
        val store = requireDependency<HeartScoreRecordStore>()
        try {
            newlyGranted.forEach { instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it) }
            store.save(entry = HeartScoreEntry.Quantity(
                id = id,
                date = date,
                component = HeartScoreComponent.BloodGlucose,
                value = 90.0,
            )).getOrThrow()
            store.save(entry = HeartScoreEntry.Pressure(
                id = id,
                date = date,
                systolic = 120.0,
                diastolic = 80.0,
            )).getOrThrow()
            store.save(entry = HeartScoreEntry.BodyMassIndex(
                id = id,
                date = date,
                value = 25.0,
                weightKg = 81.0,
                heightMeters = 1.8,
            )).getOrThrow()
            val filter = TimeRangeFilter.between(startTime = range.start, endTime = range.endExclusive)
            val glucose = client.readRecords(ReadRecordsRequest(recordType = BloodGlucoseRecord::class, timeRangeFilter = filter))
                .records.single { it.metadata.clientRecordId == id }
            assertThat(glucose.level.inMilligramsPerDeciliter).isEqualTo(90.0)
            val pressure = client.readRecords(ReadRecordsRequest(recordType = BloodPressureRecord::class, timeRangeFilter = filter))
                .records.single { it.metadata.clientRecordId == id }
            assertThat(pressure.systolic.inMillimetersOfMercury).isEqualTo(120.0)
            assertThat(pressure.diastolic.inMillimetersOfMercury).isEqualTo(80.0)
            val height = client.readRecords(ReadRecordsRequest(recordType = HeightRecord::class, timeRangeFilter = filter))
                .records.single { it.metadata.clientRecordId == "$id-height" }
            val weight = client.readRecords(ReadRecordsRequest(recordType = WeightRecord::class, timeRangeFilter = filter))
                .records.single { it.metadata.clientRecordId == "$id-weight" }
            assertThat(height.height.inMeters).isEqualTo(1.8)
            assertThat(weight.weight.inKilograms).isEqualTo(81.0)
        } finally {
            recordTypes.forEach { type ->
                client.deleteRecords(
                    recordType = type,
                    recordIdsList = emptyList(),
                    clientRecordIdsList = listOf(id, "$id-height", "$id-weight"),
                )
            }
        }
    }
}
