//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import edu.stanford.myheartcounts.consentforms.ui.SignedConsentFormsLayout
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.LoadingLayout

/**
 * The loading, failed or ready state of the signed consent forms screen.
 */
sealed interface SignedConsentFormsScreenState {

    /**
     * The content shown for this state.
     */
    val layout: ComposableContent

    /**
     * The forms are being read.
     */
    data object Loading : SignedConsentFormsScreenState {
        override val layout = LoadingLayout()
    }

    /**
     * The forms couldn't be read; [layout] offers to try again.
     */
    data class Error(override val layout: GroveErrorLayout) : SignedConsentFormsScreenState

    /**
     * The forms were read and are listed by [layout].
     */
    data class Content(override val layout: SignedConsentFormsLayout) : SignedConsentFormsScreenState
}
