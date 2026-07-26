//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.sample.app.health

import androidx.fragment.app.FragmentActivity

data class PermissionAction(val onClick: (FragmentActivity) -> Unit)
