//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.study

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.grovealliance.foundation.UUIDSerializer
import org.grovealliance.scheduler.TaskCategory
import org.grovealliance.scheduler.TaskContext
import org.grovealliance.scheduler.TaskStorageKey
import org.grovealliance.studydefinition.Component
import java.util.UUID

/**
 * The study-related information attached to a scheduler task created for a study component.
 *
 * Stored in a task's [TaskContext] under [StudyContextKey], it links an occurrence back to the
 * enrollment, component, and schedule it originated from.
 */
@Serializable
data class StudyContext(
    @Serializable(with = UUIDSerializer::class)
    val studyId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val componentId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val scheduleId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val enrollmentId: UUID,
)

/**
 * The [TaskStorageKey] under which a task's [StudyContext] is stored.
 */
object StudyContextKey : TaskStorageKey<StudyContext> {
    override val identifier: String = "org.grovealliance.study.studyContext"
    override val serializer: KSerializer<StudyContext> = StudyContext.serializer()
}

/**
 * The user-facing action associated with a study task, dispatched on when the task is opened.
 */
@Serializable
sealed interface ScheduledTaskAction {
    /**
     * Present an informational article.
     */
    @Serializable
    @SerialName("presentInformational")
    data class PresentInformational(val component: Component.Informational) : ScheduledTaskAction

    /**
     * Answer a questionnaire.
     */
    @Serializable
    @SerialName("answerQuestionnaire")
    data class AnswerQuestionnaire(val component: Component.Questionnaire) : ScheduledTaskAction

    /**
     * Perform a timed walking or running test.
     */
    @Serializable
    @SerialName("promptTimedWalkingTest")
    data class PromptTimedWalkingTest(val component: Component.TimedWalkingTest) : ScheduledTaskAction

    /**
     * Perform a custom active task.
     */
    @Serializable
    @SerialName("performCustomActiveTask")
    data class PerformCustomActiveTask(val component: Component.CustomActiveTask) : ScheduledTaskAction
}

/**
 * The [TaskStorageKey] under which a task's [ScheduledTaskAction] is stored.
 */
object ScheduledTaskActionKey : TaskStorageKey<ScheduledTaskAction> {
    override val identifier: String = "org.grovealliance.study.scheduledTaskAction"
    override val serializer: KSerializer<ScheduledTaskAction> = ScheduledTaskAction.serializer()
}

/**
 * The scheduler [TaskCategory]s used for study components.
 */
object StudyTaskCategories {
    /**
     * An informational article task.
     */
    val informational = TaskCategory("org.grovealliance.GroveStudy.task.informational")

    /**
     * A timed walking test task.
     */
    val timedWalkingTest = TaskCategory("org.grovealliance.GroveStudy.task.timedWalkingTest")

    /**
     * A timed running test task.
     */
    val timedRunningTest = TaskCategory("org.grovealliance.GroveStudy.task.timedRunningTest")

    /**
     * A questionnaire task.
     */
    val questionnaire = TaskCategory.questionnaire

    /**
     * A custom active task, distinguished by its [identifier].
     */
    fun customActiveTask(identifier: String) =
        TaskCategory("org.grovealliance.GroveStudy.task.customActiveTask:$identifier")
}

/**
 * Identifiers for the scheduler tasks created on behalf of studies.
 *
 * Task ids are prefixed per study so a study's tasks can be found (and cleaned up) by prefix without
 * decoding the study definition.
 */
object StudyTaskId {
    /**
     * The prefix shared by all study-domain task ids.
     */
    const val PREFIX = "org.grovealliance.GroveStudy.studyComponentTask."

    /**
     * The task-id prefix for the study with [studyId].
     */
    fun prefix(studyId: UUID): String = "$PREFIX$studyId"

    /**
     * The task id for a specific component schedule of a study.
     */
    fun of(studyId: UUID, componentId: UUID, scheduleId: UUID): String =
        "${prefix(studyId)}.$componentId.$scheduleId"
}
