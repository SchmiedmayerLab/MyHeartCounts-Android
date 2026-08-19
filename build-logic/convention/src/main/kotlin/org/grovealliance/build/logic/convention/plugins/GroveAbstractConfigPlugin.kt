//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.build.logic.convention.plugins

import org.grovealliance.build.logic.convention.extensions.apply
import org.grovealliance.build.logic.convention.extensions.findBundle
import org.grovealliance.build.logic.convention.extensions.implementation
import org.grovealliance.build.logic.convention.extensions.testImplementation
import org.grovealliance.build.logic.convention.model.PluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate

abstract class GroveAbstractConfigPlugin(private val modulePlugin: PluginId) : Plugin<Project> {
    private val defaultConfig by lazy { GroveBaseConfigConventionPlugin() }

    override fun apply(project: Project) = with(project) {
        apply(modulePlugin)

        defaultConfig.apply(this)

        dependencies {
            if (path != LOGGING_MODULE) {
                implementation(project(LOGGING_MODULE))
            }
            testImplementation(findBundle("unit-testing"))
        }
    }

    private companion object {
        const val LOGGING_MODULE = ":core-logging"
    }
}
