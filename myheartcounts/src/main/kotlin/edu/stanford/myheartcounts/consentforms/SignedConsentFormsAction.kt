//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

/**
 * An intent raised by the signed consent forms screen.
 */
sealed interface SignedConsentFormsAction {

    /**
     * The participant left the screen.
     */
    data object Close : SignedConsentFormsAction

    /**
     * The participant went back from an open form to the list of forms.
     */
    data object Back : SignedConsentFormsAction

    /**
     * The participant asked to read the forms again after they couldn't be read.
     */
    data object Retry : SignedConsentFormsAction

    /**
     * The participant pulled the list to read the forms again.
     */
    data object Refresh : SignedConsentFormsAction

    /**
     * The participant opened the form identified by [id].
     */
    data class FormSelected(val id: String) : SignedConsentFormsAction

    /**
     * The participant asked to share the open form.
     */
    data object Share : SignedConsentFormsAction
}
