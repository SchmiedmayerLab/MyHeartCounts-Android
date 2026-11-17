//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import edu.stanford.myheartcounts.BuildInfo
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.promptedactions.PromptedActionsMapper
import edu.stanford.myheartcounts.promptedactions.PromptedActionsMode
import edu.stanford.myheartcounts.standard.MHCAppRevision
import edu.stanford.myheartcounts.study.EnrolledStudy
import edu.stanford.myheartcounts.ui.DynamicSection
import edu.stanford.myheartcounts.ui.ExternalLink
import edu.stanford.myheartcounts.ui.ToggleRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.GroveAlertDialog
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.ItemDisplayRow
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.account.CustomAccountSections
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSection
import org.grovealliance.ui.scheduler.PromptedActionsCard
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.Spacings
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Builds the account overview.
 */
interface MHCAccountOverviewMapper {

    /**
     * Maps [input] into the overview to render. Its sections observe the flows in [input] and every
     * interaction is reported through its action sink, so the result stays valid as the state changes.
     */
    fun map(input: MHCAccountOverviewInput): MHCAccountOverview

    /**
     * Maps the feedback [message] into the sheet for writing it, beneath [appBar]; edits are reported
     * through [onMessageChanged], and the sheet can be swiped away only while [isDismissible].
     */
    fun mapFeedback(
        appBar: StateFlow<GroveAppBar>,
        message: StateFlow<String>,
        onMessageChanged: (String) -> Unit,
        isDismissible: () -> Boolean,
    ): FeedbackSheet

    /**
     * The feedback sheet's app bar, closing through [onClose] and offering [onSend] while [canSend].
     */
    fun mapFeedbackAppBar(
        canSend: Boolean,
        onClose: () -> Unit,
        onSend: suspend () -> Unit,
    ): GroveAppBar
}

/**
 * Default [MHCAccountOverviewMapper] implementation.
 */
class MHCAccountOverviewMapperImpl(
    private val appRevision: MHCAppRevision,
    private val timeProvider: TimeProvider,
    private val buildInfo: BuildInfo,
    private val promptedActionsMapper: PromptedActionsMapper,
) : MHCAccountOverviewMapper {

    override fun map(input: MHCAccountOverviewInput) = MHCAccountOverview(
        customSections = CustomAccountSections(
            sections = listOf(
                promptedActions(input = input),
                studyParticipation(input = input),
                support(input = input),
                appInfo(input = input),
                withdrawFromStudy(input = input),
            ),
        ),
        scaffoldState = input.scaffoldState,
        onDismiss = { input.actionSink.push(MHCAccountOverviewAction.Dismissed) },
    )

    override fun mapFeedback(
        appBar: StateFlow<GroveAppBar>,
        message: StateFlow<String>,
        onMessageChanged: (String) -> Unit,
        isDismissible: () -> Boolean,
    ) = FeedbackSheet(
        appBar = appBar,
        message = message,
        onMessageChanged = onMessageChanged,
        isDismissible = isDismissible,
    )

    override fun mapFeedbackAppBar(
        canSend: Boolean,
        onClose: () -> Unit,
        onSend: suspend () -> Unit,
    ) = groveAppBar {
        title(MHCStrings.account_feedback_title)
        close { onClose() }
        action(
            content = AsyncTextButton(
                title = StringResource(MHCStrings.account_feedback_send),
                enabled = canSend,
                containerColor = { Colors.transparent },
                textColor = { if (canSend) Colors.primary else Colors.onSurfaceVariant },
                action = onSend,
            ),
        )
    }

    private fun promptedActions(input: MHCAccountOverviewInput) = DynamicSection(
        section = input.promptedActions.map { actions ->
            promptedActionsSection(
                card = promptedActionsMapper.mapCard(
                    actions = actions,
                    mode = PromptedActionsMode.All,
                    onClick = { input.actionSink.push(MHCAccountOverviewAction.PromptedActionsSelected) },
                ),
            )
        }.stateIn(
            scope = input.scope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = promptedActionsSection(card = null),
        ),
    )

    private fun promptedActionsSection(card: PromptedActionsCard?) = groveSection(key = PROMPTED_ACTIONS_KEY) {
        card?.let { content(key = PROMPTED_ACTIONS_KEY, content = it) }
    }

    private fun studyParticipation(input: MHCAccountOverviewInput) = DynamicSection(
        section = input.enrolledStudy
            .map { enrolledStudy ->
                studyParticipationSection(
                    enrolledStudy = enrolledStudy(enrolledStudy = enrolledStudy, input = input),
                    input = input,
                )
            }
            .stateIn(
                scope = input.scope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = studyParticipationSection(
                    enrolledStudy = null,
                    input = input,
                ),
            ),
    )

    private fun studyParticipationSection(
        enrolledStudy: CardRow<EnrolledStudyRow>?,
        input: MHCAccountOverviewInput,
    ) = groveSection(key = STUDY_PARTICIPATION_KEY) {
        card(key = STUDY_PARTICIPATION_KEY) {
            enrolledStudy?.let { row(row = it) }
            row(
                row = navigationRow(
                    key = "participation-stats",
                    icon = Icons.Default.MilitaryTech,
                    label = MHCStrings.account_view_participation_stats,
                    onClick = { input.actionSink.push(MHCAccountOverviewAction.ParticipationStatsSelected) },
                ),
            )
            row(row = postTrialNudges(input = input))
            row(
                row = navigationRow(
                    key = "demographics",
                    icon = Icons.Outlined.Person,
                    label = MHCStrings.demographics_title,
                    onClick = { input.actionSink.push(MHCAccountOverviewAction.DemographicsSelected) },
                ),
            )
            row(
                row = navigationRow(
                    key = "comorbidities",
                    icon = Icons.Outlined.MedicalInformation,
                    label = MHCStrings.account_update_comorbidities,
                    onClick = { input.actionSink.push(MHCAccountOverviewAction.UpdateComorbiditiesSelected) },
                ),
            )
            row(
                row = navigationRow(
                    key = "signed-consent-forms",
                    icon = Icons.Outlined.Description,
                    label = MHCStrings.account_review_consent_forms,
                    onClick = { input.actionSink.push(MHCAccountOverviewAction.SignedConsentFormsSelected) },
                ),
            )
            row(
                row = navigationRow(
                    key = "workout-preference",
                    icon = Icons.Default.FitnessCenter,
                    label = MHCStrings.account_workout_preference,
                    onClick = { input.actionSink.push(MHCAccountOverviewAction.WorkoutPreferenceSelected) },
                ),
            )
        }
    }

    private fun enrolledStudy(
        enrolledStudy: EnrolledStudy?,
        input: MHCAccountOverviewInput,
    ): CardRow<EnrolledStudyRow>? {
        val title = enrolledStudy?.title ?: return null
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(timeProvider.currentZone())
        return CardRow(
            key = "enrolled-study",
            content = EnrolledStudyRow(
                title = title,
                shortExplanation = enrolledStudy.shortExplanation,
                enrolledSince = StringResource(MHCStrings.account_enrolled_since, formatter.format(enrolledStudy.enrollmentDate)),
                website = ExternalLink(
                    text = StringResource(MHCStrings.account_study_website),
                    onClicked = { input.actionSink.push(MHCAccountOverviewAction.StudyHomepageSelected) },
                ),
            ),
        )
    }

    private fun postTrialNudges(input: MHCAccountOverviewInput) = CardRow(
        key = "post-trial-nudges",
        content = ToggleRow(
            title = StringResource(MHCStrings.account_post_trial_nudges_title),
            subtitle = StringResource(MHCStrings.account_post_trial_nudges_subtitle),
            isOn = input.postTrialNudgesOptIn,
            onToggle = { input.actionSink.push(MHCAccountOverviewAction.PostTrialNudgesChanged(isOptedIn = it)) },
        ),
    )

    private fun support(input: MHCAccountOverviewInput) = groveSection(key = SUPPORT_KEY) {
        card(key = SUPPORT_KEY) {
            if (buildInfo.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                row(
                    row = navigationRow(
                        key = "change-language",
                        icon = Icons.Default.Language,
                        label = MHCStrings.account_change_language,
                        onClick = { input.actionSink.push(MHCAccountOverviewAction.ChangeLanguageSelected) },
                    ),
                )
            }
            row(
                row = navigationRow(
                    key = "send-feedback",
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = MHCStrings.account_send_feedback,
                    onClick = { input.actionSink.push(MHCAccountOverviewAction.SendFeedbackSelected) },
                ),
            )
        }
    }

    private fun appInfo(input: MHCAccountOverviewInput) = groveSection(key = APP_INFO_KEY) {
        card(key = APP_INFO_KEY) {
            row(
                key = "version",
                content = ItemDisplayRow(
                    leadingImage = ImageResource(image = Icons.Default.Info),
                    label = StringResource(MHCStrings.app_name),
                    value = StringResource(appRevision.version),
                ),
            )
            row(
                key = "privacy-policy",
                content = ExternalLink(
                    text = StringResource(MHCStrings.account_privacy_policy),
                    icon = ImageResource(image = Icons.Default.Shield),
                    padding = { Spacings.medium },
                    onClicked = { input.actionSink.push(MHCAccountOverviewAction.PrivacyPolicySelected) },
                ),
            )
            row(
                row = navigationRow(
                    key = "license-information",
                    icon = Icons.Default.AccountBalance,
                    label = MHCStrings.license_information_title,
                    onClick = { input.actionSink.push(MHCAccountOverviewAction.LicenseInformationSelected) },
                ),
            )
        }
    }

    private fun navigationRow(
        key: String,
        icon: ImageVector,
        @StringRes label: Int,
        onClick: () -> Unit,
    ) = CardRow(
        key = key,
        content = ItemDisplayRow(
            leadingImage = ImageResource(image = icon),
            label = StringResource(label),
            value = null,
            showArrow = true,
            onClick = onClick,
        ),
    )

    private fun withdrawFromStudy(input: MHCAccountOverviewInput) = groveSection(key = WITHDRAW_FROM_STUDY_KEY) {
        content(
            key = WITHDRAW_FROM_STUDY_KEY,
            content = withdrawFromStudyButton(input = input),
        )
    }

    private fun withdrawFromStudyButton(input: MHCAccountOverviewInput) = WithdrawFromStudyButton(
        state = input.withdrawalState,
        onWithdrawSelected = { input.actionSink.push(MHCAccountOverviewAction.WithdrawSelected) },
        confirmation = GroveAlertDialog(
            title = StringResource(MHCStrings.account_withdraw_confirm_title),
            message = StringResource(MHCStrings.account_withdraw_confirm_message),
            confirmButton = GroveAlertDialog.Button(
                title = StringResource(MHCStrings.account_withdraw_confirm_action),
                style = GroveAlertDialog.Button.Style.Destructive,
                action = { input.actionSink.push(MHCAccountOverviewAction.WithdrawConfirmed) },
            ),
            dismissButton = GroveAlertDialog.Button(
                title = StringResource(MHCStrings.account_withdraw_cancel),
                action = {},
            ),
            onDismiss = { input.actionSink.push(MHCAccountOverviewAction.WithdrawDialogDismissed) },
        ),
        failure = GroveAlertDialog(
            title = StringResource(MHCStrings.account_withdraw_failed_title),
            message = StringResource(MHCStrings.account_withdraw_failed_message),
            confirmButton = GroveAlertDialog.Button(
                title = StringResource(MHCStrings.account_withdraw_failed_dismiss),
                action = {},
            ),
            onDismiss = { input.actionSink.push(MHCAccountOverviewAction.WithdrawDialogDismissed) },
        ),
    )

    private companion object {
        const val PROMPTED_ACTIONS_KEY = "prompted-actions"
        const val STUDY_PARTICIPATION_KEY = "study-participation"
        const val SUPPORT_KEY = "support"
        const val APP_INFO_KEY = "app-info"
        const val WITHDRAW_FROM_STUDY_KEY = "withdraw-from-study"
    }
}
