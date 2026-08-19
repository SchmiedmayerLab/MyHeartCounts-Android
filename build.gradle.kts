//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) version libs.versions.kotlin apply false
    alias(libs.plugins.detekt) version libs.versions.detekt
    alias(libs.plugins.dokka) version libs.versions.dokka
    alias(libs.plugins.google.devtools.ksp) version libs.versions.kspVersion apply false
    jacoco
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.paparazzi) apply false
}

subprojects {
    setupDokka()
    setupDetekt()
    setupJacoco()
}

installCustomTasks()

dokka {
    moduleName.set("Grove Documentation")
    dokkaPublications.html {
        includes.from("README.md")
    }
}

// Dokka v2 aggregates by depending on each module rather than by wiring partial tasks together.
dependencies {
    subprojects.forEach { dokka(project(it.path)) }
}

tasks.named("dokkaGeneratePublicationHtml") {
    dependsOn("copyDocumentationImages")
}

fun Project.setupDokka() {
    apply(plugin = rootProject.libs.plugins.dokka.get().pluginId)

    dokka {
        // Dokka v2 registers source sets from the Kotlin plugin, which the Android plugin does not
        // provide; without this the modules document nothing.
        if (file("src/main/kotlin").exists()) {
            dokkaSourceSets.maybeCreate("main").sourceRoots.from(file("src/main/kotlin"))
        }
        dokkaSourceSets.configureEach {
            enableAndroidDocumentationLink.set(true)
            skipDeprecated.set(true)
            skipEmptyPackages.set(true)
            documentedVisibilities.set(setOf(VisibilityModifier.Public))
            jdkVersion.set(JavaVersion.VERSION_21.majorVersion.toInt())
            if (file("README.md").exists()) {
                includes.from("README.md")
            }
        }
    }
}

fun Project.setupDetekt() {
    val libs = rootProject.libs
    apply(plugin = libs.plugins.detekt.get().pluginId)
    detekt {
        toolVersion = libs.versions.detekt.get()
        config.setFrom("$rootDir/internal/detekt-config.yml")
        autoCorrect = true
        ignoreFailures = false
        source.setFrom(
            files(
                "src/main",
                "src/test",
                "src/androidTest",
                "build.gradle.kts"
            )
        )
    }

    dependencies {
        detektPlugins(libs.detekt.formatting)
    }

    tasks.withType<Detekt> {
        jvmTarget = JavaVersion.VERSION_21.toString()
        reports {
            xml.required.set(true)
            html.required.set(true)
            txt.required.set(true)
            sarif.required.set(true)
        }
    }
}

fun Project.enableAndroidTestCoverage() {
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension>("android") {
            buildTypes.getByName("debug").enableAndroidTestCoverage = true
        }
    }
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
            buildTypes.getByName("debug").enableAndroidTestCoverage = true
        }
    }
}

fun Project.setupJacoco() {
    apply(plugin = "jacoco")

    // Instrumented coverage is on by default. A module whose dependencies JaCoCo cannot instrument
    // turns it off in its own build file and says why.
    enableAndroidTestCoverage()
    val buildDir = layout.buildDirectory.get()
    val coverageExclusions = listOf(
        // Android
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Activity.class",
        "**/*Application.class",
        "**/di/*Module.*",
    )
    val reportTask = tasks.register("jacocoCoverageReport", JacocoReport::class.java) {
        classDirectories.setFrom(
            fileTree("$buildDir/intermediates/classes/debug") {
                exclude(coverageExclusions)
            } + fileTree("$buildDir/tmp/kotlin-classes/debug") {
                exclude(coverageExclusions)
            }
        )
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
        sourceDirectories.setFrom(files("$projectDir/src/main"))

        executionData.setFrom(
            files("$buildDir/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec") +
                fileTree("$buildDir/outputs/code_coverage/debugAndroidTest/connected") {
                    include("**/*.ec")
                }
        )
        doLast {
            println("Jacoco report generated in: ${reports.html.outputLocation.get()}")
        }
    }

    tasks.withType<Test>().configureEach {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
        finalizedBy(reportTask)
    }
}

/**
 * Installs all custom tasks defined in /gradle/tasks
 */
fun Project.installCustomTasks() {
    val tasksDir = File("$rootDir/gradle/tasks")
    if (tasksDir.exists() && tasksDir.isDirectory) {
        tasksDir.listFiles { file -> file.extension == "kts" }
            ?.forEach { file -> apply(from = file) }
    }

    tasks.register<Copy>("copyDocumentationImages") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        fileTree("$rootDir").matching {
            include("**/screens/*.jpg")
        }.forEach { file ->
            val relativePath = file.parentFile.relativeTo(File("$rootDir"))
            from(file.parentFile) {
                include("*.jpg")
            }
            into("$buildDir/dokka/htmlMultiModule/$relativePath")
        }
    }
}
