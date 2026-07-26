//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.build.logic.convention.plugins

import edu.stanford.spezi.build.logic.convention.extensions.android
import edu.stanford.spezi.build.logic.convention.extensions.findLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class DesugaringConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        android {
            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }

        dependencies {
            add("coreLibraryDesugaring", findLibrary("android-desugaring"))
        }
    }
}
