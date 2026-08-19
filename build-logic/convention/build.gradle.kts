//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "org.grovealliance.build.logic"

val javaVersion = JavaVersion.VERSION_21

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
    }
}

dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradle)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

fun NamedDomainObjectContainer<PluginDeclaration>.conventionPlugin(id: String, className: String) {
    register(className) {
        this.id = "grove.$id"
        implementationClass = "org.grovealliance.build.logic.convention.plugins.$className"
    }
}

/**
 * Plugins specific to the My Heart Counts application rather than to the Grove framework modules.
 */
fun NamedDomainObjectContainer<PluginDeclaration>.applicationPlugin(id: String, className: String) {
    register(className) {
        this.id = "mhc.$id"
        implementationClass = "edu.stanford.myheartcounts.build.logic.convention.plugins.$className"
    }
}

gradlePlugin {
    plugins {
        // Please keep plugins sorted. Select all method calls below and in Android Studio `Edit > Sort Lines`
        applicationPlugin(id = "studybundle", className = "MHCStudyBundleConventionPlugin")
        applicationPlugin(id = "studybundlefixture", className = "MHCStudyBundleFixtureConventionPlugin")
        conventionPlugin(id = "application", className = "GroveApplicationConventionPlugin")
        conventionPlugin(id = "base", className = "GroveBaseConfigConventionPlugin")
        conventionPlugin(id = "compose", className = "GroveComposeConventionPlugin")
        conventionPlugin(id = "desugaring", className = "DesugaringConventionPlugin")
        conventionPlugin(id = "library", className = "GroveLibraryConventionPlugin")
        conventionPlugin(id = "serialization", className = "GroveSerializationConventionPlugin")
    }
}
