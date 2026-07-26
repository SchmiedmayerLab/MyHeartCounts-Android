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
