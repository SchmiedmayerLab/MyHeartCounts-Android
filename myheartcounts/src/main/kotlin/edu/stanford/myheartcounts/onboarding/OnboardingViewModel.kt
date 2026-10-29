//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.demographics.DemographicsEditor
import edu.stanford.myheartcounts.demographics.ui.DemographicsScreen
import edu.stanford.myheartcounts.demographics.ui.DemographicsScreenMode
import edu.stanford.myheartcounts.featureflag.FeatureFlag
import edu.stanford.myheartcounts.featureflag.FeatureFlags
import edu.stanford.myheartcounts.firebase.MHCCloudFunctions
import edu.stanford.myheartcounts.firebase.MHCFirebaseRegionInitializer
import edu.stanford.myheartcounts.model.Country
import edu.stanford.myheartcounts.model.StudyVariant
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.notification.NotificationPermissionHandler
import edu.stanford.myheartcounts.standard.consent.MHCConsentHandler
import edu.stanford.myheartcounts.standard.health.HealthAccess
import edu.stanford.myheartcounts.study.StudyEnroller
import edu.stanford.myheartcounts.study.StudyVariantProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.grovealliance.account.AccountLoginScreen
import org.grovealliance.account.AccountService
import org.grovealliance.account.firebase.FirebaseAuthProvider
import org.grovealliance.consent.ConsentFormState
import org.grovealliance.consent.ConsentResponses
import org.grovealliance.consent.ConsentScreen
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.health.HealthConnectAvailability
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ConsumeEvents
import org.grovealliance.ui.DismissStyle
import org.grovealliance.ui.DisplayedEffect
import org.grovealliance.ui.EventSink
import org.grovealliance.ui.EventSourceFlow
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveIconButton
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.GroveToastDisplayStyle
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.PermissionResult
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.horizontalSlideBackward
import org.grovealliance.ui.horizontalSlideForward
import org.grovealliance.ui.mutableScaffoldState
import org.grovealliance.ui.rememberPermissionRequester
import org.grovealliance.ui.showErrorToast
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.validation.ValidationRule
import org.grovealliance.ui.validation.minimalEmail

/**
 * Coordinates the onboarding flow: it owns the [currentStep] state, drives navigation between steps
 * via the [OnboardingStepProvider], and exposes a single [OnboardingScreenContent] for the screen to
 * render. UI interactions arrive as [OnboardingAction]s and one-shot UI requests are emitted as
 * [OnboardingEvent]s; step layouts themselves are produced by the [OnboardingStepLayoutMapper].
 */
// Onboarding is where most of the app's setup happens — eligibility, account, consent, health and
// notification permissions, enrollment — so this coordinates more collaborators than a screen
// normally would. Splitting it by step would spread one linear flow across several types.
@Suppress("TooManyFunctions", "LongParameterList")
class OnboardingViewModel(
    private val navigator: Navigator,
    private val onboardingStepProvider: OnboardingStepProvider,
    private val onboardingStepLayoutMapper: OnboardingStepLayoutMapper,
    private val notificationPermissionHandler: NotificationPermissionHandler,
    private val studyEnroller: StudyEnroller,
    private val firebaseRegionInitializer: MHCFirebaseRegionInitializer,
    private val cloudFunctions: MHCCloudFunctions,
    private val accountService: AccountService,
    private val consentHandler: MHCConsentHandler,
    private val onboardingCompletion: OnboardingCompletion,
    private val healthAccess: HealthAccess,
    private val onboardingProgressTracker: OnboardingProgressTracker,
    private val featureFlags: FeatureFlags,
    private val demographicsEditor: DemographicsEditor,
    private val variantProvider: StudyVariantProvider,
) : ViewModel() {
    private val logger by groveLogger()
    private val actionSource = ActionSource(::onAction)
    private val actionSink = actionSource.sink<OnboardingAction>()
    private val eventSink = EventSink<OnboardingEvent>()
    private val scaffoldState = mutableScaffoldState(appBar = GroveAppBar.Empty)
    private var hadRequestedPermissionsBefore = false
    private val defaultAppBar = groveAppBar {
        back(::previousStep)
    }

    private val answers = MutableStateFlow(OnboardingAnswers.default)
    private val waitlistEmail = MutableStateFlow("")
    private var consentForm: ConsentFormState? = null
    private var isSharingConsent = false
    private val countries by lazy { Country.allCountries() }

    private val currentStep = MutableStateFlow(
        run {
            if (featureFlags.isEnabled(FeatureFlag.SimplifyOnboarding)) {
                // The shortened flow skips the country selection that normally initializes Firebase.
                val country = countries.first { it.code == "US" }
                firebaseRegionInitializer.initialize(country = country)
            }
            stepState(step = onboardingStepProvider.getInitialStep())
        }
    )

    val content = OnboardingScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        actionSink = actionSink,
        events = eventSink.source(),
        currentStep = currentStep.asStateFlow(),
    )

    init {
        viewModelScope.launch {
            onboardingProgressTracker.track(steps = currentStep.map { it.step })
        }
        // The HealthAccess step offers to install Health Connect while it is missing; once the
        // participant returns with it installed, the step offers to continue instead.
        viewModelScope.launch {
            healthAccess.availability.collect { _ ->
                if (currentStep.value.step == OnboardingStep.HealthAccess) {
                    currentStep.update { stepState(step = it.step) }
                }
            }
        }
    }

    private fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.Displayed -> onScreenDisplayed()
            OnboardingAction.Next -> nextStep()
            OnboardingAction.Previous, OnboardingAction.BackPressed -> previousStep()
            OnboardingAction.OpenStudyWebsite -> showWebViewSheet(url = presentedVariant().homepage)
            OnboardingAction.OpenPrivacyPolicy -> showWebViewSheet(url = presentedVariant().privacyPolicy)
            OnboardingAction.Skipped -> {
                logger.i { "User chose to skip ${currentStep.value.step}" }
                nextStep()
            }
            is OnboardingAction.ShowLearnMore -> showLearnMoreSheet(title = action.title, content = action.content)
            is OnboardingAction.PermissionChanged -> onPermissionChanged(action.result)
            is OnboardingAction.OnboardingAnswersChanged -> answers.update { action.answers }
            is OnboardingAction.EligibilityAction -> onEligibilityAction(action)
            is OnboardingAction.StepAction -> onStepAction(action)
        }
    }

    private fun onStepAction(action: OnboardingAction.StepAction) {
        when (action) {
            OnboardingAction.NotificationPermissionRequested -> requestNotificationPermission()
            OnboardingAction.HealthConnectInstallRequested -> installHealthConnect()
            OnboardingAction.ReactivationDeclined -> declineReactivation()
            is OnboardingAction.ConsentFormChanged -> onConsentFormChanged(state = action.state)
        }
    }

    private fun onEligibilityAction(action: OnboardingAction.EligibilityAction) {
        when (action) {
            OnboardingAction.ShowCountrySelectionSheet -> showCountrySelectionSheet()
            is OnboardingAction.CountriesSheetDismissed -> onCountriesSheetDismissed(action.selectedCountry)
            is OnboardingAction.WaitlistEmailChanged -> waitlistEmail.value = action.email
        }
    }

    private fun onPermissionChanged(result: PermissionResult) {
        when (result) {
            is PermissionResult.Granted -> nextStep()
            is PermissionResult.Denied -> when {
                result.shouldShowRationale -> nextStep()
                hadRequestedPermissionsBefore ->
                    navigator.push(NavigationEvent.LaunchAppSettings(action = Settings.ACTION_APP_NOTIFICATION_SETTINGS))
                else -> nextStep()
            }
        }
    }

    private fun installHealthConnect() {
        if (!healthAccess.installHealthConnect()) {
            scaffoldState.showErrorToast(message = StringResource(MHCStrings.onboarding_health_access_install_error))
        }
    }

    /**
     * Asks for Health Connect access and continues whatever the participant granted, as they may
     * share as little as they are comfortable with.
     */
    private suspend fun requestHealthPermissions() {
        if (healthAccess.availability.value != HealthConnectAvailability.Available) return
        val isFullyAuthorized = healthAccess.requestPermissions()
        logger.i { "Health Connect access requested; fully authorized: $isFullyAuthorized" }
    }

    private fun requestNotificationPermission() {
        if (notificationPermissionHandler.isGranted()) {
            nextStep()
        } else {
            hadRequestedPermissionsBefore = notificationPermissionHandler.hasRequestedPermissionBefore()
            notificationPermissionHandler.onPermissionRequested()
            eventSink.push(OnboardingEvent.RequestPermission(permission = notificationPermissionHandler.permission))
        }
    }

    private fun nextStep() {
        viewModelScope.launch {
            handle(result = onboardingStepProvider.getNext(currentStep.value.step, answers.value))
        }
    }

    private fun onConsentFormChanged(state: ConsentFormState) {
        val wasComplete = consentForm?.isComplete
        consentForm = state
        if (wasComplete != state.isComplete) updateConsentAppBar()
    }

    /**
     * The consent step's app bar, offering to share the form once it is complete.
     */
    private fun consentAppBar() = groveAppBar {
        title(MHCStrings.onboarding_consent_title)
        back(::previousStep)
        action(
            icon = GroveIconButton(
                image = ImageResource(image = Icons.Default.Share),
                enabled = consentForm?.isComplete == true && !isSharingConsent,
                onClick = ::shareConsent,
            ),
        )
    }

    private fun updateConsentAppBar() {
        if (currentStep.value.step == OnboardingStep.Consent) scaffoldState.setAppBar(appBar = consentAppBar())
    }

    private fun shareConsent() {
        val responses = consentForm?.responses ?: return
        isSharingConsent = true
        updateConsentAppBar()
        viewModelScope.launch {
            consentHandler.share(responses = responses).onFailure {
                logger.e(it) { "Sharing the consent form failed." }
                scaffoldState.showErrorToast(message = StringResource(MHCStrings.onboarding_consent_share_error))
            }
            isSharingConsent = false
            updateConsentAppBar()
        }
    }

    /**
     * Signs out a previously withdrawn participant who chose not to reactivate their account, and
     * returns to the login step.
     */
    private fun declineReactivation() {
        viewModelScope.launch {
            accountService.logout()
                .onSuccess { handle(result = OnboardingStepResult.Step(step = OnboardingStep.Login)) }
                .onFailure {
                    logger.e(it) { "Signing out after declining reactivation failed." }
                    scaffoldState.showErrorToast(message = StringResource(MHCStrings.onboarding_reactivate_logout_error))
                }
        }
    }

    private fun previousStep() {
        handle(result = onboardingStepProvider.getPrevious(currentStep.value.step))
    }

    private fun onScreenDisplayed() {
        if (currentStep.value.step == OnboardingStep.Notifications && notificationPermissionHandler.isGranted()) {
            nextStep()
        }
    }

    private fun handle(result: OnboardingStepResult) {
        when (result) {
            OnboardingStepResult.Completed -> {
                onboardingCompletion.markComplete()
                navigator.push(event = NavigationEvent.SwitchRoot(root = MHCRoute.Root.Study))
            }
            OnboardingStepResult.Dismissed -> navigator.push(event = NavigationEvent.Pop)
            is OnboardingStepResult.Step -> {
                val step = result.step
                currentStep.update { stepState(step = step) }
                val appBar = when (step) {
                    OnboardingStep.Welcome, OnboardingStep.ReactivateAccount -> GroveAppBar.Empty
                    OnboardingStep.Login -> null
                    OnboardingStep.Eligibility -> groveAppBar {
                        title(MHCStrings.eligibility_title)
                        back(::previousStep)
                    }

                    OnboardingStep.Comprehension -> groveAppBar {
                        title(MHCStrings.consent_survey_title)
                        back(::previousStep)
                    }

                    OnboardingStep.Consent -> consentAppBar()
                    OnboardingStep.Demographics -> groveAppBar {
                        title(MHCStrings.demographics_title)
                        back(::previousStep)
                    }

                    else -> defaultAppBar
                }
                scaffoldState.setAppBar(appBar = appBar)
            }
        }
    }

    private fun showLearnMoreSheet(title: StringResource, content: StringResource) {
        val sheet = LearnMoreSheet(
            appBar = groveAppBar {
                title(title)
                close { scaffoldState.dismissBottomSheet() }
            },
            description = content,
        )
        scaffoldState.showBottomSheet(sheet = sheet)
    }

    private fun showWebViewSheet(url: String) {
        val sheet = WebViewSheet(
            appBar = groveAppBar {
                close { scaffoldState.dismissBottomSheet() }
            },
            url = url,
        )
        scaffoldState.showBottomSheet(sheet = sheet)
    }

    /**
     * Uploads the consent form the participant just signed.
     *
     * A failure is logged and onboarding continues: the participant has consented either way, and
     * blocking them here would be worse than a form the study has to chase up. The account fields
     * the uploader stamps are what say whether it arrived.
     */
    private suspend fun uploadConsent() {
        val responses = answers.value.consentResponses ?: return
        consentHandler.upload(responses = responses)
            .onFailure { logger.e(it) { "Consent upload failed; continuing onboarding." } }
    }

    private suspend fun joinWaitlist() {
        val validation = ValidationRule.minimalEmail.validate(waitlistEmail.value)
        if (validation != null) {
            return scaffoldState.showErrorToast(message = validation.message)
        }
        val country = answers.value.country ?: return scaffoldState.showErrorToast(
            message = StringResource(MHCStrings.onboarding_waitlist_error),
        )

        // The waitlist call requires a signed-in participant, and someone in a country the study has
        // not launched in has no account. Signing up anonymously is what gets them past the rules;
        // on failure the anonymous account is discarded again rather than left behind.
        val joined = runCatchingAsync {
            accountService.signIn(FirebaseAuthProvider.Anonymous).getOrThrow()
            cloudFunctions.joinWaitlist(regionCode = country.code, email = waitlistEmail.value)
                .onFailure { accountService.logout() }
                .getOrThrow()
        }
        joined.exceptionOrNull()?.let { failure ->

            logger.e(failure) { "Failed to join the launch waitlist" }
            return scaffoldState.showErrorToast(
                message = StringResource(MHCStrings.onboarding_waitlist_error),
            )
        }

        scaffoldState.showToast(
            imageResource = ImageResource(Icons.Outlined.Email),
            message = StringResource(MHCStrings.onboarding_waitlist_success),
            displayStyle = GroveToastDisplayStyle.DefaultLong,
        )
        logger.i { "The participant joined the launch waitlist for '${country.code}'" }
    }

    /**
     * The variant whose web pages the current step links to: the one of the participant's country
     * while it waits for a launch there, the selected variant otherwise.
     */
    private fun presentedVariant(): StudyVariant {
        val country = answers.value.country
        val waitingVariant = StudyVariant.entries.firstOrNull { it.regionCode == country?.code }
        return if (currentStep.value.step == OnboardingStep.CountryUnavailable && waitingVariant != null) {
            waitingVariant
        } else {
            variantProvider.currentVariant
        }
    }

    private fun showCountrySelectionSheet() {
        val sheet = onboardingStepLayoutMapper.countrySelectionSheet(stepInput(currentStep.value.step))
        scaffoldState.showBottomSheet(sheet = sheet)
    }

    private fun onCountriesSheetDismissed(country: Country?) {
        answers.update { it.copy(country = country ?: it.country) }
        scaffoldState.dismissBottomSheet()
    }

    private fun stepState(step: OnboardingStep): OnboardingStepState {
        return OnboardingStepState(
            step = step,
            content = onboardingStepLayoutMapper.map(stepInput(step)),
        )
    }

    private fun stepInput(step: OnboardingStep) = OnboardingStepInput(
        step = step,
        scope = viewModelScope,
        actionSink = actionSink,
        advance = ::advance,
        answers = answers.asStateFlow(),
        waitlistEmail = waitlistEmail.asStateFlow(),
        countries = countries,
        healthConnectAvailability = healthAccess.availability.value,
        isDemographicsComplete = demographicsEditor.isComplete,
    )

    private suspend fun advance(step: OnboardingStep) {
        when (step) {
            OnboardingStep.Consent -> uploadConsent()
            OnboardingStep.HealthAccess -> requestHealthPermissions()
            OnboardingStep.Demographics -> demographicsEditor.flush()
                .onFailure {
                    logger.e(it) { "Storing the demographics failed; staying on the step." }
                    return
                }
            OnboardingStep.FinalEnrollment -> studyEnroller.enroll()
                .onFailure { logger.e(it) { "Enrollment failed; continuing into the app." } }
            OnboardingStep.ReactivateAccount -> cloudFunctions.markAccountForStudyReenrollment()
                .onFailure {
                    logger.e(it) { "Reactivating the account failed." }
                    scaffoldState.showErrorToast(message = StringResource(MHCStrings.onboarding_reactivate_error))
                    return
                }
            else -> Unit
        }
        when (step) {
            OnboardingStep.CountryUnavailable -> joinWaitlist()
            else -> {
                val result = onboardingStepProvider.getNext(step, answers.value)
                initializeFirebaseIfNeeded(result)
                handle(result = result)
            }
        }
    }

    /**
     * Initializes Firebase before entering a step that needs it.
     *
     * The participant's country decides which Firebase project the app talks to, so leaving
     * eligibility is the earliest point at which Firebase can be initialized. Participants who turn
     * out to be ineligible for reasons other than their country never reach a step that talks to a
     * backend, so no project is contacted for them at all: Firebase is initialized only on the
     * eligible and country-unavailable paths.
     *
     * Eligible participants take part in the variant of their country.
     */
    private fun initializeFirebaseIfNeeded(result: OnboardingStepResult) {
        val next = (result as? OnboardingStepResult.Step)?.step ?: return
        if (next != OnboardingStep.StudyOverview && next != OnboardingStep.CountryUnavailable) return
        val country = answers.value.country ?: return
        if (next == OnboardingStep.StudyOverview) {
            val variant = StudyVariant.entries.firstOrNull { it.regionCode == country.code } ?: StudyVariant.Stanford
            variantProvider.select(variant = variant)
        }
        if (!firebaseRegionInitializer.initialize(country)) {
            logger.e { "Failed to initialize Firebase for '${country.code}'." }
        }
    }
}

/**
 * The root content of the onboarding screen: hosts the scaffold, animates between steps as
 * [currentStep] changes, forwards back presses and display events through [actionSink], and consumes
 * [events] to fulfil one-shot requests such as system permission prompts.
 */
data class OnboardingScreenContent(
    val scaffoldState: GroveScaffoldState,
    val actionSink: ActionSink<OnboardingAction>,
    val events: EventSourceFlow<OnboardingEvent>,
    val currentStep: StateFlow<OnboardingStepState>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        GroveScaffold(state = scaffoldState) {
            val step by currentStep.collectAsStateWithLifecycle()
            BackHandler(enabled = step.step != OnboardingStep.Welcome) {
                actionSink.push(OnboardingAction.BackPressed)
            }
            DisplayedEffect {
                actionSink.push(OnboardingAction.Displayed)
            }
            val permissionRequester = rememberPermissionRequester()

            ConsumeEvents(events) {
                when (it) {
                    is OnboardingEvent.RequestPermission -> permissionRequester.request(
                        permission = it.permission,
                        onResult = { result -> actionSink.push(OnboardingAction.PermissionChanged(result)) },
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState.step.ordinal > initialState.step.ordinal) {
                        horizontalSlideForward
                    } else {
                        horizontalSlideBackward
                    }
                },
                label = "onboardingStep",
            ) { state ->
                state.content.Content()
            }
        }
    }
}

/**
 * The currently displayed [step] paired with its rendered [content].
 */
data class OnboardingStepState(
    val step: OnboardingStep,
    val content: ComposableContent,
)

/**
 * A user interaction or lifecycle signal raised by the onboarding UI for the [OnboardingViewModel] to
 * act on. The UI only dispatches these; all decisions about navigation and presentation live in the
 * view model.
 */
sealed interface OnboardingAction {
    data object Displayed : OnboardingAction
    data object Next : OnboardingAction
    data object Previous : OnboardingAction
    data object BackPressed : OnboardingAction
    data object Skipped : OnboardingAction
    data object OpenStudyWebsite : OnboardingAction
    data object OpenPrivacyPolicy : OnboardingAction
    data class ShowLearnMore(val title: StringResource, val content: StringResource) : OnboardingAction
    data class PermissionChanged(val result: PermissionResult) : OnboardingAction
    data class OnboardingAnswersChanged(val answers: OnboardingAnswers) : OnboardingAction

    /**
     * An intent raised by the eligibility step's country selection and waitlist.
     */
    sealed interface EligibilityAction : OnboardingAction

    data object ShowCountrySelectionSheet : EligibilityAction
    data class CountriesSheetDismissed(val selectedCountry: Country?) : EligibilityAction
    data class WaitlistEmailChanged(val email: String) : EligibilityAction

    /**
     * An intent raised by a single step's own controls.
     */
    sealed interface StepAction : OnboardingAction

    data object NotificationPermissionRequested : StepAction
    data object HealthConnectInstallRequested : StepAction
    data object ReactivationDeclined : StepAction
    data class ConsentFormChanged(val state: ConsentFormState) : StepAction
}

/**
 * The login step content: the account login screen with [footer] below its form. [onSuccess]
 * advances the flow once the user is signed in; [onDismiss] returns to the previous step.
 */
data class LoginStep(
    val onSuccess: () -> Unit,
    val onDismiss: () -> Unit,
    val footer: ComposableContent,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        AccountLoginScreen(
            dismissStyle = DismissStyle.NONE,
            footer = footer,
            onSuccess = onSuccess,
            onDismiss = onDismiss,
        )
    }
}

/**
 * The consent step content. [onConsent] receives the collected [ConsentResponses] once the document
 * is fully signed; [onFormChanged] receives the form's state whenever a response or its completion
 * changes.
 */
data class ConsentStep(
    val onConsent: suspend (ConsentResponses) -> Unit,
    val onFormChanged: (ConsentFormState) -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        ConsentScreen(
            onConsent = onConsent,
            onFormChanged = onFormChanged,
        )
    }
}

/**
 * The demographics step content: the demographics questions above [continueButton], which is enabled
 * once every question needing an answer has one.
 */
data class DemographicsStep(
    val continueButton: StateFlow<AsyncTextButton>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        val continueButton by continueButton.collectAsStateWithLifecycle()

        Column(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                DemographicsScreen(mode = DemographicsScreenMode.Embedded)
            }
            continueButton.Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}

/**
 * The data and callbacks a step layout needs in order to render: the current [step] and collected
 * [answers], the coroutine [scope] for suspending actions, the available [countries] and pending
 * [waitlistEmail], whether Health Connect can be used ([healthConnectAvailability]), whether every
 * demographics question needing an answer has one ([isDemographicsComplete]), plus [advance] to move
 * forward and [push] to dispatch an [OnboardingAction].
 */
data class OnboardingStepInput(
    val step: OnboardingStep,
    val scope: CoroutineScope,
    val advance: suspend (OnboardingStep) -> Unit,
    val answers: StateFlow<OnboardingAnswers>,
    val waitlistEmail: StateFlow<String>,
    val countries: List<Country>,
    val healthConnectAvailability: HealthConnectAvailability,
    val isDemographicsComplete: StateFlow<Boolean>,
    private val actionSink: ActionSink<OnboardingAction>,
) {

    fun push(action: OnboardingAction) {
        actionSink.push(action)
    }
}

/**
 * A one-shot request from the [OnboardingViewModel] to the onboarding UI that the UI must fulfil,
 * such as launching a system dialog.
 */
sealed interface OnboardingEvent {

    /**
     * Asks the UI to launch the system permission dialog for [permission] and report the result back.
     */
    data class RequestPermission(val permission: String) : OnboardingEvent
}
