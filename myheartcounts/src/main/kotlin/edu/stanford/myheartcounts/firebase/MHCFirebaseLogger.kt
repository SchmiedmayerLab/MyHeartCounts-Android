//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import org.grovealliance.core.logging.groupLogger

/**
 * Logger shared by the components that talk to Firebase. Every message is logged under the
 * `MHCFirebase` tag, prefixed with the name of the component that logs it.
 */
fun mhcFirebaseLogger() = groupLogger(tag = "MHCFirebase")
