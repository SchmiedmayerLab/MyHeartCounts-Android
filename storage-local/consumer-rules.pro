#
# This source file is part of the My Heart Counts Android open-source project
#
# SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
#
# SPDX-License-Identifier: MIT
#

# androidx.security:security-crypto-ktx bundles Google Tink, which is compiled against Error Prone's
# annotations but does not depend on them at runtime. They are compile-only, so they are absent from
# the runtime classpath and R8 fails the build on the dangling references once the encrypted storage
# path becomes reachable.
-dontwarn com.google.errorprone.annotations.**
