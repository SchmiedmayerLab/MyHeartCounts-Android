#!/bin/bash

#
# This source file is part of the My Heart Counts open-source project
#
# SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
#
# SPDX-License-Identifier: MIT

echo "Running detekt..."
./gradlew detekt

detektStatus=$?
if [[ "$detektStatus" = 0 ]] ; then
    echo "Detekt run successfully"
    exit 0
else
    echo 1>&2 "Detekt found violations"
    exit 1
fi