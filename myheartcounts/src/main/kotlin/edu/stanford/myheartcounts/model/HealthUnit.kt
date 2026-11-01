//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.model

/**
 * A unit a health measurement is recorded in.
 *
 * Units of the same [Dimension] convert into one another; units of different dimensions are
 * incompatible.
 *
 * @property symbols the spellings that denote the unit in stored data, the first being the
 * canonical one
 */
enum class HealthUnit(
    val symbols: List<String>,
    private val dimension: Dimension,
    private val factor: Double,
) {
    Count(symbols = listOf("count"), dimension = Dimension.Count, factor = 1.0),
    CountPerSecond(symbols = listOf("count/s"), dimension = Dimension.Frequency, factor = 60.0),
    CountPerMinute(symbols = listOf("count/min"), dimension = Dimension.Frequency, factor = 1.0),
    Millisecond(symbols = listOf("ms"), dimension = Dimension.Time, factor = 0.001),
    Second(symbols = listOf("s"), dimension = Dimension.Time, factor = 1.0),
    Minute(symbols = listOf("min"), dimension = Dimension.Time, factor = 60.0),
    Hour(symbols = listOf("hr", "h"), dimension = Dimension.Time, factor = 3_600.0),
    Day(symbols = listOf("d"), dimension = Dimension.Time, factor = 86_400.0),
    Millimeter(symbols = listOf("mm"), dimension = Dimension.Length, factor = 0.001),
    Centimeter(symbols = listOf("cm"), dimension = Dimension.Length, factor = 0.01),
    Meter(symbols = listOf("m"), dimension = Dimension.Length, factor = 1.0),
    Kilometer(symbols = listOf("km"), dimension = Dimension.Length, factor = 1_000.0),
    Inch(symbols = listOf("in"), dimension = Dimension.Length, factor = 0.0254),
    Foot(symbols = listOf("ft"), dimension = Dimension.Length, factor = 0.3048),
    Yard(symbols = listOf("yd"), dimension = Dimension.Length, factor = 0.9144),
    Mile(symbols = listOf("mi"), dimension = Dimension.Length, factor = 1_609.344),
    Gram(symbols = listOf("g"), dimension = Dimension.Mass, factor = 0.001),
    Kilogram(symbols = listOf("kg"), dimension = Dimension.Mass, factor = 1.0),
    Pound(symbols = listOf("lb"), dimension = Dimension.Mass, factor = 0.45359237),
    Stone(symbols = listOf("st"), dimension = Dimension.Mass, factor = 6.35029318),
    Kilocalorie(symbols = listOf("kcal", "Cal"), dimension = Dimension.Energy, factor = 1.0),
    Kilojoule(symbols = listOf("kJ"), dimension = Dimension.Energy, factor = 1.0 / 4.184),
    MillimeterOfMercury(symbols = listOf("mmHg"), dimension = Dimension.Pressure, factor = 1.0),
    MilligramPerDeciliter(symbols = listOf("mg/dL"), dimension = Dimension.MassConcentration, factor = 1.0),
    MillimolePerLiter(symbols = listOf("mmol/L"), dimension = Dimension.MolarConcentration, factor = 1.0),
    ;

    /**
     * Whether a value in this unit can be expressed in [other].
     */
    fun isCompatibleWith(other: HealthUnit): Boolean = dimension == other.dimension

    /**
     * Expresses [value], given in this unit, in [target]; `null` when the units are incompatible.
     */
    fun convert(value: Double, target: HealthUnit): Double? = when {
        this == target -> value
        isCompatibleWith(target) -> value * factor / target.factor
        else -> null
    }

    private enum class Dimension {
        Count,
        Frequency,
        Time,
        Length,
        Mass,
        Energy,
        Pressure,
        MassConcentration,
        MolarConcentration,
    }

    companion object {
        /**
         * The unit denoted by [symbol], or `null` when the symbol names no known unit.
         */
        fun fromSymbol(symbol: String): HealthUnit? = entries.firstOrNull { symbol in it.symbols }
    }
}
