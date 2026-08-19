//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.StudyBundle
import java.io.File

/**
 * Shared test data for study definitions, backed by an example `.studybundle` on the
 * `testFixtures` classpath.
 *
 * The example study contains one component of each kind (informational, questionnaire, timed walking
 * test, custom active task, health data collection) and schedules exercising the once-event, daily,
 * and weekly cases.
 */
object StudyBundleFixtures {
    private const val EXAMPLE_BUNDLE_RESOURCE = "fixtures/example.studybundle"

    /**
     * The directory of the example study bundle.
     */
    fun exampleBundleDir(): File {
        val url = requireNotNull(javaClass.getResource("/$EXAMPLE_BUNDLE_RESOURCE")) {
            "Missing test fixture: $EXAMPLE_BUNDLE_RESOURCE"
        }
        return File(url.toURI())
    }

    /**
     * The opened example [StudyBundle].
     */
    fun exampleBundle(): StudyBundle = StudyBundle.open(exampleBundleDir())

    /**
     * The raw `definition.json` of the example bundle.
     */
    fun exampleDefinitionJson(): String = File(exampleBundleDir(), "definition.json").readText()
}
