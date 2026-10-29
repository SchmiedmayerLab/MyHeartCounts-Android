//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.customsamples.CustomSampleSource
import edu.stanford.myheartcounts.healthstats.HealthStatsAggregation
import edu.stanford.myheartcounts.healthstats.HealthStatsMetric
import edu.stanford.myheartcounts.healthstats.HealthStatsReadPolicy
import edu.stanford.myheartcounts.healthstats.HealthStatsRequest
import edu.stanford.myheartcounts.healthstats.HealthStatsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.grovealliance.core.requireDependency
import org.grovealliance.scheduler.InstantRange
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

/**
 * Exercises dashboard listeners with server writes, updates and deletions for a fresh participant.
 */
@RunWith(AndroidJUnit4::class)
class DashboardFirestoreTest {
    private val range = InstantRange(
        start = Instant.now().minusSeconds(86_400),
        endExclusive = Instant.now().plusSeconds(86_400),
    )
    private val measuredAt = Instant.now()

    @Before
    fun signIn(): Unit = runBlocking {
        assumeTrue("Needs -Pmhc.firebaseEmulatorHost", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
        FirebaseTestSession.signUpFreshParticipant()
    }

    @After
    fun signOut() = runBlocking {
        if (FirebaseTestSession.isEmulatorConfigured) FirebaseTestSession.signOut()
    }

    @Test
    fun customObservationsDeliverAllFourTypesAndLiveChanges() = runBlocking {
        // given
        val source = requireDependency<CustomSampleSource>()
        val expectedValues = mapOf(
            CustomQuantitySampleType.DietScore to 17.0,
            CustomQuantitySampleType.MentalWellbeingScore to 20.0,
            CustomQuantitySampleType.NicotineExposure to 2.0,
            CustomQuantitySampleType.BloodLipids to 110.0,
        )

        for ((type, value) in expectedValues) {
            val id = UUID.randomUUID().toString()
            val path = "HealthObservations_${type.id}/$id"
            observe(flow = source.samples(type = type, timeRange = range)) { updates ->
                assertThat(updates.awaitMatching { it.getOrThrow().isEmpty() }.getOrThrow()).isEmpty()

                // when
                write(path = path, data = observation(id = id, type = type, value = value))

                // then
                val sample = updates.awaitMatching { it.getOrThrow().singleOrNull()?.value == value }.getOrThrow().single()
                assertThat(sample.id.toString()).isEqualTo(id)
                assertThat(sample.type).isEqualTo(type)
                assertThat(sample.timeRange.start).isEqualTo(measuredAt)

                // when
                write(path = path, data = observation(id = id, type = type, value = value + 1))

                // then
                updates.awaitMatching { it.getOrThrow().singleOrNull()?.value == value + 1 }

                // when
                write(path = path, data = observation(
                    id = id,
                    type = type,
                    value = value,
                    date = range.start.minusSeconds(1),
                ))

                // then
                updates.awaitMatching { it.getOrThrow().isEmpty() }

                // when
                write(path = path, data = observation(id = id, type = type, value = value))
                updates.awaitMatching { it.getOrThrow().isNotEmpty() }
                delete(path = path)

                // then
                updates.awaitMatching { it.getOrThrow().isEmpty() }
            }
        }
    }

    @Test
    fun statsDeliverServerValuesAndLiveReplacementsAndDeletions() = runBlocking {
        // given
        val store = requireDependency<HealthStatsStore>()
        val request = HealthStatsRequest.Quantity(
            metric = HealthStatsMetric.Steps,
            timeRange = range,
            aggregation = HealthStatsAggregation.Sum,
        )
        val month = YearMonth.from(measuredAt.atZone(ZoneOffset.UTC))
        val path = "stats/steps/months/$month"
        observe(flow = store.updates(request = request)) { updates ->
            updates.awaitMatching { it.getOrThrow().elements.isEmpty() && !it.getOrThrow().isFromCache }

            // when
            write(path = path, data = steps(value = 1234))

            // then
            val first = updates.awaitMatching {
                it.getOrThrow().elements.singleOrNull()?.value == 1234.0 && !it.getOrThrow().isFromCache
            }.getOrThrow()
            assertThat(first.diagnostics).isEmpty()
            assertThat(first.hasPendingWrites).isFalse()
            assertThat(first.contributingSourceIds).containsExactly("com.apple.HealthKit")
            val fetched = store.fetch(request = request, readPolicy = HealthStatsReadPolicy.Server).getOrThrow()
            assertThat(fetched.elements).isEqualTo(first.elements)
            assertThat(fetched.isFromCache).isFalse()

            // when
            write(path = path, data = steps(value = 5678))

            // then
            updates.awaitMatching { it.getOrThrow().elements.singleOrNull()?.value == 5678.0 }

            // when
            delete(path = path)

            // then
            updates.awaitMatching { it.getOrThrow().elements.isEmpty() && !it.getOrThrow().isFromCache }
        }
    }

    private fun observation(
        id: String,
        type: CustomQuantitySampleType,
        value: Double,
        date: Instant = measuredAt,
    ): Map<String, Any> = mapOf(
        "resourceType" to "Observation",
        "id" to id,
        "status" to "final",
        "code" to mapOf("coding" to listOf(mapOf("code" to type.id, "system" to "https://spezi.stanford.edu"))),
        "effectiveDateTime" to date.toString(),
        "valueQuantity" to mapOf(
            "value" to value,
            "unit" to if (type == CustomQuantitySampleType.BloodLipids) "mg/dL" else "count",
        ),
    )

    private fun steps(value: Int): Map<String, Any> = mapOf(
        "version" to 0,
        "metric" to "steps",
        "hourly" to mapOf("com.apple.HealthKit" to listOf(mapOf(
            "start" to measuredAt.minusSeconds(3600).toString(),
            "end" to measuredAt.toString(),
            "unit" to "count",
            "sum" to value,
        ))),
    )

    private suspend fun <T> observe(flow: Flow<T>, block: suspend (Channel<T>) -> Unit) = coroutineScope {
        val updates = Channel<T>(capacity = Channel.UNLIMITED)
        val job = launch(context = Dispatchers.Default) { flow.collect { updates.send(it) } }
        try {
            withTimeout(30_000) { block(updates) }
        } finally {
            job.cancel()
            updates.cancel()
        }
    }

    private suspend fun <T> Channel<T>.awaitMatching(predicate: (T) -> Boolean): T {
        while (true) {
            val value = receive()
            if (predicate(value)) return value
        }
    }

    private suspend fun write(path: String, data: Map<String, Any>) {
        request(method = "PATCH", path = path, body = JSONObject().put("fields", fields(data)).toString())
    }

    private suspend fun delete(path: String) {
        request(method = "DELETE", path = path, body = null)
    }

    private fun fields(data: Map<*, *>): JSONObject = JSONObject().apply {
        data.forEach { (key, value) -> put(key.toString(), field(value)) }
    }

    private fun field(value: Any?): JSONObject = when (value) {
        is String -> JSONObject().put("stringValue", value)
        is Int -> JSONObject().put("integerValue", value.toString())
        is Double -> JSONObject().put("doubleValue", value)
        is Map<*, *> -> JSONObject().put("mapValue", JSONObject().put("fields", fields(value)))
        is List<*> -> JSONObject().put("arrayValue", JSONObject().put("values", JSONArray(value.map { field(it) })))
        else -> error("Unsupported fixture value: $value")
    }

    private suspend fun request(method: String, path: String, body: String?) = withContext(Dispatchers.IO) {
        check(FirebaseTestSession.isEmulatorConfigured)
        val host = requireNotNull(requireDependency<MHCFirebaseEmulator>().host)
        val project = FirebaseApp.getInstance().options.projectId
        val account = FirebaseTestSession.firestore.accountId()
        val connection = URL(
            "http://$host:${MHCFirebaseEmulator.FIRESTORE_PORT}/v1/projects/$project/" +
                "databases/(default)/documents/users/$account/$path",
        ).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Authorization", "Bearer owner")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            check(connection.responseCode in 200..299) {
                "$method $path: ${connection.responseCode} ${connection.errorStream?.bufferedReader()?.use { it.readText() }}"
            }
        } finally {
            connection.disconnect()
        }
    }
}
