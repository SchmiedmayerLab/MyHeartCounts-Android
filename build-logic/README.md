<!--

This source file is part of the My Heart Counts Android open-source project

SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)

SPDX-License-Identifier: MIT
-->

# Application Build Logic

## Overview

`build-logic` holds the convention plugin specific to the My Heart Counts application. The Grove
modules the application builds on carry their own conventions in
[Grove Kotlin](https://github.com/SchmiedmayerLab/grove-kotlin).

## Usage

To apply the plugin, add the following to your `build.gradle.kts`:

```kotlin
plugins {
  alias(libs.plugins.mhc.studybundle)
}
```

## Plugins

- [`mhc.studybundle`](convention/src/main/kotlin/edu/stanford/myheartcounts/build/logic/convention/plugins/MHCStudyBundleConventionPlugin.kt)
  - Exports the study bundle from the `MyHeartCounts-StudyDefinitions` submodule with the same Swift
    exporter the iOS application uses, and packages it into the assets of every application variant.
    Any task that assembles the application runs the export, and re-runs it only once the submodule
    moves.
