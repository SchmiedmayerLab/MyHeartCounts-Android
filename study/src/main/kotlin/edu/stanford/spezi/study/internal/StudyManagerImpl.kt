//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:TooManyFunctions")

package edu.stanford.spezi.study.internal

import edu.stanford.spezi.core.logging.speziLogger
import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.scheduler.InstantRange
import edu.stanford.spezi.scheduler.NotificationThread
import edu.stanford.spezi.scheduler.NotificationTime
import edu.stanford.spezi.scheduler.Schedule
import edu.stanford.spezi.scheduler.Scheduler
import edu.stanford.spezi.scheduler.ShadowedOutcomesHandling
import edu.stanford.spezi.scheduler.Task
import edu.stanford.spezi.scheduler.TaskCategory
import edu.stanford.spezi.scheduler.TaskContext
import edu.stanford.spezi.scheduler.TaskDraft
import edu.stanford.spezi.study.ScheduledTaskAction
import edu.stanford.spezi.study.ScheduledTaskActionKey
import edu.stanford.spezi.study.StudyContext
import edu.stanford.spezi.study.StudyContextKey
import edu.stanford.spezi.study.StudyEnrollment
import edu.stanford.spezi.study.StudyEnrollmentException
import edu.stanford.spezi.study.StudyManager
import edu.stanford.spezi.study.StudyTaskCategories
import edu.stanford.spezi.study.StudyTaskId
import edu.stanford.spezi.studydefinition.Component
import edu.stanford.spezi.studydefinition.ComponentSchedule
import edu.stanford.spezi.studydefinition.NotificationsConfig
import edu.stanford.spezi.studydefinition.OneTimeSchedule
import edu.stanford.spezi.studydefinition.ScheduleDefinition
import edu.stanford.spezi.studydefinition.StudyBundle
import edu.stanford.spezi.studydefinition.StudyLifecycleEvent
import edu.stanford.spezi.studydefinition.TimedWalkingTestConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/**
 * Default [StudyManager] implementation.
 *
 * Enrollments are persisted through [enrollmentDao]; each study's component schedules are compiled
 * into [scheduler] tasks tagged with a [StudyContext]. Tasks belonging to a study share a task-id
 * prefix, which drives cleanup on unenrollment and revision changes.
 */
internal class StudyManagerImpl @Suppress("detekt:LongParameterList") constructor(
    private val enrollmentDao: StudyEnrollmentDao,
    private val scheduler: Scheduler,
    private val scheduleCompiler: ScheduleCompiler,
    private val timeProvider: TimeProvider,
    private val bundleStore: BundleStore,
    private val mapper: StudyEnrollmentMapper,
    private val scope: CoroutineScope,
    environment: StudyEnvironment = StudyEnvironment(),
) : StudyManager {

    private val logger by speziLogger()
    private val preferredLocale: Locale = environment.preferredLocale
    private val zone: ZoneId = timeProvider.currentZone()

    override val enrollments: Flow<List<StudyEnrollment>> =
        enrollmentDao.observeAll().map { entities -> entities.map { mapper.mapModel(it) } }

    override fun configure() {
        scope.launch {
            reconcileStoredEnrollments()
            removeOrphanedTasks()
            removeOrphanedStudyBundles()
            observeCompletedTaskEvents()
        }
    }

    override suspend fun studyEnrollments(): List<StudyEnrollment> =
        enrollmentDao.all().map { mapper.mapModel(it) }

    override suspend fun enrollment(id: UUID): StudyEnrollment? =
        enrollmentDao.byId(id.toString())?.let { mapper.mapModel(it) }

    override suspend fun enroll(studyBundle: StudyBundle, enrollmentDate: Instant?) {
        val date = enrollmentDate ?: timeProvider.nowInstant()
        val definition = studyBundle.studyDefinition

        val existing = enrollmentDao.byStudyId(definition.id.toString())
        if (existing.isNotEmpty()) {
            resolveReenrollment(existing, definition.studyRevision, studyBundle)
            return
        }

        definition.metadata.studyDependency?.let { dependency ->
            val enrolledInDependency = enrollmentDao.all().any { UUID.fromString(it.studyId) == dependency }
            if (!enrolledInDependency) throw StudyEnrollmentException.MissingStudyDependency()
        }

        val enrollmentId = UUID.randomUUID()
        val storedBundle = bundleStore.store(enrollmentId, studyBundle)
        val enrollment = StudyEnrollment(
            id = enrollmentId,
            enrollmentDate = date,
            studyId = definition.id,
            studyRevision = definition.studyRevision,
        )
        enrollmentDao.upsert(mapper.mapEntity(enrollment))

        registerStudyTasks(enrollment, storedBundle)
        if (isToday(date)) {
            handleLifecycleEvent(StudyLifecycleEvent.Enrollment, enrollment, storedBundle, date)
        }
        // Anchored to the enrollment date rather than the current moment, so that a participant
        // enrolling with an earlier date resumes their existing timeline instead of restarting it.
        handleLifecycleEvent(StudyLifecycleEvent.Activation, enrollment, storedBundle, date)
        setupBackgroundHealthDataCollection(storedBundle)
    }

    override suspend fun unenroll(enrollment: StudyEnrollment) {
        deleteTasksWithPrefix(StudyTaskId.prefix(enrollment.studyId))
        enrollmentDao.delete(enrollment.id.toString())
        bundleStore.delete(enrollment.id)
    }

    override suspend fun informAboutStudies(studyBundles: Collection<StudyBundle>) {
        for (bundle in studyBundles) {
            val definition = bundle.studyDefinition
            val outdated = enrollmentDao.byStudyId(definition.id.toString())
                .filter { it.studyRevision.toUInt() < definition.studyRevision }
            for (entity in outdated) {
                val enrollmentId = UUID.fromString(entity.id)
                val storedBundle = bundleStore.store(enrollmentId, bundle)
                enrollmentDao.updateRevision(entity.id, definition.studyRevision.toLong())
                val updated = mapper.mapModel(entity).copy(studyRevision = definition.studyRevision)
                registerStudyTasks(updated, storedBundle)
                setupBackgroundHealthDataCollection(storedBundle)
            }
        }
    }

    override suspend fun removeOrphanedTasks() {
        val activeStudyIds = enrollmentDao.all().map { UUID.fromString(it.studyId) }.toSet()
        val orphanIds = scheduler.queryTasks(
            range = ALL_TIME,
            predicate = { task ->
                task.id.startsWith(StudyTaskId.PREFIX) &&
                    activeStudyIds.none { task.id.startsWith(StudyTaskId.prefix(it)) }
            },
        ).map { it.id }.toSet()
        orphanIds.forEach { deleteAllVersions(it) }
    }

    override suspend fun removeOrphanedStudyBundles() {
        val activeIds = enrollmentDao.all().map { UUID.fromString(it.id) }.toSet()
        bundleStore.deleteOrphans(activeIds)
    }

    private suspend fun resolveReenrollment(
        existing: List<StudyEnrollmentEntity>,
        incomingRevision: UInt,
        studyBundle: StudyBundle,
    ) {
        val single = existing.singleOrNull()
            ?: throw StudyEnrollmentException.AlreadyEnrolledInNewerRevision()
        when {
            single.studyRevision.toUInt() == incomingRevision ->
                logger.i { "Ignoring enrollment: already enrolled at this revision." }
            single.studyRevision.toUInt() < incomingRevision ->
                informAboutStudies(listOf(studyBundle))
            else -> throw StudyEnrollmentException.AlreadyEnrolledInNewerRevision()
        }
    }

    private suspend fun reconcileStoredEnrollments() {
        for (enrollment in studyEnrollments()) {
            val bundle = bundleStore.open(enrollment.id) ?: continue
            registerStudyTasks(enrollment, bundle)
            setupBackgroundHealthDataCollection(bundle)
        }
    }

    private suspend fun registerStudyTasks(enrollment: StudyEnrollment, bundle: StudyBundle) {
        val study = bundle.studyDefinition
        val activeTaskIds = mutableSetOf<String>()
        for (schedule in study.componentSchedules) {
            registerScheduleTask(schedule, enrollment, bundle)?.let { activeTaskIds.add(it) }
        }
        cleanupRemovedTasks(study.id, activeTaskIds)
    }

    /**
     * Registers (or refreshes) the task for a single component schedule, returning the id of the task
     * to keep active, or `null` when the schedule produces no task.
     */
    private suspend fun registerScheduleTask(
        schedule: ComponentSchedule,
        enrollment: StudyEnrollment,
        bundle: StudyBundle,
    ): String? {
        val component = bundle.studyDefinition.component(schedule.componentId)
            ?.takeIf { it.kind != Component.Kind.INTERNAL }
            ?: return null

        val eventSchedule = (schedule.scheduleDefinition as? ScheduleDefinition.Once)
            ?.schedule as? OneTimeSchedule.Event
        return if (eventSchedule != null) {
            registerEventPlaceholder(schedule, component, enrollment, bundle)
        } else {
            scheduleCompiler.schedule(schedule.scheduleDefinition, enrollment.enrollmentDate)?.let { taskSchedule ->
                createStudyTask(
                    scheduleId = schedule.id,
                    component = component,
                    enrollment = enrollment,
                    bundle = bundle,
                    taskSchedule = taskSchedule,
                    completionPolicy = schedule.completionPolicy,
                    notifications = schedule.notifications,
                ).id
            }
        }
    }

    /**
     * Marks an event-based schedule's task id as active, refreshing an already-materialized task so
     * its title and locale stay current. The task itself is created when the event fires.
     */
    private suspend fun registerEventPlaceholder(
        schedule: ComponentSchedule,
        component: Component,
        enrollment: StudyEnrollment,
        bundle: StudyBundle,
    ): String {
        val taskId = StudyTaskId.of(bundle.id, schedule.componentId, schedule.id)
        scheduler.latestVersion(taskId)?.let { existing ->
            createStudyTask(
                scheduleId = schedule.id,
                component = component,
                enrollment = enrollment,
                bundle = bundle,
                taskSchedule = existing.schedule,
                completionPolicy = schedule.completionPolicy,
                notifications = schedule.notifications,
            )
        }
        return taskId
    }

    private suspend fun cleanupRemovedTasks(studyId: UUID, activeTaskIds: Set<String>) {
        val prefix = StudyTaskId.prefix(studyId)
        val orphanIds = scheduler
            .queryTasks(ALL_TIME, predicate = { it.id.startsWith(prefix) && it.id !in activeTaskIds })
            .map { it.id }.toSet()
        orphanIds.forEach { deleteAllVersions(it) }
    }

    private suspend fun handleLifecycleEvent(
        event: StudyLifecycleEvent,
        enrollment: StudyEnrollment,
        bundle: StudyBundle,
        date: Instant,
    ) {
        for (schedule in bundle.studyDefinition.componentSchedules) {
            materializeEventTask(schedule, event, enrollment, bundle, date)
        }
    }

    /**
     * Creates the task for [schedule] if it is anchored to [event]; a no-op otherwise.
     */
    private suspend fun materializeEventTask(
        schedule: ComponentSchedule,
        event: StudyLifecycleEvent,
        enrollment: StudyEnrollment,
        bundle: StudyBundle,
        date: Instant,
    ) {
        val eventSchedule = (schedule.scheduleDefinition as? ScheduleDefinition.Once)
            ?.schedule as? OneTimeSchedule.Event ?: return
        val component = bundle.studyDefinition.component(schedule.componentId)
            ?.takeIf { it.kind != Component.Kind.INTERNAL }
        if (eventSchedule.event == event && component != null) {
            createStudyTask(
                scheduleId = schedule.id,
                component = component,
                enrollment = enrollment,
                bundle = bundle,
                taskSchedule = scheduleCompiler.eventOccurrenceSchedule(eventSchedule, date),
                completionPolicy = schedule.completionPolicy,
                notifications = schedule.notifications,
            )
        }
    }

    @Suppress("detekt:LongParameterList")
    private suspend fun createStudyTask(
        scheduleId: UUID,
        component: Component,
        enrollment: StudyEnrollment,
        bundle: StudyBundle,
        taskSchedule: Schedule,
        completionPolicy: AllowedCompletionPolicy,
        notifications: NotificationsConfig,
    ): Task {
        val (category, action) = categoryAndAction(component)
        val enabledNotifications = notifications as? NotificationsConfig.Enabled
        val draft = TaskDraft(
            id = StudyTaskId.of(bundle.id, component.id, scheduleId),
            title = bundle.displayTitle(component, preferredLocale) ?: "",
            instructions = bundle.displaySubtitle(component, preferredLocale) ?: "",
            category = category,
            schedule = taskSchedule,
            completionPolicy = completionPolicy,
            context = TaskContext().apply {
                this[StudyContextKey] = StudyContext(
                    studyId = bundle.id,
                    componentId = component.id,
                    scheduleId = scheduleId,
                    enrollmentId = enrollment.id,
                )
                this[ScheduledTaskActionKey] = action
            },
            scheduleNotifications = enabledNotifications != null,
            notificationThread = enabledNotifications?.thread ?: NotificationThread.None,
            notificationTime = enabledNotifications?.time?.let {
                NotificationTime(hour = it.hour, minute = it.minute, second = it.second)
            },
        )
        return scheduler.createOrUpdateTask(
            draft = draft,
            effectiveFrom = timeProvider.nowInstant(),
            shadowedOutcomesHandling = ShadowedOutcomesHandling.DELETE,
        ).getOrThrow().task
    }

    private fun categoryAndAction(component: Component): Pair<TaskCategory, ScheduledTaskAction> = when (component) {
        is Component.Informational ->
            StudyTaskCategories.informational to ScheduledTaskAction.PresentInformational(component)
        is Component.Questionnaire ->
            StudyTaskCategories.questionnaire to ScheduledTaskAction.AnswerQuestionnaire(component)
        is Component.TimedWalkingTest -> {
            val category = when (component.test.kind) {
                TimedWalkingTestConfiguration.Kind.WALKING -> StudyTaskCategories.timedWalkingTest
                TimedWalkingTestConfiguration.Kind.RUNNING -> StudyTaskCategories.timedRunningTest
            }
            category to ScheduledTaskAction.PromptTimedWalkingTest(component)
        }
        is Component.CustomActiveTask ->
            StudyTaskCategories.customActiveTask(component.activeTask.identifier) to
                ScheduledTaskAction.PerformCustomActiveTask(component)
        is Component.HealthDataCollection ->
            error("Health data collection components are not eligible for task creation.")
    }

    private fun observeCompletedTaskEvents() {
        scope.launch {
            scheduler.newOutcomes.collect { outcome ->
                val task = scheduler.latestVersion(outcome.taskId) ?: return@collect
                val studyContext = task.context[StudyContextKey] ?: return@collect
                val enrollment = studyEnrollments().firstOrNull { it.id == studyContext.enrollmentId }
                    ?: return@collect
                val bundle = bundleStore.open(enrollment.id) ?: return@collect
                handleLifecycleEvent(
                    StudyLifecycleEvent.CompletedTask(studyContext.componentId),
                    enrollment,
                    bundle,
                    timeProvider.nowInstant(),
                )
            }
        }
    }

    private suspend fun deleteTasksWithPrefix(prefix: String) {
        val ids = scheduler.queryTasks(ALL_TIME, predicate = { it.id.startsWith(prefix) })
            .map { it.id }.toSet()
        ids.forEach { deleteAllVersions(it) }
    }

    private suspend fun deleteAllVersions(taskId: String) {
        scheduler.deleteAllVersions(taskId)
    }

    /**
     * Records the study's declared health-data sample types. Background collection itself is
     * coordinated outside this module.
     */
    private fun setupBackgroundHealthDataCollection(bundle: StudyBundle) {
        val sampleTypes = bundle.studyDefinition.healthDataCollectionComponents.flatMap { it.sampleTypes }
        if (sampleTypes.isNotEmpty()) {
            logger.i { "Study ${bundle.id} declares health data collection for: $sampleTypes" }
        }
    }

    private fun isToday(instant: Instant): Boolean =
        instant.atZone(zone).toLocalDate() == timeProvider.nowInstant().atZone(zone).toLocalDate()

    private companion object {
        private val ALL_TIME = InstantRange(
            start = Instant.ofEpochMilli(0),
            endExclusive = Instant.ofEpochMilli(Long.MAX_VALUE),
        )
    }
}
