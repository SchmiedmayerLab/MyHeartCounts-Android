//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.build.logic.convention.model

enum class PluginId(val id: String) {
    ANDROID_APPLICATION(id = "com.android.application"),
    ANDROID_LIBRARY(id = "com.android.library"),
    COMPOSE_COMPILER("org.jetbrains.kotlin.plugin.compose"),
    SERIALIZATION("org.jetbrains.kotlin.plugin.serialization"),
    PAPARAZZI("app.cash.paparazzi"),
}
