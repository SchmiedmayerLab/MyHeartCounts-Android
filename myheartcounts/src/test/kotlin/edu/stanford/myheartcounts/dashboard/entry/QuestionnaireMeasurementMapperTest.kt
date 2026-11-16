//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.entry

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import org.grovealliance.core.time.FakeTimeProvider
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Type
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Date

class QuestionnaireMeasurementMapperTest {
    private val timeProvider = FakeTimeProvider()
    private val mapper = QuestionnaireMeasurementMapperImpl(timeProvider = timeProvider)

    @Before
    fun setup() {
        timeProvider.setNow(instant = NOW)
    }

    @Test
    fun `a complete response reports a blood pressure and a glucose reading at its authored time`() {
        // given
        val response = response(
            systolic = millimetresOfMercury(value = 120.0),
            diastolic = millimetresOfMercury(value = 80.0),
            glucose = Quantity().setValue(95.0).setUnit("mg/dL"),
        )

        // when
        val entries = mapper.mapEntries(
            response = response,
            questionnaire = questionnaire(),
        ).getOrThrow()

        // then
        val pressure = entries.filterIsInstance<HeartScoreEntry.Pressure>().single()
        val glucose = entries.filterIsInstance<HeartScoreEntry.Quantity>().single()
        assertThat(pressure.systolic).isEqualTo(120.0)
        assertThat(pressure.diastolic).isEqualTo(80.0)
        assertThat(pressure.date).isEqualTo(AUTHORED)
        assertThat(glucose.component).isEqualTo(HeartScoreComponent.BloodGlucose)
        assertThat(glucose.value).isEqualTo(95.0)
        assertThat(glucose.date).isEqualTo(AUTHORED)
    }

    @Test
    fun `unanswered measurements report nothing`() {
        // when
        val entries = mapper.mapEntries(
            response = response(),
            questionnaire = questionnaire(),
        ).getOrThrow()

        // then
        assertThat(entries).isEmpty()
    }

    @Test
    fun `a blood pressure answered in part refuses the whole response`() {
        // given
        val response = response(
            systolic = millimetresOfMercury(value = 120.0),
            glucose = Quantity().setValue(95.0).setUnit("mg/dL"),
        )

        // when
        val result = mapper.mapEntries(
            response = response,
            questionnaire = questionnaire(),
        )

        // then
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `an answer in another unit refuses the whole response`() {
        // given
        val response = response(
            systolic = millimetresOfMercury(value = 120.0),
            diastolic = millimetresOfMercury(value = 80.0),
            glucose = Quantity().setValue(5.3).setUnit("mmol/L"),
        )

        // when
        val result = mapper.mapEntries(
            response = response,
            questionnaire = questionnaire(),
        )

        // then
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `a measurement answered more than once refuses the whole response`() {
        // given
        val response = response(glucose = Quantity().setValue(95.0).setUnit("mg/dL"))
        response.item.single { it.linkId == GLUCOSE_LINK_ID }.addAnswer().setValue(Quantity().setValue(99.0).setUnit("mg/dL"))

        // when
        val result = mapper.mapEntries(
            response = response,
            questionnaire = questionnaire(),
        )

        // then
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `only questions marked for extraction report measurements`() {
        // given
        val questionnaire = questionnaire().apply {
            item.single { it.linkId == GLUCOSE_LINK_ID }.removeExtension(OBSERVATION_EXTRACT_URL)
        }

        // when
        val entries = mapper.mapEntries(
            response = response(glucose = Quantity().setValue(95.0).setUnit("mg/dL")),
            questionnaire = questionnaire,
        ).getOrThrow()

        // then
        assertThat(entries).isEmpty()
    }

    @Test
    fun `a marked measurement that cannot be recorded is left out`() {
        // given
        val questionnaire = questionnaire().apply {
            addItem(measurement(linkId = "hdl", code = "2085-9", marking = BooleanType(true)))
        }
        val response = response(glucose = Quantity().setValue(95.0).setUnit("mg/dL")).apply {
            addItem().setLinkId("hdl").addAnswer().setValue(Quantity().setValue(50.0).setUnit("mg/dL"))
        }

        // when
        val entries = mapper.mapEntries(
            response = response,
            questionnaire = questionnaire,
        ).getOrThrow()

        // then
        assertThat(entries.map { (it as HeartScoreEntry.Quantity).component }).containsExactly(HeartScoreComponent.BloodGlucose)
    }

    @Test
    fun `a response without an authored time is dated now`() {
        // given
        val response = response(glucose = Quantity().setValue(95.0).setUnit("mg/dL")).setAuthored(null)

        // when
        val entries = mapper.mapEntries(
            response = response,
            questionnaire = questionnaire(),
        ).getOrThrow()

        // then
        assertThat(entries.single().date).isEqualTo(NOW)
    }

    private fun questionnaire() = Questionnaire().apply {
        addItem(
            measurement(linkId = PANEL_LINK_ID, code = "85354-9", marking = BooleanType(true))
                .setType(Questionnaire.QuestionnaireItemType.GROUP)
                .apply {
                    addItem(measurement(linkId = SYSTOLIC_LINK_ID, code = "8480-6", marking = CodeType("component")))
                    addItem(measurement(linkId = DIASTOLIC_LINK_ID, code = "8462-4", marking = CodeType("component")))
                },
        )
        addItem(measurement(linkId = GLUCOSE_LINK_ID, code = "2339-0", marking = BooleanType(true)))
    }

    private fun measurement(linkId: String, code: String, marking: Type) = Questionnaire.QuestionnaireItemComponent()
        .setLinkId(linkId)
        .setType(Questionnaire.QuestionnaireItemType.DECIMAL)
        .addCode(Coding().setSystem("http://loinc.org").setCode(code))
        .apply { addExtension(OBSERVATION_EXTRACT_URL, marking) }

    private fun response(
        systolic: Quantity? = null,
        diastolic: Quantity? = null,
        glucose: Quantity? = null,
    ) = QuestionnaireResponse().apply {
        setAuthored(Date.from(AUTHORED))
        addItem().setLinkId(PANEL_LINK_ID).apply {
            addItem().setLinkId(SYSTOLIC_LINK_ID).apply { systolic?.let { addAnswer().setValue(it) } }
            addItem().setLinkId(DIASTOLIC_LINK_ID).apply { diastolic?.let { addAnswer().setValue(it) } }
        }
        addItem().setLinkId(GLUCOSE_LINK_ID).apply { glucose?.let { addAnswer().setValue(it) } }
    }

    private fun millimetresOfMercury(value: Double): Quantity = Quantity().setValue(value).setUnit("mm[Hg]")

    private companion object {
        const val OBSERVATION_EXTRACT_URL = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-observationExtract"
        const val PANEL_LINK_ID = "blood-pressure-panel"
        const val SYSTOLIC_LINK_ID = "systolic"
        const val DIASTOLIC_LINK_ID = "diastolic"
        const val GLUCOSE_LINK_ID = "glucose"
        val AUTHORED: Instant = Instant.parse("2026-01-01T09:30:00Z")
        val NOW: Instant = Instant.parse("2026-01-01T12:00:00Z")
    }
}
