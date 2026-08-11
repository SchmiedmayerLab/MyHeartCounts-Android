//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.scheduler.fixtures.TaskDraftFixtures
import edu.stanford.spezi.scheduler.internal.SchedulerDatabase
import edu.stanford.spezi.scheduler.internal.SchedulerImpl
import edu.stanford.spezi.scheduler.internal.SchedulerMapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class SchedulerImplTest {
    private val zone: ZoneId = ZoneId.systemDefault()
    private lateinit var database: SchedulerDatabase
    private lateinit var scheduler: SchedulerImpl
    private val calculator = FakeScheduleCalculator()
    private val dao get() = database.dao()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SchedulerDatabase::class.java,
        ).build()
        scheduler = SchedulerImpl(
            database = database,
            timeProvider = TimeProvider.create(context),
            mapper = SchedulerMapper(),
            scheduleCalculator = calculator,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `creates and reads back a single version`() = runTest {
        // when
        val result = addDailyTask()

        // then
        assertThat(result.didChange).isTrue()
        assertThat(dao.allTaskVersions()).hasSize(1)
        assertThat(scheduler.latestVersion("t")?.title).isEqualTo("Title")
    }

    @Test
    fun `unchanged update is a no-op`() = runTest {
        // given
        addDailyTask()

        // when
        val again = addDailyTask()

        // then
        assertThat(again.didChange).isFalse()
        assertThat(dao.allTaskVersions()).hasSize(1)
    }

    @Test
    fun `changed update creates a new version`() = runTest {
        // given
        addDailyTask(effectiveFrom = at(2026, 6, 14))

        // when
        val updated = scheduler.createOrUpdateTask(
            draft = TaskDraftFixtures.create(
                id = "t",
                title = "New Title",
                instructions = "Do it",
                schedule = calculator.daily(hour = 9, minute = 0, startingAt = at(2026, 6, 14)),
            ),
            effectiveFrom = at(2026, 6, 18),
        ).getOrThrow()

        // then
        assertThat(updated.didChange).isTrue()
        val versions = scheduler.allVersions("t")
        assertThat(versions).hasSize(2)
        assertThat(versions.first().nextVersionEffectiveFrom).isEqualTo(at(2026, 6, 18))
        assertThat(versions.last().isLatestVersion).isTrue()
    }

    @Test
    fun `query events expands the schedule and joins outcomes`() = runTest {
        // given
        addDailyTask()

        // when
        val events = scheduler.queryEvents(days(2026, 6, 14, 3)).first()

        // then
        assertThat(events).hasSize(3)
        assertThat(events.all { !it.isCompleted }).isTrue()

        // when
        scheduler.complete(events.first(), ignoreCompletionPolicy = true).getOrThrow()

        // then
        val afterCompletion = scheduler.queryEvents(days(2026, 6, 14, 3)).first()
        assertThat(afterCompletion.first().isCompleted).isTrue()
        assertThat(dao.allOutcomes()).hasSize(1)
    }

    @Test
    fun `event assembly respects version boundaries`() = runTest {
        // given
        addDailyTask(title = "V1", effectiveFrom = at(2026, 6, 14))
        scheduler.createOrUpdateTask(
            draft = TaskDraftFixtures.create(
                id = "t",
                title = "V2",
                instructions = "Do it",
                schedule = calculator.daily(hour = 9, minute = 0, startingAt = at(2026, 6, 14)),
            ),
            effectiveFrom = at(2026, 6, 16),
        ).getOrThrow()

        // when
        val titlesByDay = scheduler.queryEvents(days(2026, 6, 14, 4)).first()
            .associate { it.occurrence.start to it.task.title }

        // then
        assertThat(titlesByDay[at(2026, 6, 14, 9)]).isEqualTo("V1")
        assertThat(titlesByDay[at(2026, 6, 15, 9)]).isEqualTo("V1")
        assertThat(titlesByDay[at(2026, 6, 16, 9)]).isEqualTo("V2")
        assertThat(titlesByDay[at(2026, 6, 17, 9)]).isEqualTo("V2")
    }

    @Test
    fun `update fails when it would shadow an outcome`() = runTest {
        // given
        addDailyTask()
        val event = scheduler.queryEvents(days(2026, 6, 14, 1)).first().first()
        scheduler.complete(event, ignoreCompletionPolicy = true).getOrThrow()

        // when
        val result = scheduler.createOrUpdateTask(
            draft = TaskDraftFixtures.create(
                id = "t",
                title = "Shadowing",
                instructions = "Do it",
                schedule = calculator.daily(hour = 9, minute = 0, startingAt = at(2026, 6, 14)),
            ),
            effectiveFrom = at(2026, 6, 14),
            shadowedOutcomesHandling = ShadowedOutcomesHandling.THROW_ERROR,
        )

        // then
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `delete task removes that version and newer ones but keeps older`() = runTest {
        // given
        addDailyTask(title = "V1", effectiveFrom = at(2026, 6, 14))
        scheduler.createOrUpdateTask(
            draft = TaskDraftFixtures.create(
                id = "t",
                title = "V2",
                instructions = "Do it",
                schedule = calculator.daily(hour = 9, minute = 0, startingAt = at(2026, 6, 14)),
            ),
            effectiveFrom = at(2026, 6, 18),
        ).getOrThrow()
        val v2 = scheduler.allVersions("t").last()

        // when
        scheduler.deleteTask(v2)

        // then
        val remaining = scheduler.allVersions("t")
        assertThat(remaining).hasSize(1)
        assertThat(remaining.single().title).isEqualTo("V1")
        assertThat(remaining.single().isLatestVersion).isTrue()
    }

    @Test
    fun `delete all versions and a full wipe clear the store`() = runTest {
        // given
        addDailyTask()
        scheduler.complete(scheduler.queryEvents(days(2026, 6, 14, 1)).first().first(), ignoreCompletionPolicy = true)
            .getOrThrow()

        // when
        scheduler.deleteAllVersions("t")

        // then
        assertThat(dao.allTaskVersions()).isEmpty()
        assertThat(dao.allOutcomes()).isEmpty()

        // when
        addDailyTask()
        dao.deleteAllOutcomes()
        dao.deleteAllTaskVersions()

        // then
        assertThat(dao.allTaskVersions()).isEmpty()
    }

    @Test
    fun `query tasks applies sort and fetch limit`() = runTest {
        // given
        scheduler.createOrUpdateTask(
            draft = TaskDraftFixtures.create(
                id = "b",
                title = "B",
                schedule = calculator.daily(hour = 9, minute = 0, startingAt = at(2026, 6, 14)),
            ),
            effectiveFrom = at(2026, 6, 14),
        ).getOrThrow()
        scheduler.createOrUpdateTask(
            draft = TaskDraftFixtures.create(
                id = "a",
                title = "A",
                schedule = calculator.daily(hour = 9, minute = 0, startingAt = at(2026, 6, 14)),
            ),
            effectiveFrom = at(2026, 6, 14),
        ).getOrThrow()

        // when
        val byTitle = scheduler.queryTasks(days(2026, 6, 14, 1), sortBy = compareBy { it.title })

        // then
        assertThat(byTitle.map { it.title }).containsExactly("A", "B").inOrder()
        assertThat(scheduler.queryTasks(days(2026, 6, 14, 1), fetchLimit = 1)).hasSize(1)
    }

    private suspend fun addDailyTask(id: String = "t", title: String = "Title", effectiveFrom: Instant = at(2026, 6, 14)) =
        scheduler.createOrUpdateTask(
            draft = TaskDraftFixtures.create(
                id = id,
                title = title,
                instructions = "Do it",
                schedule = calculator.daily(hour = 9, minute = 0, startingAt = effectiveFrom),
            ),
            effectiveFrom = effectiveFrom,
        ).getOrThrow()

    private fun at(year: Int, month: Int, day: Int, hour: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone).toInstant()

    private fun days(year: Int, month: Int, day: Int, count: Long): InstantRange {
        val start = ZonedDateTime.of(year, month, day, 0, 0, 0, 0, zone)
        return InstantRange(
            start = start.toInstant(),
            endExclusive = start.plusDays(count).toInstant(),
        )
    }
}
