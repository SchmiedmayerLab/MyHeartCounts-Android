//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study.internal

import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.core.time.FakeTimeProvider
import edu.stanford.spezi.foundation.fixtures.UUIDFixtures
import edu.stanford.spezi.scheduler.FakeScheduleCalculator
import edu.stanford.spezi.scheduler.FakeScheduler
import edu.stanford.spezi.scheduler.NotificationThread
import edu.stanford.spezi.scheduler.NotificationTime
import edu.stanford.spezi.scheduler.RecurrenceFrequency
import edu.stanford.spezi.scheduler.fixtures.TaskDraftFixtures
import edu.stanford.spezi.study.ScheduledTaskAction
import edu.stanford.spezi.study.ScheduledTaskActionKey
import edu.stanford.spezi.study.StudyContextKey
import edu.stanford.spezi.study.StudyTaskCategories
import edu.stanford.spezi.study.StudyTaskId
import edu.stanford.spezi.studydefinition.StudyBundle
import edu.stanford.spezi.studydefinition.fixtures.StudyBundleFixtures
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

class StudyManagerImplTest {

    private val zone: ZoneId = ZoneId.of("America/Los_Angeles")
    private val now = ZonedDateTime.of(2026, 7, 1, 8, 30, 0, 0, zone).toInstant()
    private val studyId = UUIDFixtures.repeating('1')

    private val scheduler = FakeScheduler()
    private val scheduleCalculator = FakeScheduleCalculator()
    private val dao = FakeStudyEnrollmentDao()
    private val timeProvider = FakeTimeProvider()
    private val compiler = ScheduleCompiler(scheduleCalculator, timeProvider)
    private lateinit var bundleStore: BundleStore

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setup() {
        timeProvider.setNow(instant = now)
        timeProvider.setZone(zone = zone)
        scheduleCalculator.setNow(instant = now)
        scheduleCalculator.setZone(zone = zone)
        bundleStore = BundleStore(tempFolder.newFolder("study-bundles"))
    }

    @Test
    fun `enrolling registers a task per user-interactive schedule`() = runTest {
        // when
        manager().enroll(exampleBundle())

        // then
        assertThat(scheduler.tasks).hasSize(4)
        assertThat(scheduler.tasks.keys).containsExactly(
            StudyTaskId.of(studyId, UUIDFixtures.repeating('3'), UUIDFixtures.repeating('b')),
            StudyTaskId.of(studyId, UUIDFixtures.repeating('4'), UUIDFixtures.repeating('c')),
            StudyTaskId.of(studyId, UUIDFixtures.repeating('2'), UUIDFixtures.repeating('a')),
            StudyTaskId.of(studyId, UUIDFixtures.repeating('5'), UUIDFixtures.repeating('d')),
        )
    }

    @Test
    fun `does not create a task for the health data collection component`() = runTest {
        // when
        manager().enroll(exampleBundle())

        // then
        assertThat(scheduler.tasks.keys.none { it.contains("66666666") }).isTrue()
    }

    @Test
    fun `tasks carry study context and a scheduled action`() = runTest {
        // given
        val mgr = manager()

        // when
        mgr.enroll(exampleBundle())
        val enrollment = mgr.studyEnrollments().single()
        val questionnaireTask = scheduler.tasks.values.first { it.category == StudyTaskCategories.questionnaire }

        // then
        val context = questionnaireTask.context[StudyContextKey]
        assertThat(context).isNotNull()
        assertThat(context!!.studyId).isEqualTo(studyId)
        assertThat(context.enrollmentId).isEqualTo(enrollment.id)
        assertThat(questionnaireTask.context[ScheduledTaskActionKey])
            .isInstanceOf(ScheduledTaskAction.AnswerQuestionnaire::class.java)
        assertThat(questionnaireTask.title).isEqualTo("Daily Check-In")
    }

    @Test
    fun `weekly schedule compiles into a weekly recurrence`() = runTest {
        // when
        manager().enroll(exampleBundle())
        val walkTask = scheduler.tasks.values.first { it.category == StudyTaskCategories.timedWalkingTest }

        // then
        assertThat(walkTask.schedule.recurrence?.frequency).isEqualTo(RecurrenceFrequency.WEEKLY)
    }

    @Test
    fun `enabled notifications compile into the task's notification settings`() = runTest {
        // when
        manager().enroll(exampleBundle())
        val questionnaireTask = scheduler.tasks.values.first { it.category == StudyTaskCategories.questionnaire }
        val walkTask = scheduler.tasks.values.first { it.category == StudyTaskCategories.timedWalkingTest }

        // then
        assertThat(questionnaireTask.scheduleNotifications).isTrue()
        assertThat(questionnaireTask.notificationThread).isEqualTo(NotificationThread.Custom("daily-checkin"))
        assertThat(questionnaireTask.notificationTime).isEqualTo(NotificationTime(hour = 9, minute = 0, second = 0))
        // the walk schedule has notifications disabled
        assertThat(walkTask.scheduleNotifications).isFalse()
    }

    @Test
    fun `activation anchors event tasks to an earlier enrollment date`() = runTest {
        // given
        val enrollmentDate = now.minus(30, ChronoUnit.DAYS)

        // when
        manager().enroll(
            studyBundle = activationBundle(),
            enrollmentDate = enrollmentDate,
        )

        // then
        val task = scheduler.tasks.getValue(
            StudyTaskId.of(studyId, UUIDFixtures.repeating('5'), UUIDFixtures.repeating('d'))
        )
        val expected = enrollmentDate.atZone(zone).plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0)
        assertThat(task.schedule.start).isEqualTo(expected.toInstant())
    }

    @Test
    fun `re-enrolling at the same revision is a no-op`() = runTest {
        // given
        val mgr = manager()
        mgr.enroll(exampleBundle())
        val tasksAfterFirst = scheduler.tasks.keys.toSet()

        // when
        mgr.enroll(exampleBundle())

        // then
        assertThat(mgr.studyEnrollments()).hasSize(1)
        assertThat(scheduler.tasks.keys).isEqualTo(tasksAfterFirst)
    }

    @Test
    fun `enrolling at a newer revision forwards to an update`() = runTest {
        // given
        val mgr = manager()
        mgr.enroll(exampleBundle())

        // when
        mgr.enroll(exampleBundle(revision = 2))

        // then
        assertThat(mgr.studyEnrollments()).hasSize(1)
        assertThat(mgr.studyEnrollments().single().studyRevision).isEqualTo(2u)
    }

    @Test
    fun `unenrolling removes tasks and the enrollment`() = runTest {
        // given
        val mgr = manager()
        mgr.enroll(exampleBundle())
        val enrollment = mgr.studyEnrollments().single()

        // when
        mgr.unenroll(enrollment)

        // then
        assertThat(mgr.studyEnrollments()).isEmpty()
        assertThat(scheduler.tasks).isEmpty()
    }

    @Test
    fun `removeOrphanedTasks deletes study tasks without a matching enrollment`() = runTest {
        // given
        val mgr = manager()
        scheduler.createOrUpdateTask(
            draft = TaskDraftFixtures.create(
                id = StudyTaskId.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                title = "orphan",
                schedule = edu.stanford.spezi.scheduler.Schedule.once(at = now),
            ),
            effectiveFrom = now,
        )

        // when
        mgr.removeOrphanedTasks()

        // then
        assertThat(scheduler.tasks).isEmpty()
    }

    private fun manager() = StudyManagerImpl(
        enrollmentDao = dao,
        scheduler = scheduler,
        scheduleCompiler = compiler,
        timeProvider = timeProvider,
        bundleStore = bundleStore,
        mapper = StudyEnrollmentMapper(),
        scope = TestScope(),
        environment = StudyEnvironment(preferredLocale = Locale.forLanguageTag("en-US")),
    )

    /**
     * The example bundle with its lifecycle events switched to activation, which unlike enrollment
     * is fired even when enrolling with an earlier date.
     */
    private fun activationBundle(): StudyBundle {
        val copy = tempFolder.newFolder()
            .resolve("example.spezistudybundle")
            .also { StudyBundleFixtures.exampleBundleDir().copyRecursively(it, overwrite = true) }
        val definition = File(copy, "definition.json")
        definition.writeText(definition.readText().replace("\"enrollment\": {}", "\"activation\": {}"))
        return StudyBundle.open(copy)
    }

    private fun exampleBundle(revision: Int? = null): StudyBundle {
        val source = StudyBundleFixtures.exampleBundleDir()
        if (revision == null) return StudyBundle.open(source)
        val copy = tempFolder.newFolder()
            .resolve("example.spezistudybundle").also { source.copyRecursively(it, overwrite = true) }
        val definition = File(copy, "definition.json")
        definition.writeText(definition.readText().replace("\"studyRevision\": 1", "\"studyRevision\": $revision"))
        return StudyBundle.open(copy)
    }
}
