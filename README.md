<!--

This source file is part of the My Heart Counts Android open-source project

SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)

SPDX-License-Identifier: MIT

-->

# My Heart Counts Android

[![Build and Test](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/actions/workflows/build-test-analyze.yml/badge.svg)](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/actions/workflows/build-test-analyze.yml)
[![Deployment](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/actions/workflows/deployment.yml/badge.svg)](https://github.com/SchmiedmayerLab/MyHeartCounts-Android/actions/workflows/deployment.yml)
[![REUSE status](https://api.reuse.software/badge/github.com/SchmiedmayerLab/MyHeartCounts-Android)](https://api.reuse.software/info/github.com/SchmiedmayerLab/MyHeartCounts-Android)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE.md)

Kotlin &amp; Android Version of the My Heart Counts ecosystem.

This repository holds the application. The accounts, onboarding, consent, questionnaire, scheduling,
study, and design system modules it is built from live in
[Grove Kotlin](https://github.com/SchmiedmayerLab/grove-kotlin).

### Grove

Grove is pinned as a submodule and built from source, so a Grove change reaches the app without a
publishing step in between:

```bash
git submodule update --init
```

The application depends on Grove by coordinate — `org.grovealliance:<module>`, declared in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml) — and the submodule commit decides which Grove
that is. While a feature is in flight, move the submodule to its branch and commit the new pin alongside
the code that needs it:

```bash
git -C grove-kotlin switch feature/the-feature
git add grove-kotlin
```

A pin merged into `main` has to be a commit that is on Grove's `main`, which CI verifies: a commit that
only existed on a branch is gone once Grove squash-merges and deletes it. To build against a Grove
checkout somewhere else, set `grove.path` in `~/.gradle/gradle.properties` rather than editing the build.

Grove and the application have to pin the same Android Gradle Plugin and Kotlin versions, because a
composite build loads the plugins of both into one process. Gradle checks this as it configures and says
so when the two drift apart.

#### Landing a change across both repositories

A change that spans Grove and the application lands in a fixed order, because the pin has to name a
commit that is on Grove's `main`. Merge the Grove pull request first, then update the clone the pin is
read from:

```bash
git -C <grove clone> switch main && git -C <grove clone> pull
```

A squash merge creates a new commit on `main`, so the branch commit the application was tested against
never becomes an ancestor of it. That is why the pin cannot move ahead of the merge.

Move the pin and commit it as its own reviewable change:

```bash
git submodule update --remote grove-kotlin   # follows `branch = main` from .gitmodules
git add grove-kotlin
git commit -m "Bump the Grove pin"
```

`git submodule update --remote` takes the head of the tracked branch, which is always a commit CI will
accept. `git submodule status` prints the pin, and its leading character is the state — a space means
the checkout matches the pin, `+` that it differs, `-` that it is not initialised yet.

Then build the application against the pinned snapshot rather than against a local checkout:

```bash
./gradlew -Pgrove.path=grove-kotlin :myheartcounts:assembleDebug
```

This is the step that catches a stale or unpushed pin. With `grove.path` pointing at a Grove checkout of
your own, every ordinary build is green regardless of what the pin names; the flag overrides it for one
invocation. A pin that predates the Grove change fails while resolving dependencies, because the
composite build substitutes an `org.grovealliance` coordinate only while a project carrying it is part
of the included build:

```
Could not find org.grovealliance:firebase:0.1.0
```

Open the application pull request once that build passes. Opening it earlier is fine as a draft, but its
checks fail until the pin moves.

### Study Bundle

The app packages the My Heart Counts study bundle as a zstd-compressed archive — the same format the
storage bucket serves in production — so a build has a study to run before it reaches the bucket, and
bundled and downloaded bundles unpack through one code path. The bundle is not committed here: the
[`MyHeartCounts-StudyDefinitions`](https://github.com/SchmiedmayerLab/MyHeartCounts-StudyDefinitions)
submodule pins the study definitions, and Gradle exports the archive from them with the same Swift
exporter the iOS application uses, so both platforms package what the pinned commit describes.

`./gradlew :myheartcounts:exportStudyBundle` refreshes the assets; any task that assembles the
application runs the export itself, and re-runs it only once the submodule moves. The unit tests read
that exported archive rather than a committed artifact, which is what holds the exporter and the Kotlin
decoder to the same schema. The export needs a Swift toolchain: it uses the one on `PATH`, and otherwise
runs in the container named by `myHeartCounts.studyBundle.swiftImage`. Force either with
`-PstudyBundleToolchain=swift` or `-PstudyBundleToolchain=docker`.

Dependabot advances both submodules to the head of the branch they track weekly, so the bundle and Grove
move forward through a reviewable commit.

### Continuous Integration and Delivery Setup

#### Google Play Internal Deployment

The `main` branch deploys to Google Play internal testing after the deployment gate is enabled.
Publishing a semantic-version GitHub release deploys the same application to production.

The application ID is permanently configured as `edu.stanford.myheartcounts`. Deployment uses
Fastlane, GitHub Actions, the Stanford Play Console account, and the
`fastlane-deployment-426021` Google Cloud project.

See the [Google Play deployment guide](./deployment/README.md) for the infrastructure inventory,
secret-handling rules, bootstrap procedure, and release workflow.

## Contributing

Contributions to this project are welcome. Please make sure to read the [contribution guidelines](https://github.com/SchmiedmayerLab/.github/blob/main/CONTRIBUTING.md) and the [contributor covenant code of conduct](https://github.com/SchmiedmayerLab/.github/blob/main/CODE_OF_CONDUCT.md) first. You can find a list of contributors in the [CONTRIBUTORS.md](CONTRIBUTORS.md) file.

## License

This project is licensed under the MIT License. See [LICENSE.md](LICENSE.md) for more information.

## Citation

If you use this software, please cite it using the metadata in [CITATION.cff](CITATION.cff), which GitHub surfaces through the [*Cite this repository*](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-citation-files) button.

## Our Research

For more information, visit the [Schmiedmayer Lab GitHub organization](https://github.com/SchmiedmayerLab).

![Schmiedmayer Lab](https://raw.githubusercontent.com/SchmiedmayerLab/.github/main/assets/footer-light.png#gh-light-mode-only)
![Schmiedmayer Lab](https://raw.githubusercontent.com/SchmiedmayerLab/.github/main/assets/footer-dark.png#gh-dark-mode-only)
