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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.firebase.MHCCloudFunctions
import edu.stanford.myheartcounts.firebase.MHCFirebaseRegionInitializer
import edu.stanford.myheartcounts.model.Country
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.notification.NotificationPermissionHandler
import edu.stanford.myheartcounts.standard.consent.MHCConsentDocumentProvider
import edu.stanford.myheartcounts.standard.consent.MHCConsentUploader
import edu.stanford.myheartcounts.study.StudyEnroller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.grovealliance.account.AccountLoginScreen
import org.grovealliance.account.AccountService
import org.grovealliance.account.firebase.FirebaseAuthProvider
import org.grovealliance.consent.ConsentResponses
import org.grovealliance.consent.ConsentScreen
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.resources.Strings
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ConsumeEvents
import org.grovealliance.ui.DismissStyle
import org.grovealliance.ui.DisplayedEffect
import org.grovealliance.ui.EventSink
import org.grovealliance.ui.EventSourceFlow
import org.grovealliance.ui.GroveAppBar
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
import org.grovealliance.ui.validation.ValidationRule
import org.grovealliance.ui.validation.minimalEmail
import kotlin.time.Duration.Companion.seconds

private const val STUDY_WEBSITE_URL = "https://myheartcounts.stanford.edu"

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
    private val consentUploader: MHCConsentUploader,
    private val consentDocumentProvider: MHCConsentDocumentProvider,
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
    private val countries by lazy { Country.allCountries() }

    private val currentStep = MutableStateFlow(stepState(step = onboardingStepProvider.getInitialStep()))

    val content = OnboardingScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        actionSink = actionSink,
        events = eventSink.source(),
        currentStep = currentStep.asStateFlow(),
    )

    private fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.Displayed -> onScreenDisplayed()
            OnboardingAction.Next -> nextStep()
            OnboardingAction.Previous, OnboardingAction.BackPressed -> previousStep()
            OnboardingAction.ShowCountrySelectionSheet -> showCountrySelectionSheet()
            is OnboardingAction.CountriesSheetDismissed -> onCountriesSheetDismissed(action.selectedCountry)
            OnboardingAction.OpenStudyWebsite -> showWebViewSheet(url = STUDY_WEBSITE_URL)
            is OnboardingAction.WaitlistEmailChanged -> waitlistEmail.value = action.email
            is OnboardingAction.OnboardingAnswersChanged -> answers.update { action.answers }
            OnboardingAction.NotificationPermissionRequested -> requestNotificationPermission()
            OnboardingAction.NotificationSkipped -> {
                logger.i { "User chose to skip notification permission" }
                nextStep()
            }
            is OnboardingAction.ShowLearnMore -> showLearnMoreSheet(title = action.title, content = action.content)
            is OnboardingAction.PermissionChanged -> onPermissionChanged(action.result)
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
        handle(result = onboardingStepProvider.getNext(currentStep.value.step, answers.value))
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
            OnboardingStepResult.Completed -> navigator.push(event = NavigationEvent.SwitchRoot(root = MHCRoute.Root.Study))
            OnboardingStepResult.Dismissed -> navigator.push(event = NavigationEvent.Pop)
            is OnboardingStepResult.Step -> {
                val step = result.step
                currentStep.update { stepState(step = step) }
                val appBar = when (step) {
                    OnboardingStep.Welcome -> GroveAppBar.Empty
                    OnboardingStep.Login -> null
                    OnboardingStep.Eligibility -> groveAppBar {
                        title(MHCStrings.eligibility_title)
                        back(::previousStep)
                    }

                    OnboardingStep.Comprehension -> groveAppBar {
                        title(MHCStrings.consent_survey_title)
                        back(::previousStep)
                    }

                    OnboardingStep.Consent -> groveAppBar {
                        title(MHCStrings.onboarding_consent_title)
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

    @Suppress("SameParameterValue")
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
        consentUploader.upload(
            document = consentDocumentProvider.document(),
            responses = responses,
        ).onFailure { logger.e(it) { "Consent upload failed; continuing onboarding." } }
    }

    private suspend fun joinWaitlist() {
        val validation = ValidationRule.minimalEmail.validate(waitlistEmail.value)
        if (validation != null) {
            return scaffoldState.showErrorToast(message = validation.message)
        }
        val country = answers.value.country ?: return scaffoldState.showErrorToast(
            message = StringResource(Strings.onboarding_waitlist_error),
        )

        // The waitlist call requires a signed-in participant, and someone in a country the study has
        // not launched in has no account. Signing up anonymously is what gets them past the rules,
        // matching iOS; on failure the anonymous account is discarded again rather than left behind.
        val joined = accountService.signIn(FirebaseAuthProvider.Anonymous).mapCatching {
            cloudFunctions.joinWaitlist(regionCode = country.code, email = waitlistEmail.value)
                .onFailure { accountService.logout() }
                .getOrThrow()
        }
        if (joined.isFailure) {
            logger.e(joined.exceptionOrNull()) { "Failed to join the launch waitlist" }
            return scaffoldState.showErrorToast(
                message = StringResource(Strings.onboarding_waitlist_error),
            )
        }

        scaffoldState.showToast(
            imageResource = ImageResource(Icons.Outlined.Email),
            message = StringResource(Strings.onboarding_waitlist_success),
            displayStyle = GroveToastDisplayStyle.DefaultLong,
        )
        logger.i { "The participant joined the launch waitlist for '${country.code}'" }
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
    )

    private suspend fun advance(step: OnboardingStep) {
        val delay = when (step) {
            OnboardingStep.Consent,
            OnboardingStep.DataCollection,
            OnboardingStep.FinalEnrollment,
            -> 1.seconds

            else -> 0.seconds
        }
        delay(delay) // TODO: Demo purposes only, remove when real work is done in the step
        if (step == OnboardingStep.Consent) {
            uploadConsent()
        }
        if (step == OnboardingStep.FinalEnrollment) {
            studyEnroller.enroll()
                .onFailure { logger.e(it) { "Enrollment failed; continuing into the app." } }
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
     * backend, so no project is contacted for them at all — matching iOS, which loads Firebase only
     * on the eligible and country-unavailable paths.
     */
    private fun initializeFirebaseIfNeeded(result: OnboardingStepResult) {
        val next = (result as? OnboardingStepResult.Step)?.step ?: return
        if (next != OnboardingStep.StudyOverview && next != OnboardingStep.CountryUnavailable) return
        val country = answers.value.country ?: return
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
    data object NotificationPermissionRequested : OnboardingAction
    data object NotificationSkipped : OnboardingAction
    data object ShowCountrySelectionSheet : OnboardingAction
    data class CountriesSheetDismissed(val selectedCountry: Country?) : OnboardingAction
    data object OpenStudyWebsite : OnboardingAction
    data class WaitlistEmailChanged(val email: String) : OnboardingAction
    data class ShowLearnMore(val title: StringResource, val content: StringResource) : OnboardingAction
    data class PermissionChanged(val result: PermissionResult) : OnboardingAction
    data class OnboardingAnswersChanged(val answers: OnboardingAnswers) : OnboardingAction
}

/**
 * The login step content, wrapping the account login screen. [onSuccess] advances the flow once the
 * user is signed in; [onDismiss] returns to the previous step.
 */
data class LoginStep(
    val onSuccess: () -> Unit,
    val onDismiss: () -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        AccountLoginScreen(
            dismissStyle = DismissStyle.NONE,
            onSuccess = onSuccess,
            onDismiss = onDismiss,
        )
    }
}

/**
 * The consent step content. [onConsent] receives the collected [ConsentResponses] once the document
 * is fully signed.
 */
data class ConsentStep(
    val onConsent: suspend (ConsentResponses) -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        ConsentScreen(onConsent)
    }
}

/**
 * The data and callbacks a step layout needs in order to render: the current [step] and collected
 * [answers], the coroutine [scope] for suspending actions, the available [countries] and pending
 * [waitlistEmail], plus [advance] to move forward and [push] to dispatch an [OnboardingAction].
 */
data class OnboardingStepInput(
    val step: OnboardingStep,
    val scope: CoroutineScope,
    val advance: suspend (OnboardingStep) -> Unit,
    val answers: StateFlow<OnboardingAnswers>,
    val waitlistEmail: StateFlow<String>,
    val countries: List<Country>,
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
