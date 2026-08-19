//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.build.logic.convention.plugins

import org.grovealliance.build.logic.convention.extensions.apply
import org.grovealliance.build.logic.convention.extensions.findLibrary
import org.grovealliance.build.logic.convention.extensions.implementation
import org.grovealliance.build.logic.convention.model.PluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class GroveSerializationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        apply(PluginId.SERIALIZATION)

        dependencies {
            implementation(findLibrary("kotlinx-serialization-json"))
        }
    }
}
