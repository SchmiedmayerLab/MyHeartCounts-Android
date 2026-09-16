//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyHeartCounts-Android"

fun catalogVersion(catalog: File, name: String): String =
    Regex("^$name\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE)
        .find(catalog.readText())
        ?.groupValues
        ?.get(1)
        ?: error("No '$name' version in $catalog")

// Grove is built from source out of the submodule, so a Grove change reaches the app without a
// publishing step in between. `grove.path` builds against a checkout somewhere else instead.
val groveDirectory = rootDir.resolve(providers.gradleProperty("grove.path").getOrElse("grove-kotlin"))

// Every task needs Grove to configure, so a checkout that skipped submodules populates it here rather
// than failing. The study bundle export does the same for the study definitions.
if (!groveDirectory.resolve("settings.gradle.kts").isFile) {
    runCatching {
        providers.exec {
            workingDir = rootDir
            commandLine("git", "submodule", "update", "--init", "--", groveDirectory.path)
        }.result.get().assertNormalExitValue()
    }
}

require(groveDirectory.resolve("settings.gradle.kts").isFile) {
    "No Grove Kotlin checkout at $groveDirectory. Run 'git submodule update --init', or set grove.path."
}
includeBuild(groveDirectory)

// A composite build loads the plugins of both builds into one process, so the two builds have to
// agree on the versions that carry them.
listOf("agp", "kotlin").forEach { name ->
    val app = catalogVersion(rootDir.resolve("gradle/libs.versions.toml"), name)
    val grove = catalogVersion(groveDirectory.resolve("gradle/libs.versions.toml"), name)
    check(app == grove) {
        "The app pins $name $app and Grove pins $grove. Align both version catalogs."
    }
}

include(":myheartcounts")
