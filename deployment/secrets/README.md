<!--

This source file is part of the My Heart Counts Android open-source project

SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)

SPDX-License-Identifier: MIT
-->

# Private Deployment Files

Place the following files in this directory:

- `play-service-account.json`
- `upload-keystore.jks`
- `signing.env`

Git ignores those files. Keep this directory at mode `700` and each private file at mode `600`.
Never paste their contents into chat or commit them.

The canonical key alias is `schmiedmayerlab-myheartcounts-upload`. The `signing.env` file contains
the matching alias and randomly generated password needed to open `upload-keystore.jks`.
