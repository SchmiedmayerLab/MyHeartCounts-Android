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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Please keep the projects sorted. Select all method calls below and in Android Studio `Edit > Sort Lines`
include(":account")
include(":account-firebase")
include(":consent")
include(":contact")
include(":core")
include(":core-coroutines")
include(":core-lifecycle")
include(":core-logging")
include(":core-time")
include(":core-viewmodel")
include(":foundation")
include(":health")
include(":health-fhir")
include(":markdown")
include(":myheartcounts")
include(":onboarding")
include(":questionnaire")
include(":resources")
include(":sample-app")
include(":scheduler")
include(":storage-credential")
include(":storage-local")
include(":study")
include(":study-definition")
include(":testing-concurrency")
include(":testing-core")
include(":testing-screenshot")
include(":testing-ui")
include(":ui")
include(":ui-account")
include(":ui-scheduler")
include(":ui-theme")
include(":ui-validation")
