//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.build.logic.convention.plugins

import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import edu.stanford.spezi.build.logic.convention.model.PluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Exports the study bundle archive into a library module's unit test resources.
 *
 * The tests unpack the real exporter output rather than a committed artifact; the export is
 * deterministic, so the fixture is exactly reproducible.
 */
class MHCStudyBundleFixtureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin(PluginId.ANDROID_LIBRARY.id) {
            val export = tasks.register<MHCExportStudyBundle>("exportStudyBundleTestFixture") {
                group = BUILD_SETUP_GROUP
                description = "Exports the study bundle archive the unit tests unpack."
                applyStudyBundleConventions(this@with)
                scratchDirectory.set(rootProject.layout.buildDirectory.dir("studyBundleFixture"))
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                onVariants { variant ->
                    variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.sources?.resources?.addGeneratedSourceDirectory(
                        export,
                        MHCExportStudyBundle::outputDirectory
                    )
                }
            }
        }
    }
}
