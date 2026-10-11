//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

/**
 * Builds the account overview.
 */
interface MHCAccountOverviewMapper {

    /**
     * Maps [input] into the overview to render. Its sections observe the flows in [input] and every
     * interaction is reported through its action sink, so the result stays valid as the state changes.
     */
    fun map(input: MHCAccountOverviewInput): MHCAccountOverview
}

/**
 * Default [MHCAccountOverviewMapper] implementation.
 */
class MHCAccountOverviewMapperImpl : MHCAccountOverviewMapper {

    override fun map(input: MHCAccountOverviewInput) = MHCAccountOverview(
        sections = MHCAccountOverviewSections(
            items = listOf(
                withdrawFromStudy(input = input),
            ),
        ),
        onDismiss = { input.actionSink.push(MHCAccountOverviewAction.Dismissed) },
    )

    private fun withdrawFromStudy(input: MHCAccountOverviewInput) = WithdrawFromStudySection(
        state = input.withdrawalState,
        onWithdrawSelected = { input.actionSink.push(MHCAccountOverviewAction.WithdrawSelected) },
        onWithdrawConfirmed = { input.actionSink.push(MHCAccountOverviewAction.WithdrawConfirmed) },
        onDismissed = { input.actionSink.push(MHCAccountOverviewAction.WithdrawDialogDismissed) },
    )
}
