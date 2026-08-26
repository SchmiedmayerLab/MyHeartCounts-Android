#
# This source file is part of the My Heart Counts Android open-source project
#
# SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
#
# SPDX-License-Identifier: MIT
#

# zstd-jni's native code reaches back into its Java classes with GetFieldID/GetMethodID, looking the
# members up by their source names. R8 keeps the classes carrying native methods but still renames
# their fields (ZstdInputStreamNoFinalizer.stream became "d"), so the lookups return null and the
# library aborts the process with SIGABRT the first time the study bundle is decompressed.
-keep class com.github.luben.zstd.** { *; }
