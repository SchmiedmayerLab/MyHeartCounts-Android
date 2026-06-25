# My Heart Counts Android

Kotlin &amp; Android Version of the My Heart Counts ecosystem.


### Application Structure

- **Design System**: Provides a cohesive user interface and user experience
  components. [Read More](./core/design/README.md)
- **Account**: Provides Account management components. [Read More](./modules/account/README.md)
- **Onboarding**: Provides Onboarding screens for the
  application. [Read More](./modules/onboarding/README.md)
- **Contact**: Provides Contact screens. [Read More](./modules/contact/README.md)

### Continuous Integration and Delivery Setup

#### Google Play Internal Deployment

First, create a Google Cloud Services Account and corresponding JSON secrets key in accordance to the [fastlane supply](https://docs.fastlane.tools/actions/supply/) documentation. Store the JSON representation of the key in a `SERVICE_ACCOUNT_JSON_KEY` secret available to the GitHub action.

Follow along
the [Set up your Google APIs console](https://developer.android.com/identity/sign-in/credential-manager-siwg#set-google)
documentation to create a OAuth client ID. Store secrets.xml representation of the key in
a `SECRETS_XML` secret available to the GitHub action.

This is the secrets.xml representation of the key:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <string name="serverClientId" translatable="false">replace-with-actual-id</string>
</resources>
```

In Firebase, the provider Google must also be added in the project in the Authentication menu item
in the Login method tab.

It is recommended to sign your APK before uploading it to the Google Play store. Setup your signing setup as detailed in the [Sign your app](https://developer.android.com/studio/publish/app-signing.html) documentation.

Create a base64 representation of your keystore (`base64 -i ./filetokeystore/keystore.jks`) and save
it in the `KEY_STORE` secret available to the GitHub action. Save the keystore password and key
password in the `KEY_PASSWORD` secret and save the key alias in the `KEY_ALIAS` secret, both
available to the GitHub action.


## Contributing

Contributions to this project are welcome. Please make sure to read the [contribution guide](https://github.com/SchmiedmayerLab/.github/blob/main/CONTRIBUTING.md) and the [Contributor Covenant Code of Conduct](https://github.com/SchmiedmayerLab/.github/blob/main/CODE_OF_CONDUCT.md) first.


## License

This project is licensed under the MIT License. See [Licenses](LICENSES) for more information.


## Our Research

For more information, visit the [Schmiedmayer Lab GitHub organization](https://github.com/SchmiedmayerLab).

![Stanford and Stanford Medicine logos](https://raw.githubusercontent.com/SchmiedmayerLab/.github/main/assets/stanford-footer-light.png#gh-light-mode-only)
![Stanford and Stanford Medicine logos](https://raw.githubusercontent.com/SchmiedmayerLab/.github/main/assets/stanford-footer-dark.png#gh-dark-mode-only)
