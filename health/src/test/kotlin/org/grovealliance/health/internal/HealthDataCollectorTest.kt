//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.internal

import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.response.ChangesResponse
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.testing.stubs.MutableStub
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import org.grovealliance.health.CollectionMode
import org.grovealliance.health.CollectionTimeRange
import org.grovealliance.health.HealthConstraint
import org.grovealliance.health.RecordType
import org.grovealliance.storage.local.LocalStorage
import org.grovealliance.storage.local.LocalStorageSetting
import org.junit.Test
import java.time.Instant

class HealthDataCollectorTest {
    @Test
    fun `first collection durably baselines and drains every boundary page`() = runTest {
        val client = FakeHealthConnectClient()
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        var page = 0
        client.overrides.getChanges = MutableStub {
            page += 1
            ChangesResponse(
                changes = listOf(UpsertionChange(stepRecord("2026-08-19T1$page:00:00Z"))),
                nextChangesToken = "baseline-page-$page",
                hasMore = page == 1,
                changesTokenExpired = false,
            )
        }
        val constraint = OrderedHealthConstraint()
        val collector = collector(client, tokenStore, constraint, backgroundScope)

        val response = collector.collectOnce()

        assertThat(response.hasMore).isFalse()
        assertThat(constraint.resyncCalls).isEqualTo(1)
        assertThat(constraint.events).containsExactly("upsert", "upsert").inOrder()
        assertThat(tokenStore.getState(RecordType.steps, collector.collectionScopeId)?.phase)
            .isEqualTo(ChangesTokenPhase.COMMITTED)
        assertThat(tokenStore.getToken(RecordType.steps, collector.collectionScopeId))
            .isEqualTo("baseline-page-2")
    }

    @Test
    fun `failed first baseline retains and retries the exact durable boundary`() = runTest {
        val client = FakeHealthConnectClient()
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        val constraint = FailFirstBaselineHealthConstraint()
        val collector = collector(client, tokenStore, constraint, backgroundScope)

        val failure = runCatching { collector.collectOnce() }.exceptionOrNull()
        val pending = requireNotNull(tokenStore.getState(RecordType.steps, collector.collectionScopeId))

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(pending.phase).isEqualTo(ChangesTokenPhase.PENDING_BASELINE)

        collector.collectOnce()

        assertThat(constraint.resyncCalls).isEqualTo(2)
        assertThat(tokenStore.getState(RecordType.steps, collector.collectionScopeId)?.phase)
            .isEqualTo(ChangesTokenPhase.COMMITTED)
    }

    @Test
    fun `does not advance the token when delivery fails`() = runTest {
        val client = FakeHealthConnectClient()
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        val initialToken = client.getChangesToken(ChangesTokenRequest(setOf(StepsRecord::class)))
        tokenStore.storeToken(RecordType.steps, initialToken)
        client.insertRecords(listOf(stepRecord("2026-08-19T17:00:00Z")))
        val constraint = FailOnceHealthConstraint()
        val collector = collector(client, tokenStore, constraint, backgroundScope)

        val failure = runCatching { collector.collectOnce() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(tokenStore.getToken(RecordType.steps)).isEqualTo(initialToken)

        collector.collectOnce()

        assertThat(constraint.newRecordCalls).isEqualTo(2)
        assertThat(tokenStore.getToken(RecordType.steps)).isNotEqualTo(initialToken)
    }

    @Test
    fun `captures changes made while an expired token is fully resynchronized`() = runTest {
        val client = FakeHealthConnectClient()
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        val expiredToken = client.getChangesToken(ChangesTokenRequest(setOf(StepsRecord::class)))
        tokenStore.storeToken(RecordType.steps, expiredToken)
        client.expireToken(expiredToken)
        val constraint = InsertDuringResyncHealthConstraint(client)

        collector(client, tokenStore, constraint, backgroundScope).collectOnce()

        assertThat(constraint.resyncCalls).isEqualTo(1)
        assertThat(constraint.deliveredRecords).isEqualTo(1)
        assertThat(tokenStore.getToken(RecordType.steps)).isNotEqualTo(expiredToken)
    }

    @Test
    fun `delivers upsertion and deletion in Health Connect page order`() = runTest {
        val client = FakeHealthConnectClient()
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        val token = client.getChangesToken(ChangesTokenRequest(setOf(StepsRecord::class)))
        tokenStore.storeToken(RecordType.steps, token)
        client.overrides.getChanges = MutableStub {
            ChangesResponse(
                changes = listOf(
                    UpsertionChange(stepRecord("2026-08-19T17:00:00Z")),
                    DeletionChange("source-record"),
                ),
                nextChangesToken = "next-token",
                hasMore = false,
                changesTokenExpired = false,
            )
        }
        val constraint = OrderedHealthConstraint()

        collector(client, tokenStore, constraint, backgroundScope).collectOnce()

        assertThat(constraint.events).containsExactly("upsert", "delete").inOrder()
    }

    @Test
    fun `manual collection drains every acknowledged page`() = runTest {
        val client = FakeHealthConnectClient()
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        val token = client.getChangesToken(ChangesTokenRequest(setOf(StepsRecord::class)))
        tokenStore.storeToken(RecordType.steps, token)
        var page = 0
        client.overrides.getChanges = MutableStub {
            page += 1
            ChangesResponse(
                changes = listOf(UpsertionChange(stepRecord("2026-08-19T1$page:00:00Z"))),
                nextChangesToken = "page-$page",
                hasMore = page == 1,
                changesTokenExpired = false,
            )
        }
        val constraint = OrderedHealthConstraint()

        collector(client, tokenStore, constraint, backgroundScope).collectUntilDrained()

        assertThat(constraint.events).containsExactly("upsert", "upsert").inOrder()
        assertThat(tokenStore.getToken(RecordType.steps)).isEqualTo("page-2")
    }

    @Test
    fun `reports an upsert that leaves the collection filter`() = runTest {
        val client = FakeHealthConnectClient()
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        val token = client.getChangesToken(ChangesTokenRequest(setOf(StepsRecord::class)))
        tokenStore.storeToken(RecordType.steps, token)
        client.overrides.getChanges = MutableStub {
            ChangesResponse(
                changes = listOf(UpsertionChange(stepRecord("2026-08-19T17:00:00Z"))),
                nextChangesToken = "next-token",
                hasMore = false,
                changesTokenExpired = false,
            )
        }
        val constraint = OrderedHealthConstraint()

        collector(client, tokenStore, constraint, backgroundScope, predicate = { false }).collectOnce()

        assertThat(constraint.events).containsExactly("excluded")
    }

    @Test
    fun `switching projection A to B to A forces baselines and fences the old collector`() = runTest {
        val client = FakeHealthConnectClient()
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        val firstA = ScopedHealthConstraint("projection-a")
        val projectionB = ScopedHealthConstraint("projection-b")
        val secondA = ScopedHealthConstraint("projection-a")
        val firstCollector = collector(client, tokenStore, firstA, backgroundScope)

        firstCollector.collectOnce()
        collector(client, tokenStore, projectionB, backgroundScope).collectOnce()

        val fenced = runCatching { firstCollector.collectOnce() }.exceptionOrNull()
        collector(client, tokenStore, secondA, backgroundScope).collectOnce()

        assertThat(fenced).isInstanceOf(IllegalStateException::class.java)
        assertThat(firstA.resyncCalls).isEqualTo(1)
        assertThat(projectionB.resyncCalls).isEqualTo(1)
        assertThat(secondA.resyncCalls).isEqualTo(1)
    }

    @Test
    fun `same projection label in two repositories has independent owner and token state`() = runTest {
        val client = FakeHealthConnectClient()
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        val repositoryA = ScopedHealthConstraint("shared-projection", "repository-a")
        val repositoryB = ScopedHealthConstraint("shared-projection", "repository-b")
        val collectorA = collector(client, tokenStore, repositoryA, backgroundScope)
        val collectorB = collector(client, tokenStore, repositoryB, backgroundScope)

        collectorA.collectOnce()
        collectorB.collectOnce()
        collectorA.collectOnce()

        assertThat(repositoryA.resyncCalls).isEqualTo(1)
        assertThat(repositoryB.resyncCalls).isEqualTo(1)
        assertThat(tokenStore.getState(RecordType.steps, "shared-projection", "repository-a")).isNotNull()
        assertThat(tokenStore.getState(RecordType.steps, "shared-projection", "repository-b")).isNotNull()
    }

    @Test
    fun `stop and join fences an in-flight baseline before token reset`() = runTest {
        val tokenStore = ChangesTokenStore(InMemoryLocalStorage())
        val constraint = BlockingBaselineHealthConstraint()
        val collector = collector(FakeHealthConnectClient(), tokenStore, constraint, backgroundScope)
        collector.startDataCollection()
        constraint.entered.await()

        collector.stopDataCollectionAndJoin()
        tokenStore.deleteToken(RecordType.steps, collector.collectionScopeId)

        assertThat(tokenStore.getState(RecordType.steps, collector.collectionScopeId)).isNull()
    }

    private fun collector(
        client: FakeHealthConnectClient,
        tokenStore: ChangesTokenStore,
        constraint: HealthConstraint,
        scope: CoroutineScope,
        predicate: ((Record) -> Boolean)? = null,
    ) = HealthDataCollector(
        recordType = RecordType.steps,
        deliverySetting = HealthDataCollectorDeliverySetting(CollectionMode.Manual, false),
        timeRange = CollectionTimeRange.NewRecords,
        predicate = predicate,
        tokenStore = tokenStore,
        scope = scope,
        healthConstraint = constraint,
        client = client,
    )

    private fun stepRecord(start: String) = StepsRecord(
        startTime = Instant.parse(start),
        startZoneOffset = null,
        endTime = Instant.parse(start).plusSeconds(60),
        endZoneOffset = null,
        count = 100,
        metadata = Metadata.autoRecorded(testDevice()),
    )

    private class FailOnceHealthConstraint : HealthConstraint {
        var newRecordCalls = 0

        override suspend fun <T : Record> handleNewRecords(addedRecords: Set<T>, type: RecordType<out T>) {
            newRecordCalls += 1
            if (newRecordCalls == 1) error("Sink did not persist the page.")
        }

        override suspend fun <T : Record> handleDeletedRecords(
            deletedRecordIds: Set<String>,
            type: RecordType<out T>,
        ) = Unit

        override suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) = Unit
    }

    private class InsertDuringResyncHealthConstraint(
        private val client: FakeHealthConnectClient,
    ) : HealthConstraint {
        var resyncCalls = 0
        var deliveredRecords = 0

        override suspend fun <T : Record> handleNewRecords(addedRecords: Set<T>, type: RecordType<out T>) {
            deliveredRecords += addedRecords.size
        }

        override suspend fun <T : Record> handleDeletedRecords(
            deletedRecordIds: Set<String>,
            type: RecordType<out T>,
        ) = Unit

        override suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) {
            resyncCalls += 1
            client.insertRecords(
                listOf(
                    StepsRecord(
                        startTime = Instant.parse("2026-08-19T18:00:00Z"),
                        startZoneOffset = null,
                        endTime = Instant.parse("2026-08-19T18:01:00Z"),
                        endZoneOffset = null,
                        count = 101,
                        metadata = Metadata.autoRecorded(
                            Device(
                                type = Device.TYPE_PHONE,
                                manufacturer = "Example Device Company",
                                model = "Test Phone",
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    private class OrderedHealthConstraint : HealthConstraint {
        val events = mutableListOf<String>()
        var resyncCalls = 0

        override suspend fun <T : Record> handleNewRecords(addedRecords: Set<T>, type: RecordType<out T>) {
            events += "upsert"
        }

        override suspend fun <T : Record> handleDeletedRecords(
            deletedRecordIds: Set<String>,
            type: RecordType<out T>,
        ) {
            events += "delete"
        }

        override suspend fun <T : Record> handleExcludedRecords(
            excludedRecordIds: Set<String>,
            type: RecordType<out T>,
        ) {
            events += "excluded"
        }

        override suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) {
            resyncCalls += 1
        }
    }

    private class FailFirstBaselineHealthConstraint : HealthConstraint {
        var resyncCalls = 0

        override suspend fun <T : Record> handleNewRecords(addedRecords: Set<T>, type: RecordType<out T>) = Unit

        override suspend fun <T : Record> handleDeletedRecords(
            deletedRecordIds: Set<String>,
            type: RecordType<out T>,
        ) = Unit

        override suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) {
            resyncCalls += 1
            if (resyncCalls == 1) error("Baseline did not complete.")
        }
    }

    private class ScopedHealthConstraint(
        private val projection: String,
        private val repository: String = HealthConstraint.DEFAULT_REPOSITORY_SCOPE_ID,
    ) : HealthConstraint {
        var resyncCalls = 0

        override fun <T : Record> collectionScopeId(type: RecordType<out T>) = projection

        override fun <T : Record> collectionRepositoryId(type: RecordType<out T>) = repository

        override suspend fun <T : Record> handleNewRecords(addedRecords: Set<T>, type: RecordType<out T>) = Unit

        override suspend fun <T : Record> handleDeletedRecords(
            deletedRecordIds: Set<String>,
            type: RecordType<out T>,
        ) = Unit

        override suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) {
            resyncCalls += 1
        }
    }

    private class BlockingBaselineHealthConstraint : HealthConstraint {
        val entered = CompletableDeferred<Unit>()

        override suspend fun <T : Record> handleNewRecords(addedRecords: Set<T>, type: RecordType<out T>) = Unit

        override suspend fun <T : Record> handleDeletedRecords(
            deletedRecordIds: Set<String>,
            type: RecordType<out T>,
        ) = Unit

        override suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) {
            entered.complete(Unit)
            awaitCancellation()
        }
    }

    private class InMemoryLocalStorage : LocalStorage {
        private val values = mutableMapOf<String, ByteArray>()

        override suspend fun <T : Any> store(
            key: String,
            value: T,
            settings: LocalStorageSetting,
            serializer: SerializationStrategy<T>,
        ) = store(key, value, settings) { kotlinx.serialization.json.Json.encodeToString(serializer, it).encodeToByteArray() }

        override suspend fun <T : Any> store(
            key: String,
            value: T,
            settings: LocalStorageSetting,
            encoding: (T) -> ByteArray,
        ) {
            values[key] = encoding(value)
        }

        override suspend fun <T : Any> read(
            key: String,
            settings: LocalStorageSetting,
            serializer: DeserializationStrategy<T>,
        ): T? = read(key, settings) { kotlinx.serialization.json.Json.decodeFromString(serializer, it.decodeToString()) }

        override suspend fun <T : Any> read(
            key: String,
            settings: LocalStorageSetting,
            decoding: (ByteArray) -> T,
        ): T? = values[key]?.let(decoding)

        override suspend fun delete(key: String) {
            values.remove(key)
        }
    }

    private fun testDevice() = Device(
        type = Device.TYPE_PHONE,
        manufacturer = "Example Device Company",
        model = "Test Phone",
    )
}
