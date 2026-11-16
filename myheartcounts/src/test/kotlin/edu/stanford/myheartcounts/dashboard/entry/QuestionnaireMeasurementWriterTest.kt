//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.entry

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Test
import java.time.Instant

class QuestionnaireMeasurementWriterTest {
    private val mapper = FakeQuestionnaireMeasurementMapper()
    private val entryWriter = FakeHeartScoreEntryWriter()
    private val writer = QuestionnaireMeasurementWriterImpl(
        mapper = mapper,
        entryWriter = entryWriter,
    )

    @Test
    fun `saves every measurement a response reports`() = runTest {
        // given
        mapper.setEntries(entries = Result.success(listOf(PRESSURE, GLUCOSE)))

        // when
        val result = writer.save(
            response = QuestionnaireResponse(),
            questionnaire = Questionnaire(),
        )

        // then
        assertThat(result.isSuccess).isTrue()
        assertThat(entryWriter.savedEntries).containsExactly(PRESSURE, GLUCOSE).inOrder()
    }

    @Test
    fun `saves nothing from a refused response`() = runTest {
        // given
        mapper.setEntries(entries = Result.failure(IllegalArgumentException("answered in part")))

        // when
        val result = writer.save(
            response = QuestionnaireResponse(),
            questionnaire = Questionnaire(),
        )

        // then
        assertThat(result.isFailure).isTrue()
        assertThat(entryWriter.savedEntries).isEmpty()
    }

    @Test
    fun `a failed measurement does not stop the others and fails the result`() = runTest {
        // given
        mapper.setEntries(entries = Result.success(listOf(PRESSURE, GLUCOSE)))
        entryWriter.setFailure(entry = PRESSURE)

        // when
        val result = writer.save(
            response = QuestionnaireResponse(),
            questionnaire = Questionnaire(),
        )

        // then
        assertThat(result.isFailure).isTrue()
        assertThat(entryWriter.savedEntries).containsExactly(PRESSURE, GLUCOSE).inOrder()
    }

    private class FakeQuestionnaireMeasurementMapper : QuestionnaireMeasurementMapper {
        private var entries: Result<List<HeartScoreEntry>> = Result.success(emptyList())

        fun setEntries(entries: Result<List<HeartScoreEntry>>) {
            this.entries = entries
        }

        override fun mapEntries(
            response: QuestionnaireResponse,
            questionnaire: Questionnaire,
        ): Result<List<HeartScoreEntry>> = entries
    }

    private class FakeHeartScoreEntryWriter : HeartScoreEntryWriter {
        private val saved = mutableListOf<HeartScoreEntry>()
        private var failing: HeartScoreEntry? = null

        val savedEntries: List<HeartScoreEntry> get() = saved

        fun setFailure(entry: HeartScoreEntry) {
            failing = entry
        }

        override suspend fun save(entry: HeartScoreEntry): Result<Unit> {
            saved += entry
            return if (entry == failing) Result.failure(SecurityException("Write permission was not granted")) else Result.success(Unit)
        }
    }

    private companion object {
        val DATE: Instant = Instant.parse("2026-01-01T09:30:00Z")
        val PRESSURE = HeartScoreEntry.Pressure(
            id = "pressure",
            date = DATE,
            systolic = 120.0,
            diastolic = 80.0,
        )
        val GLUCOSE = HeartScoreEntry.Quantity(
            id = "glucose",
            date = DATE,
            component = HeartScoreComponent.BloodGlucose,
            value = 95.0,
        )
    }
}
