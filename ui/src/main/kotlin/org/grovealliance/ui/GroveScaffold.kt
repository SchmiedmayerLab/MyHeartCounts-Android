//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.grovealliance.core.coroutines.CoroutinesLauncher
import org.grovealliance.ui.GroveScaffoldState.Companion.invoke
import org.grovealliance.ui.MutableGroveScaffoldState.Companion.invoke
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Renders a [GroveScaffold] without a background content slot.
 *
 * @param state The scaffold state driving the app bar, toast, bottom sheet, and overlay.
 * @param content The main screen content rendered inside the scaffold's content area.
 */
@Composable
fun GroveScaffold(
    state: GroveScaffoldState,
    content: @Composable BoxScope.() -> Unit,
) {
    GroveScaffold(
        state = state,
        backgroundContent = {},
        content = content,
    )
}

/**
 * The primary Grove scaffold composable that renders a full screen layout.
 *
 * Handles the app bar, toast notifications, bottom sheet, and overlay driven by [state],
 * while composing the main [content] and an optional full-bleed [backgroundContent] behind it.
 *
 * Example:
 * ```kotlin
 *
 * // ViewModel
 * class MyViewModel : ViewModel() {
 *     private val scaffoldState = MutableGroveScaffoldState(
 *         coroutinesLauncher = coroutinesLauncher,
 *         appBar = GroveAppBarBuilder().title("My Screen").build(),
 *     )
 *
 *     val uiState = MyScreenState(
 *         scaffoldState = scaffoldState.asScaffoldState(),
 *         items = items,
 *     )
 *
 *     fun onLoadError() {
 *         scaffoldState.showToast(message = StringResource(Strings.error_loading))
 *     }
 * }
 *
 * // Screen
 * @Composable
 * fun MyScreen(viewModel: MyViewModel) {
 *     val uiState by viewModel.uiState.collectAsStateWithLifecycle()
 *     GroveScaffold(state = uiState.scaffoldState) {
 *         LazyColumn {
 *             items(uiState.items) { MyItemRow(it) }
 *         }
 *     }
 * }
 * ```
 *
 * @param state The scaffold state driving all widget slots.
 * @param backgroundContent Optional full-bleed content drawn behind the main content area (e.g. a map or image).
 * @param content The main screen content.
 */
@Composable
fun GroveScaffold(
    state: GroveScaffoldState,
    backgroundContent: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val widgets by state.widgets.collectAsStateWithLifecycle()
    val hasAppBar = widgets.appBar != null
    Scaffold(
        topBar = { widgets.appBar?.Content() }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            backgroundContent()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .consumeWindowInsets(contentPadding)
                    .padding(top = if (hasAppBar && !widgets.ignoreAppBarPadding) GroveAppBarDefaults.expandedHeight else 0.dp),
                content = content,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val localToast = rememberLocalToast(toast = widgets.toast)
        CompositionLocalProvider(LocalBottomSheetCoveringContent provides localToast.toast) {
            widgets.bottomSheet?.Sheet(Modifier.systemBarsPadding())
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.TopStart),
                visible = localToast.visible,
                enter = slideInVertically(
                    animationSpec = tween(TOAST_ANIMATION_DURATION.toInt()),
                    initialOffsetY = { fullHeight -> -fullHeight }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(TOAST_ANIMATION_DURATION.toInt()),
                    targetOffsetY = { fullHeight -> -fullHeight }
                )
            ) {
                localToast.toast?.Content()
            }
        }
        widgets.overlay?.Content(Modifier.fillMaxSize())
    }
}

/**
 * Convenience overload of [GroveScaffold] for static use-cases,
 *
 * Constructs an immutable [GroveScaffoldState] from the given parameters and passes it to the main
 * scaffold implementation. For dynamic runtime usage, construct a [MutableGroveScaffoldState]
 * and pass it directly
 */
@Composable
fun StaticGroveScaffold(
    appBar: GroveAppBar? = null,
    ignoreAppBarPadding: Boolean = false,
    backgroundContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    GroveScaffold(
        state = rememberMutableGroveScaffoldState(appBar = appBar, ignoreAppBarPadding = ignoreAppBarPadding),
        backgroundContent = backgroundContent,
        content = content,
    )
}

/**
 * Read-only state interface for [GroveScaffold].
 *
 * Exposes a [StateFlow] of [GroveScaffoldWidgets] that the scaffold observes to update its
 * app bar, toast, bottom sheet, and overlay slots reactively.
 *
 * Use the companion [invoke] factory for static / preview instances. For fully mutable
 * runtime state, use [MutableGroveScaffoldState].
 */
interface GroveScaffoldState {
    val widgets: StateFlow<GroveScaffoldWidgets>

    companion object {
        /**
         * Creates a static [GroveScaffoldState] with the given initial widgets.
         *
         * Intended for previews, screenshot tests, and other static use-cases where no
         * imperative mutations are needed. The returned state is immutable after construction.
         *
         * @param appBar Optional app bar to display.
         * @param ignoreAppBarPadding When `true`, content is not offset below the app bar.
         * @param toast Optional initial toast to display.
         * @param bottomSheet Optional initial bottom sheet to display.
         * @param overlay Optional initial overlay to display.
         */
        operator fun invoke(
            appBar: GroveAppBar? = null,
            ignoreAppBarPadding: Boolean = false,
            toast: GroveToast? = null,
            bottomSheet: BottomSheetComposableContent? = null,
            overlay: ComposableContent? = null,
        ): GroveScaffoldState = object : GroveScaffoldState {
            private val _widgets = MutableStateFlow(
                GroveScaffoldWidgets(
                    appBar = appBar,
                    ignoreAppBarPadding = ignoreAppBarPadding,
                    toast = toast,
                    bottomSheet = bottomSheet,
                    overlay = overlay,
                )
            )
            override val widgets = _widgets.asStateFlow()
        }
    }
}

/**
 * Mutable extension of [GroveScaffoldState] that exposes imperative controls for all widget slots.
 *
 * Obtain an instance via the companion [invoke] factory, typically inside a ViewModel:
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     val scaffoldState = MutableGroveScaffoldState(coroutinesLauncher)
 * }
 * ```
 *
 * Pass [asScaffoldState] to the UI layer to prevent it from calling mutation methods directly.
 *
 * **Thread safety:** All state mutations and the sheet dismiss guard are thread-safe,
 * so calls from any thread are safe.
 */
interface MutableGroveScaffoldState : GroveScaffoldState {

    /** Sets the app bar to display, or `null` to hide it. */
    fun setAppBar(appBar: GroveAppBar?)

    /** Presents [sheet] as a modal bottom sheet, replacing any currently shown sheet. */
    fun showBottomSheet(sheet: BottomSheetComposableContent)

    /**
     * Programmatically dismisses the currently shown bottom sheet.
     * No-ops if no sheet is visible or one is already in the process of being dismissed.
     */
    fun dismissBottomSheet()

    /** Presents [overlay] as a full-size composable drawn above all other scaffold content. */
    fun showOverlay(overlay: ComposableContent)

    /** Removes the currently shown overlay. */
    fun dismissOverlay()

    /**
     * Shows a [GroveToast] with the given parameters.
     *
     * If a toast is already visible, it is replaced immediately and any pending auto-dismiss
     * timer for the previous toast is canceled.
     *
     * @param imageResource Optional leading icon for the toast.
     * @param message The toast message.
     * @param displayStyle Controls how long the toast stays visible. Defaults to [GroveToastDisplayStyle.DefaultShort].
     */
    fun showToast(
        imageResource: ImageResource? = null,
        message: StringResource,
        displayStyle: GroveToastDisplayStyle = GroveToastDisplayStyle.DefaultShort,
    )

    /** Immediately hides the current toast and cancels any pending auto-dismiss timer. */
    fun hideToast()

    /**
     * Controls whether the scaffold's content area is padded below the app bar.
     *
     * Set to `true` when the content should render behind a transparent or floating app bar.
     */
    fun setIgnoreAppBarPadding(ignoreAppBarPadding: Boolean)

    /**
     * Returns a read-only [GroveScaffoldState] view of this mutable state.
     *
     * Pass this to the UI layer to prevent composables from calling mutation methods directly.
     */
    fun asScaffoldState(): GroveScaffoldState

    companion object {
        /**
         * Creates a [MutableGroveScaffoldState] backed by [coroutinesLauncher] for auto-dismiss
         * toast scheduling.
         *
         * @param coroutinesLauncher The launcher used to schedule auto-dismissible toasts.
         * @param appBar Optional initial app bar configuration.
         * @param ignoreAppBarPadding Initial value for the app bar padding flag.
         */
        operator fun invoke(
            coroutinesLauncher: CoroutinesLauncher,
            appBar: GroveAppBar? = null,
            ignoreAppBarPadding: Boolean = false,
        ): MutableGroveScaffoldState = MutableGroveScaffoldStateImpl(
            coroutinesLauncher = coroutinesLauncher,
            appBar = appBar,
            ignoreAppBarPadding = ignoreAppBarPadding,
        )
    }
}

/**
 * Shows a [Warning] toast with the given [message].
 */
fun MutableGroveScaffoldState.showErrorToast(
    message: StringResource,
    displayStyle: GroveToastDisplayStyle = GroveToastDisplayStyle.DefaultShort,
) = showToast(
    imageResource = ImageResource(Icons.Default.Warning),
    message = message,
    displayStyle = displayStyle,
)

/**
 * Creates a [MutableGroveScaffoldState] with the given parameters, using [coroutinesLauncher] from the receiver [ViewModel].
 *
 * This is a convenience function for quickly creating scaffold state inside a ViewModel without needing to
 * pass the coroutines launcher explicitly. For more complex scenarios (e.g. when you need to customize
 * the initial state or manage multiple scaffold states), consider constructing a [MutableGroveScaffoldState]
 * instance directly using its companion factory.
 *
 * @param appBar Optional initial app bar configuration.
 * @param ignoreAppBarPadding Initial value for the app bar padding flag.
 */
fun ViewModel.mutableScaffoldState(
    appBar: GroveAppBar? = null,
    ignoreAppBarPadding: Boolean = false,
): MutableGroveScaffoldState = MutableGroveScaffoldState(
    coroutinesLauncher = coroutinesLauncher,
    appBar = appBar,
    ignoreAppBarPadding = ignoreAppBarPadding,
)

/**
 * Creates and remembers a [MutableGroveScaffoldState] scoped to the current composition.
 *
 * The state is re-created whenever [appBar] or [ignoreAppBarPadding] changes. The underlying
 * coroutine scope (and therefore any pending auto-dismiss toast timers) is canceled automatically
 * when the composition leaves the tree.
 *
 * Prefer this over constructing a [MutableGroveScaffoldState] manually inside a composable,
 * as it handles coroutine-scope lifecycle and key-based invalidation for you.
 *
 * @param appBar Optional initial app bar configuration.
 * @param ignoreAppBarPadding When `true`, content is not offset below the app bar. Defaults to `false`.
 * @return A remembered [MutableGroveScaffoldState] tied to the current composition's lifecycle.
 */
@Composable
fun rememberMutableGroveScaffoldState(
    appBar: GroveAppBar? = null,
    ignoreAppBarPadding: Boolean = false,
): MutableGroveScaffoldState {
    val coroutinesLauncher = rememberCoroutinesLauncher()
    return remember(coroutinesLauncher, appBar, ignoreAppBarPadding) {
        MutableGroveScaffoldState(
            coroutinesLauncher = coroutinesLauncher,
            appBar = appBar,
            ignoreAppBarPadding = ignoreAppBarPadding,
        )
    }
}

/**
 * A snapshot of all widget slots managed by [GroveScaffold].
 *
 * Emitted by [GroveScaffoldState.widgets] whenever any slot changes. All properties are
 * nullable — a `null` value means the corresponding slot is not currently shown.
 *
 * @property appBar The app bar to display, or `null` if none.
 * @property ignoreAppBarPadding When `true`, [content][GroveScaffold] is not offset below the app bar.
 * @property toast The toast currently being shown, or `null` if none.
 * @property bottomSheet The bottom sheet currently being shown, or `null` if none.
 * @property overlay The overlay currently being shown, or `null` if none.
 */
data class GroveScaffoldWidgets(
    val appBar: GroveAppBar?,
    val ignoreAppBarPadding: Boolean,
    val toast: GroveToast?,
    val bottomSheet: BottomSheetComposableContent?,
    val overlay: ComposableContent?,
)

private class MutableGroveScaffoldStateImpl(
    private val coroutinesLauncher: CoroutinesLauncher,
    appBar: GroveAppBar?,
    ignoreAppBarPadding: Boolean,
) : MutableGroveScaffoldState {
    private val sheetEventSink = EventSink<BottomSheetEvent>()
    private val sheetEventSource = sheetEventSink.source()
    private var autoDismissToastJob: kotlinx.coroutines.Job? = null
    private val isDismissingSheet = AtomicBoolean(false)

    private val immutableState by lazy {
        object : GroveScaffoldState {
            override val widgets: StateFlow<GroveScaffoldWidgets> = this@MutableGroveScaffoldStateImpl.widgets
        }
    }

    private val _widgets = MutableStateFlow(
        GroveScaffoldWidgets(
            appBar = appBar,
            ignoreAppBarPadding = ignoreAppBarPadding,
            toast = null,
            bottomSheet = null,
            overlay = null,
        )
    )
    override val widgets: StateFlow<GroveScaffoldWidgets> = _widgets.asStateFlow()

    override fun setAppBar(appBar: GroveAppBar?) {
        _widgets.update { it.copy(appBar = appBar) }
    }

    override fun hideToast() {
        autoDismissToastJob?.cancel()
        autoDismissToastJob = null
        _widgets.update { it.copy(toast = null) }
    }

    override fun showBottomSheet(sheet: BottomSheetComposableContent) {
        isDismissingSheet.set(false)
        _widgets.update { it.copy(bottomSheet = BottomSheetContentWrapper(sheet)) }
    }

    override fun dismissBottomSheet() {
        if (_widgets.value.bottomSheet == null || !isDismissingSheet.compareAndSet(false, true)) return
        sheetEventSink.push(BottomSheetEvent.Dismiss)
    }

    override fun showOverlay(overlay: ComposableContent) {
        _widgets.update { it.copy(overlay = overlay) }
    }

    override fun dismissOverlay() {
        _widgets.update { it.copy(overlay = null) }
    }

    override fun asScaffoldState(): GroveScaffoldState = immutableState

    override fun showToast(
        imageResource: ImageResource?,
        message: StringResource,
        displayStyle: GroveToastDisplayStyle,
    ) {
        val toast = GroveToast(
            image = imageResource,
            message = message,
            onClick = ::hideToast,
        )
        _widgets.update { it.copy(toast = toast) }
        autoDismissToastJob?.cancel()
        autoDismissToastJob = coroutinesLauncher.launch {
            when (displayStyle) {
                is GroveToastDisplayStyle.AutoDismissible -> {
                    delay(displayStyle.after)
                    hideToast()
                }
                GroveToastDisplayStyle.Sticky -> Unit
            }
        }
    }

    override fun setIgnoreAppBarPadding(ignoreAppBarPadding: Boolean) {
        _widgets.update { it.copy(ignoreAppBarPadding = ignoreAppBarPadding) }
    }

    private inner class BottomSheetContentWrapper(
        private val content: BottomSheetComposableContent,
    ) : BottomSheetComposableContent by content {
        override val onDismiss: () -> Unit
            get() = {
                isDismissingSheet.set(false)
                content.onDismiss()
                _widgets.update { it.copy(bottomSheet = null) }
            }
        override val events: BottomSheetEventSource = sheetEventSource

        @Composable
        override fun Sheet(modifier: Modifier) {
            super.Sheet(modifier)
        }
    }
}

@Composable
private fun rememberLocalToast(toast: GroveToast?): LocalGroveToast {
    // Skip animation for previews / screenshot tests
    if (LocalInspectionMode.current) return LocalGroveToast(visible = toast != null, toast = toast)
    var toastVisible by remember { mutableStateOf(false) }
    var localToast: GroveToast? by remember { mutableStateOf(null) }
    LaunchedEffect(toast) {
        toastVisible = toast != null
        if (!toastVisible) delay(TOAST_ANIMATION_DURATION)
        localToast = toast
    }
    return remember(toastVisible, localToast) { LocalGroveToast(visible = toastVisible, toast = localToast) }
}

@Immutable
private data class LocalGroveToast(val visible: Boolean, val toast: GroveToast?)

private const val TOAST_ANIMATION_DURATION = 300L
