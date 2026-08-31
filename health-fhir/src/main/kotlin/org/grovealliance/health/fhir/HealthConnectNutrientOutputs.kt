//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.NutritionRecord

/** Every nutrient the adapter projects out of one Health Connect NutritionRecord. */
internal fun mobileNutrient(
    measurement: String,
    display: String,
    unit: String,
    extract: (NutritionRecord) -> Double?,
): NutrientSpec = NutrientSpec(
    measurement,
    MobileQuantitySpec(
        profile = requireNotNull(HealthConnectContract.mobileDietaryProfiles[measurement]),
        category = null,
        codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
        code = measurement,
        display = display,
        unitCode = unit,
        unitDisplay = unit,
    ),
    extract,
)

internal fun loincNutrient(
    measurement: String,
    code: String,
    display: String,
    unit: String,
    extract: (NutritionRecord) -> Double?,
): NutrientSpec = NutrientSpec(
    measurement,
    MobileQuantitySpec(
        profile = requireNotNull(HealthConnectContract.mobileDietaryProfiles[measurement]),
        category = null,
        codeSystem = HealthConnectContract.LOINC,
        code = code,
        display = display,
        unitCode = unit,
        unitDisplay = unit,
    ),
    extract,
)

internal fun healthConnectNutrient(
    measurement: String,
    profile: String,
    display: String,
    unit: String,
    extract: (NutritionRecord) -> Double?,
): NutrientSpec = NutrientSpec(
    measurement,
    MobileQuantitySpec(
        profile = profile,
        category = null,
        codeSystem = HealthConnectContract.HEALTH_CONNECT_MEASUREMENT,
        code = measurement,
        display = display,
        unitCode = unit,
        unitDisplay = unit,
        adapterSpecific = true,
    ),
    extract,
)

/** Every admitted Nutrition output in the catalog's measurement order; absent fields emit nothing. */
internal val NUTRIENT_OUTPUTS: List<NutrientSpec> = listOf(
    mobileNutrient("dietary-biotin", "Dietary biotin", "ug") { it.biotin?.inMicrograms },
    mobileNutrient("dietary-caffeine", "Dietary caffeine", "mg") { it.caffeine?.inMilligrams },
    mobileNutrient("dietary-calcium", "Dietary calcium", "mg") { it.calcium?.inMilligrams },
    loincNutrient("dietary-carbohydrates", "9060-5", "Carbohydrate intake Measured", "g") {
        it.totalCarbohydrate?.inGrams
    },
    mobileNutrient("dietary-chloride", "Dietary chloride", "mg") { it.chloride?.inMilligrams },
    mobileNutrient("dietary-cholesterol", "Dietary cholesterol", "mg") { it.cholesterol?.inMilligrams },
    mobileNutrient("dietary-chromium", "Dietary chromium", "ug") { it.chromium?.inMicrograms },
    mobileNutrient("dietary-copper", "Dietary copper", "ug") { it.copper?.inMicrograms },
    loincNutrient("dietary-energy", "9052-2", "Calorie intake total", "kcal") { it.energy?.inKilocalories },
    healthConnectNutrient(
        "dietary-energy-from-fat",
        HealthConnectContract.HEALTH_CONNECT_DIETARY_ENERGY_FROM_FAT_PROFILE,
        "Nutrition energy from fat",
        "kcal",
    ) { it.energyFromFat?.inKilocalories },
    mobileNutrient("dietary-fat-monounsaturated", "Dietary monounsaturated fat", "g") {
        it.monounsaturatedFat?.inGrams
    },
    mobileNutrient("dietary-fat-polyunsaturated", "Dietary polyunsaturated fat", "g") {
        it.polyunsaturatedFat?.inGrams
    },
    mobileNutrient("dietary-fat-saturated", "Dietary saturated fat", "g") { it.saturatedFat?.inGrams },
    loincNutrient("dietary-fat-total", "9067-0", "Fat intake Measured", "g") { it.totalFat?.inGrams },
    healthConnectNutrient(
        "dietary-fat-trans",
        HealthConnectContract.HEALTH_CONNECT_DIETARY_FAT_TRANS_PROFILE,
        "Nutrition trans fat",
        "g",
    ) { it.transFat?.inGrams },
    healthConnectNutrient(
        "dietary-fat-unsaturated",
        HealthConnectContract.HEALTH_CONNECT_DIETARY_FAT_UNSATURATED_PROFILE,
        "Nutrition unsaturated fat",
        "g",
    ) { it.unsaturatedFat?.inGrams },
    mobileNutrient("dietary-fiber", "Dietary fiber", "g") { it.dietaryFiber?.inGrams },
    mobileNutrient("dietary-folate", "Dietary folate", "ug") { it.folate?.inMicrograms },
    healthConnectNutrient(
        "dietary-folic-acid",
        HealthConnectContract.HEALTH_CONNECT_DIETARY_FOLIC_ACID_PROFILE,
        "Nutrition folic acid",
        "ug",
    ) { it.folicAcid?.inMicrograms },
    mobileNutrient("dietary-iodine", "Dietary iodine", "ug") { it.iodine?.inMicrograms },
    mobileNutrient("dietary-iron", "Dietary iron", "mg") { it.iron?.inMilligrams },
    mobileNutrient("dietary-magnesium", "Dietary magnesium", "mg") { it.magnesium?.inMilligrams },
    mobileNutrient("dietary-manganese", "Dietary manganese", "mg") { it.manganese?.inMilligrams },
    mobileNutrient("dietary-molybdenum", "Dietary molybdenum", "ug") { it.molybdenum?.inMicrograms },
    mobileNutrient("dietary-niacin", "Dietary niacin", "mg") { it.niacin?.inMilligrams },
    mobileNutrient("dietary-pantothenic-acid", "Dietary pantothenic acid", "mg") {
        it.pantothenicAcid?.inMilligrams
    },
    mobileNutrient("dietary-phosphorus", "Dietary phosphorus", "mg") { it.phosphorus?.inMilligrams },
    mobileNutrient("dietary-potassium", "Dietary potassium", "mg") { it.potassium?.inMilligrams },
    loincNutrient("dietary-protein", "9080-3", "Protein intake Measured", "g") { it.protein?.inGrams },
    mobileNutrient("dietary-riboflavin", "Dietary riboflavin", "mg") { it.riboflavin?.inMilligrams },
    mobileNutrient("dietary-selenium", "Dietary selenium", "ug") { it.selenium?.inMicrograms },
    mobileNutrient("dietary-sodium", "Dietary sodium", "mg") { it.sodium?.inMilligrams },
    mobileNutrient("dietary-sugar", "Dietary sugar", "g") { it.sugar?.inGrams },
    mobileNutrient("dietary-thiamin", "Dietary thiamin", "mg") { it.thiamin?.inMilligrams },
    mobileNutrient("dietary-vitamin-a", "Dietary vitamin A", "ug") { it.vitaminA?.inMicrograms },
    mobileNutrient("dietary-vitamin-b12", "Dietary vitamin B12", "ug") { it.vitaminB12?.inMicrograms },
    mobileNutrient("dietary-vitamin-b6", "Dietary vitamin B6", "mg") { it.vitaminB6?.inMilligrams },
    mobileNutrient("dietary-vitamin-c", "Dietary vitamin C", "mg") { it.vitaminC?.inMilligrams },
    mobileNutrient("dietary-vitamin-d", "Dietary vitamin D", "ug") { it.vitaminD?.inMicrograms },
    mobileNutrient("dietary-vitamin-e", "Dietary vitamin E", "mg") { it.vitaminE?.inMilligrams },
    mobileNutrient("dietary-vitamin-k", "Dietary vitamin K", "ug") { it.vitaminK?.inMicrograms },
    mobileNutrient("dietary-zinc", "Dietary zinc", "mg") { it.zinc?.inMilligrams },
)
