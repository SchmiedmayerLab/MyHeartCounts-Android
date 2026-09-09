#
# This source file is part of the My Heart Counts Android open-source project
#
# SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
#
# SPDX-License-Identifier: MIT
#

# typeReference<T>() captures its generic argument from the signature of the anonymous
# TypeReferenceImpl subclass it creates. Those subclasses are structurally identical, so R8 merges
# them into their superclass and every TypeReference then reports the same type -- which collapses
# every DependenciesGraph key onto one another. TypeReferenceImpl cross-checks the captured
# signature against the erasure so non-generic keys stay correct regardless, but distinguishing
# List<String> from List<Int> needs the subclasses to survive as distinct classes.
-keep,allowobfuscation,allowshrinking class org.grovealliance.foundation.TypeReferenceImpl
-keep,allowobfuscation,allowshrinking class * extends org.grovealliance.foundation.TypeReferenceImpl
