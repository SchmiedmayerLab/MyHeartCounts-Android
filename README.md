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


### Application Structure

- **Design System**: Provides a cohesive user interface and user experience
  components. [View the UI modules](./ui/)
- **Account**: Provides Account management components. [View the module](./account/)
- **Onboarding**: Provides Onboarding screens for the
  application. [View the module](./onboarding/)
- **Contact**: Provides Contact screens. [View the module](./contact/)
- **Health Connect FHIR R4**: Produces deterministic Grove-conformant Mobile and
  Health Connect resource graphs from the closed supported record inventory.
  [View the module contract](./health-fhir/README.md)

### Study Bundle

The app packages the My Heart Counts study bundle as a zstd-compressed archive — the same format the
storage bucket serves in production — so a build has a study to run before it reaches the bucket, and
bundled and downloaded bundles unpack through one code path. The bundle is not committed here: the
[`MyHeartCounts-StudyDefinitions`](https://github.com/SchmiedmayerLab/MyHeartCounts-StudyDefinitions)
submodule pins the study definitions, and Gradle exports the archive from them with the same Swift
exporter the iOS application uses, so both platforms package what the pinned commit describes.

```bash
git submodule update --init
```

`./gradlew :myheartcounts:exportStudyBundle` refreshes the assets; any task that assembles the
application runs the export itself, and re-runs it only once the submodule moves. The
`:study-definition` unit tests export their archive fixture the same way instead of committing a
generated artifact. The export needs a Swift toolchain: it uses the one on `PATH`, and otherwise
runs in the container named by `myHeartCounts.studyBundle.swiftImage`. Force either with
`-PstudyBundleToolchain=swift` or `-PstudyBundleToolchain=docker`.

Dependabot advances the submodule to the head of `main` weekly, so the bundle moves forward through a
reviewable commit.

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
