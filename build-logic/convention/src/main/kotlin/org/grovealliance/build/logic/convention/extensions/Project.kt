//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.build.logic.convention.extensions

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.grovealliance.build.logic.convention.model.PluginId
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

private val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.findBundle(name: String) = libs.findBundle(name).get()

internal fun Project.findLibrary(name: String) = libs.findLibrary(name).get()

internal fun Project.findVersion(name: String) = libs.findVersion(name).get().toString()

internal fun Project.apply(pluginId: PluginId) = plugins.apply(pluginId.id)

internal fun Project.isApp() = plugins.hasPlugin(PluginId.ANDROID_APPLICATION.id)

internal fun Project.isLibrary() = plugins.hasPlugin(PluginId.ANDROID_LIBRARY.id)

internal fun Project.hasAndroidTests() = projectDir.resolve("src/androidTest").exists()
internal fun Project.hasScreenshotTests() = projectDir.resolve("src/test/snapshots").exists()

inline fun <reified T : Any> Project.extension(configBlock: T.() -> Unit) {
    extensions.getByType<T>().apply(configBlock)
}

internal fun Project.android(configBlock: CommonExtension.() -> Unit) {
    when {
        isApp() -> extension<ApplicationExtension>(configBlock)
        isLibrary() -> extension<LibraryExtension>(configBlock)
        else -> error("commonExtensions was called before setting the module type plugin")
    }
}
