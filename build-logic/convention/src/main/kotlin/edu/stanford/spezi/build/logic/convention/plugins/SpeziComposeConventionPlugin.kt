//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.build.logic.convention.plugins

import edu.stanford.spezi.build.logic.convention.extensions.android
import edu.stanford.spezi.build.logic.convention.extensions.androidTestImplementation
import edu.stanford.spezi.build.logic.convention.extensions.apply
import edu.stanford.spezi.build.logic.convention.extensions.debugImplementation
import edu.stanford.spezi.build.logic.convention.extensions.findBundle
import edu.stanford.spezi.build.logic.convention.extensions.findLibrary
import edu.stanford.spezi.build.logic.convention.extensions.hasScreenshotTests
import edu.stanford.spezi.build.logic.convention.extensions.implementation
import edu.stanford.spezi.build.logic.convention.extensions.testImplementation
import edu.stanford.spezi.build.logic.convention.model.PluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class SpeziComposeConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val includePaparazzi = newModule() || hasScreenshotTests()
        apply(PluginId.COMPOSE_COMPILER)
        if (includePaparazzi) apply(PluginId.PAPARAZZI)

        android {
            buildFeatures {
                compose = true
            }

            dependencies {
                val composeBom = platform(findLibrary("compose-bom"))
                implementation(composeBom)
                implementation(findLibrary("androidx-activity-compose"))
                implementation(findLibrary("androidx-appcompat"))
                implementation(findLibrary("navigation-compose"))
                implementation(findLibrary("androidx-compose-material-icons"))
                implementation(findLibrary("androidx-core-ktx"))
                implementation(findLibrary("coil-compose"))
                implementation(findLibrary("coil-network"))
                implementation(findLibrary("compose-foundation"))
                implementation(findLibrary("compose-material3"))
                implementation(findLibrary("compose-ui"))
                implementation(findLibrary("compose-ui-tooling-preview"))

                implementation(findLibrary("androidx-lifecycle-view-model-ktx"))
                implementation(findLibrary("androidx-lifecycle-viewmodel-savedstate"))
                implementation(findLibrary("androidx-lifecycle-viewmodel-compose"))

                androidTestImplementation(composeBom)
                androidTestImplementation(findBundle("unit-testing"))
                androidTestImplementation(findBundle("integration-testing"))
                androidTestImplementation(findLibrary("compose-ui-test"))
                debugImplementation(findLibrary("compose-ui-tooling"))
                debugImplementation(findLibrary("compose-ui-test-manifest"))

                if (includePaparazzi) testImplementation(project(SCREENSHOT_TESTING_MODULE))
            }
        }
    }

    private fun Project.newModule(): Boolean {
        return path == NEW_MODULE
    }

    private companion object {
        // Module bootstrapped with Paparazzi even before it has a src/test/snapshots/ directory,
        // so its very first screenshot tests can generate their baseline snapshots.
        const val NEW_MODULE = ":onboarding"
        const val SCREENSHOT_TESTING_MODULE = ":testing-screenshot"
    }
}
