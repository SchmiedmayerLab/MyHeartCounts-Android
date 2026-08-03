<!--

This source file is part of the My Heart Counts open-source project

SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)

SPDX-License-Identifier: MIT
-->

# Google Play Deployment

My Heart Counts uses the same deployment model as ENGAGE-HF:

- A push to `main` uses the `staging` environment and deploys to Google Play internal testing
  after the deployment gate is enabled.
- Publishing a semantic-version GitHub release, such as `1.0.0`, formats the GitHub release body
  and deploys it with the app to production.
- A manual workflow run can target `staging` or `production`.

The permanent Android application ID is `edu.stanford.myheartcounts`.

## Infrastructure

- Google Cloud project: `fastlane-deployment-426021`
- Service account: `fastlane@fastlane-deployment-426021.iam.gserviceaccount.com`
- Google Play Android Developer API: enabled
- Google Play developer account: Stanford University
- Google Play app: My Heart Counts

## Local Secret Inventory

Store private deployment material only in `deployment/secrets/`. Git ignores every file in that
directory except its instructional README.

| Local file | Purpose | GitHub Actions destination |
| --- | --- | --- |
| `play-service-account.json` | Google Play Developer API credential | Environment secret `SERVICE_ACCOUNT_JSON_KEY` |
| `upload-keystore.jks` | App-specific Google Play upload key | Repository secret `KEY_STORE`, base64 encoded |
| `signing.env` | Local `KEY_ALIAS` and `KEY_PASSWORD` values | Repository secrets with the same names |

Public certificates exported from the upload key may be kept in `deployment/certificates/`.
Do not store passwords, private keys, JSON credentials, or base64-encoded private keys there.

### Canonical upload-key identity

- Alias: `schmiedmayerlab-myheartcounts-upload`
- Subject: `CN=My Heart Counts Android Upload Key, OU=SchmiedmayerLab, O=Stanford University, L=Stanford, ST=California, C=US`
- Algorithm: 4096-bit RSA with SHA-256
- Container: PKCS#12

Use `SchmiedmayerLab` and `Stanford University` consistently for organizational identity.
Do not create an additional upload key for this package unless the existing key is lost,
compromised, or Google Play requires a formal upload-key reset.

Restrict local secret permissions:

```shell
chmod 700 deployment/secrets
chmod 600 deployment/secrets/play-service-account.json
chmod 600 deployment/secrets/upload-keystore.jks
chmod 600 deployment/secrets/signing.env
```

Do not paste secret values into issues, pull requests, commits, CI logs, or chat.

## GitHub Configuration

Create the `staging` and `production` environments.

Repository variable:

- `PLAY_DEPLOYMENT_ENABLED=false` is the one-time bootstrap guard. It prevents pushes to `main`
  and manual deployment runs from uploading before Google Play has accepted the first signed AAB.
  Change it to `true` only after the first manual internal release and the read-only credential
  validation have succeeded. Keep it `true` for normal automatic staging and production
  deployments.

Environment variable in both environments:

- `APP_IDENTIFIER=edu.stanford.myheartcounts`

Environment secrets in both environments:

- `KEY_ALIAS`
- `KEY_PASSWORD`
- `KEY_STORE`
- `SERVICE_ACCOUNT_JSON_KEY`

Allow `main` to deploy to `staging`. Allow semantic-version tags matching
`[0-9]*.[0-9]*.[0-9]*` to deploy to `production`.

## Google Play Bootstrap

Fastlane cannot bootstrap a Play app before its first bundle exists. Keep
`PLAY_DEPLOYMENT_ENABLED=false` while completing these steps:

1. Merge the deployment pull request into `main`.
2. Manually run the `Google Play Bootstrap Bundle` GitHub workflow on `main` with version `1.0.0`.
3. Download the `my-heart-counts-1.0.0-bootstrap` artifact from that workflow run.
4. In Play Console, create the first internal-testing release and upload the signed AAB from the
   artifact.
5. Confirm the package name is `edu.stanford.myheartcounts`.
6. Enroll in Play App Signing, using a Google-generated app-signing key.
7. Confirm that the registered upload certificate has the same SHA-256 fingerprint as
   `deployment/certificates/upload-certificate-fingerprints.txt`.
8. Manually run the read-only `Google Play Access Check` GitHub workflow. It must successfully
   query all standard tracks for `edu.stanford.myheartcounts`.
9. If the access check fails, ask a Stanford Play administrator to grant the deployment service
   account access to the My Heart Counts app; no administrator action is needed if it passes.
10. Set `PLAY_DEPLOYMENT_ENABLED=true`.
11. Manually run `Deployment` against `staging` with version `1.0.0` to validate the first
    automated internal-track upload.

For automatic staging deployments, the canonical application version is
`myHeartCounts.versionName` in `gradle.properties`. With no release tag, that declared version is
used unchanged. Once releases exist, staging uses the greater of the declared version and one patch
increment above the latest semantic-version tag. Fastlane independently increments the numeric
Google Play version code for every upload.

The upload key belongs to the project team. Google Play holds the separate app-signing key used
for APKs delivered to users.

The first submission is manual in Play Console, but the bundle does not need to be built on a
developer machine. The bootstrap workflow builds it with the same JDK, Bundler lockfile, signing
identity, and application ID used by later automated deployments.

Pull-request validation builds the same release bundle with a disposable CI-only signing key. This
tests the release build and Fastlane signing path before merge without exposing the real upload key
to pull-request code.

## Store Listing Metadata

Version-controlled Google Play metadata lives in `fastlane/metadata/android/`. After the first
bundle establishes the package in Google Play, Fastlane uploads localized text metadata with each
deployment. The canonical English (United States) title is `My Heart Counts`.

Add reviewed `short_description.txt` and `full_description.txt` files to each locale before using
Fastlane as the source of truth for those fields. Images and screenshots remain explicitly skipped
until their final assets are added to the metadata tree.

Publishing a GitHub release reuses the organization's `format-release-notes.yml` workflow, shared
with My Heart Counts for iOS. Fastlane writes the formatted notes to a temporary, version-specific
changelog file for every locale in the metadata tree and uploads them with the AAB. Google Play
limits release notes to 500 Unicode characters per locale; longer notes are shortened to that limit.
Staging and manually dispatched deployments use `Bug fixes and performance improvements.` unless
the manual workflow run provides different release notes.

## Local Deployment

Create `deployment/secrets/signing.env` without committing it:

```shell
export KEY_ALIAS="replace-with-upload-key-alias"
export KEY_PASSWORD="replace-with-upload-key-password"
```

Load the values locally, then pass the service-account JSON without printing it:

```shell
source deployment/secrets/signing.env
export SERVICE_ACCOUNT_JSON_KEY="$(<deployment/secrets/play-service-account.json)"
bundle exec fastlane deployment \
  environment:internal \
  applicationid:edu.stanford.myheartcounts \
  versionname:1.0.0
unset SERVICE_ACCOUNT_JSON_KEY KEY_ALIAS KEY_PASSWORD
```

The lane:

- reads the highest version code from all standard Play tracks;
- fails closed if the Play API cannot be queried;
- increments the version code;
- builds only the `myheartcounts` application module;
- signs and uploads the AAB and its R8 mapping file;
- uploads version-controlled localized text metadata;
- uploads release notes for every configured metadata locale;
- does not upload images or screenshots until final assets are added and enabled.
