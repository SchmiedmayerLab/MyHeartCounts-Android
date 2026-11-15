//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.choice.MonthYearPicker
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.StringResource
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Presents the date of diagnosis of a comorbidity: `yyyy-MM`, `yyyy`, or empty when unknown.
 */
interface DiagnosisDateMapper {

    /**
     * The wording of [date]: month and year, the year alone, or "Unknown".
     */
    fun mapText(date: String): StringResource

    /**
     * The picker of the date of diagnosis of [condition], opening on [date] and offering the past
     * hundred years. The chosen date, or an empty one for "Don't know", goes to [onChange];
     * [onDismiss] is called when the picker closes unchanged.
     */
    fun mapPicker(
        condition: String,
        date: String,
        onChange: (String) -> Unit,
        onDismiss: () -> Unit,
    ): MonthYearPicker
}

/**
 * Default [DiagnosisDateMapper] implementation, titling each picker with its condition from
 * [conditionsProvider].
 */
class DiagnosisDateMapperImpl(
    private val conditionsProvider: ComorbiditiesConditionsProvider,
    private val timeProvider: TimeProvider,
) : DiagnosisDateMapper {

    override fun mapText(date: String): StringResource = when {
        date.isEmpty() -> StringResource(MHCStrings.comorbidities_since_unknown)
        date.contains('-') -> StringResource(
            YearMonth.parse(date).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())),
        )
        else -> StringResource(date)
    }

    override fun mapPicker(
        condition: String,
        date: String,
        onChange: (String) -> Unit,
        onDismiss: () -> Unit,
    ): MonthYearPicker {
        val currentYear = timeProvider.nowLocalDate().year
        val locale = Locale.getDefault()
        return MonthYearPicker(
            title = conditionsProvider.conditions().firstOrNull { it.id == condition }?.title
                ?: StringResource(MHCStrings.comorbidities_since),
            years = (currentYear downTo currentYear - DIAGNOSIS_YEARS).toList(),
            months = Month.entries.map { StringResource(it.getDisplayName(TextStyle.FULL, locale)) },
            year = date.take(YEAR_LENGTH).toIntOrNull(),
            month = date.drop(YEAR_LENGTH + 1).toIntOrNull(),
            yearLabel = StringResource(MHCStrings.comorbidities_diagnosis_year),
            monthLabel = StringResource(MHCStrings.comorbidities_diagnosis_month),
            noMonth = StringResource(MHCStrings.demographics_value_empty),
            confirmTitle = StringResource(MHCStrings.demographics_date_picker_confirm),
            clearTitle = StringResource(MHCStrings.comorbidities_diagnosis_unknown),
            dismissTitle = StringResource(MHCStrings.demographics_date_picker_dismiss),
            onSelect = { year, month -> onChange(month?.let { "%04d-%02d".format(year, it) } ?: year.toString()) },
            onClear = { onChange("") },
            onDismiss = onDismiss,
        )
    }

    private companion object {
        const val DIAGNOSIS_YEARS = 100
        const val YEAR_LENGTH = 4
    }
}
