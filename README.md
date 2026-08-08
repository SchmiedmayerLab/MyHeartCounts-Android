<!--

This source file is part of the My Heart Counts open-source project

SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)

SPDX-License-Identifier: MIT
-->

# My Heart Counts Android

Kotlin &amp; Android Version of the My Heart Counts ecosystem.


### Application Structure

- **Design System**: Provides a cohesive user interface and user experience
  components. [View the UI modules](./ui/)
- **Account**: Provides Account management components. [View the module](./account/)
- **Onboarding**: Provides Onboarding screens for the
  application. [View the module](./onboarding/)
- **Contact**: Provides Contact screens. [View the module](./contact/)

### Study Bundle

The app packages the study it runs — definition, consent, questionnaires, articles, explainers. The
files are **not checked in**: a pin names a published archive and its checksum, and the build fetches
and unpacks it into the app's assets.

`definition.json` is compiled from Swift, so exporting it needs macOS with Xcode while the Android CI
runs on Linux. The export therefore happens once, in its own workflow, and everything downstream
consumes the archive it publishes.

| File | Role |
|------|------|
| [`gradle.properties`](./gradle.properties) | `myHeartCounts.studyBundle.*`: bundle name, pin path, source repository, release tag prefix — read by the build, the app and the workflow |
| [`myheartcounts/study-bundle.pin.json`](./myheartcounts/study-bundle.pin.json) | The pinned archive: source ref and commit, study revision, schema version, URL and SHA-256 |
| [`.github/workflows/publish-study-bundle.yml`](./.github/workflows/publish-study-bundle.yml) | Exports a bundle and publishes it as a release, with the pin snippet to copy |
| [`MHCStudyBundleConventionPlugin`](./build-logic/convention/src/main/kotlin/edu/stanford/myheartcounts/build/logic/convention/plugins/MHCStudyBundleConventionPlugin.kt) | The `mhc.studybundle` plugin and its `fetch<Variant>StudyBundle` task |

#### Updating

1. Run **Publish Study Bundle** with the `ref` to export — a tag such as `0.2.3`, a branch such as
   `main`, or a commit SHA. Prefer a tag: it is what the iOS app pins.
2. It publishes an `mhc-study-bundle-<ref>` release whose notes carry the study revision, schema
   version, source commit and checksum.
3. Merge the pull request it opens, or copy the snippet from the release into the pin yourself.

Never hand-write the pin, and never delete a release a pin still names — an export is not
byte-reproducible, so the checksum only ever matches one published archive.

#### Building

`fetch<Variant>StudyBundle` runs with `assembleDebug` and the tests: it downloads the pinned archive,
rejects it on a checksum mismatch, and unpacks it under `myheartcounts/build/generated/assets/`. A
cold build needs network access; to avoid that, point it at a local archive, which is checked just as
strictly:

```bash
./gradlew assembleDebug -PstudyBundleArchive=/path/to/mhcStudyBundle.spezistudybundle.zip
```

Set `studyBundleArchive` in `~/.gradle/gradle.properties` to have Android Studio pick it up too.

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

Contributions to this project are welcome. Please make sure to read the [contribution guide](https://github.com/SchmiedmayerLab/.github/blob/main/CONTRIBUTING.md) and the [Contributor Covenant Code of Conduct](https://github.com/SchmiedmayerLab/.github/blob/main/CODE_OF_CONDUCT.md) first.


## License

This project is licensed under the MIT License. See [Licenses](LICENSES) for more information.


## Our Research

For more information, visit the [Schmiedmayer Lab GitHub organization](https://github.com/SchmiedmayerLab).

![Stanford and Stanford Medicine logos](https://raw.githubusercontent.com/SchmiedmayerLab/.github/main/assets/stanford-footer-light.png#gh-light-mode-only)
![Stanford and Stanford Medicine logos](https://raw.githubusercontent.com/SchmiedmayerLab/.github/main/assets/stanford-footer-dark.png#gh-dark-mode-only)
