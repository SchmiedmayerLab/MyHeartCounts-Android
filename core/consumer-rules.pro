#
# This source file is part of the My Heart Counts Android open-source project
#
# SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
#
# SPDX-License-Identifier: MIT
#

# DependenciesGraph.createDependencyOrThrow instantiates types that were never registered explicitly
# by reaching their DefaultInitializer companion through kotlin-reflect
# (KClass.companionObjectInstance). Nothing in the bytecode references those companions, so R8
# otherwise removes the companion object, the synthetic Companion field that exposes it, and the
# implementation class create() would have returned -- leaving the fallback to fail with
# "No suitable constructor found".
#
# allowobfuscation matters: kotlin-reflect finds the companion by walking from the owner's rewritten
# Kotlin metadata to a nested class. Pinning the companion's name while R8 renames its outer class
# breaks that nesting, so let R8 rename both together and only forbid their removal.
-keep,allowobfuscation interface org.grovealliance.core.DefaultInitializer
-keep,allowobfuscation class * implements org.grovealliance.core.DefaultInitializer { *; }
-keepclassmembers class * {
    public static ** Companion;
}

# The same fallback also auto-instantiates an unregistered type through its no-arg or Context
# constructor. Those constructors are likewise never called from bytecode, so R8 strips them -- it
# removed Concurrency.<init>() outright -- and the fallback then reports "No suitable constructor
# found" for a module that is only ever obtained by resolving it from the graph.
-keepclassmembers class * implements org.grovealliance.core.Module {
    public <init>();
    public <init>(android.content.Context);
}
