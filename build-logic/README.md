<!--

This source file is part of the My Heart Counts Android open-source project

SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)

SPDX-License-Identifier: MIT
-->

# Module Convention Plugins

## Overview

The `build-logic` folder contains Grove specific convention plugins that are used for common
module configurations.

## Features

- **Standardization**: Ensures that all library modules follow a specific convention, which improves
  consistency across this projects.
- **Improved reusability**: The clear separation and modularization of the build logic promotes the
  reusability of code within the project or across
  projects [(idiomatic-gradle)](https://github.com/jjohannes/idiomatic-gradle).
- **Reduced complexity and cognitive load**: Convention plugins significantly simplify build scripts
  by encapsulating commonly used conventions and configurations. This reduces the complexity of
  individual build scripts and reduces the cognitive load on developers, making it easier to
  understand and manage the build
  process [(square)](https://developer.squareup.com/blog/herding-elephants/).
- **Improved build performance**: Unlike buildSrc, which is compiled and checked on every build,
  convention plugins can be precompiled and treated like any other dependency. This avoids the
  performance penalty of recompiling the build logic with each build and leads to faster build
  times, especially in large
  projects [(square)](https://developer.squareup.com/blog/herding-elephants/).
- **Increased modularity and isolation**: By using convention plugins, the build logic can be
  modularized and isolated from the rest of the build script. This allows for cleaner code
  management and reduces the risk of bugs propagating through the build script. It also makes it
  easier to test the build logic [(square)](https://developer.squareup.com/blog/herding-elephants/).

## Usage

To apply a convention plugin, add the following to your `build.gradle.kts`:

```kotlin
plugins {
  alias(libs.plugins.grove.application)
  alias(libs.plugins.grove.compose)
}
```

## Plugins

Current list of convention plugins:

- [`grove.application`](convention/src/main/kotlin/org/grovealliance/build/logic/convention/plugins/GroveApplicationConventionPlugin.kt)
  - Convention plugin that applies by default `com.android.application` and `org.jetbrains.kotlin.android`. Additionally it applies the default project configuration of `grove.base` plugin.
- [`grove.compose`](convention/src/main/kotlin/org/grovealliance/build/logic/convention/plugins/GroveComposeConventionPlugin.kt)
  - - Convention plugin that applies the required configuration and dependencies needed for `Compose`. Note that you need to additionally apply either `grove.application` or `grove.library` plugins.
- [`grove.base`](convention/src/main/kotlin/org/grovealliance/build/logic/convention/plugins/GroveBaseConfigConventionPlugin.kt)
  - Base convention plugin used by all modules of the project. It makes sure to configure consistently versions and compile options. This plugin is advisable to be used, for modules that are added as a dependency in one of the `grove.application` or `grove.library` plugins.
- [`grove.library`](convention/src/main/kotlin/org/grovealliance/build/logic/convention/plugins/GroveLibraryConventionPlugin.kt)
  - Convention plugin that applies by default `com.android.library` and `org.jetbrains.kotlin.android`. Additionally it applies the default project configuration of `grove.base` plugin.

