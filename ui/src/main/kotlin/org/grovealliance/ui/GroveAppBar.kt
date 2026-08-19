//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("TooManyFunctions")

package org.grovealliance.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.medium

/**
 * A data class representing the top app bar configuration for Grove screens.
 *
 * Implements [ComposableContent] so it can be rendered directly as a composable.
 *
 * @property title Optional title widget displayed in the app bar.
 * @property navigation Optional navigation action (e.g. back or close button).
 * @property actions Optional list of action widgets displayed on the trailing side.
 * @property centerAlign Whether the title should be center-aligned. Defaults to `true`.
 */
data class GroveAppBar(
    val title: GroveAppBarTitle? = null,
    val navigation: GroveAppBarNavAction? = null,
    val actions: List<GroveAppBarAction>? = null,
    val centerAlign: Boolean = true,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        GroveAppBarComposable(
            modifier = modifier,
            title = title,
            navigation = navigation,
            actions = actions,
            centerAlign = centerAlign,
        )
    }

    companion object {
        /**
         * A default empty app bar with no title, navigation, or actions.
         */
        val Empty = GroveAppBar()
    }
}

/**
 * Composable function that renders the Grove top app bar.
 *
 * Renders a [CenterAlignedTopAppBar] when [centerAlign] is `true`, otherwise a standard [TopAppBar].
 * Colors are drawn from [Colors] to match the Grove design system.
 *
 * @param modifier Modifier applied to the app bar.
 * @param title Optional title widget.
 * @param navigation Optional navigation action widget.
 * @param actions Optional list of action widgets.
 * @param centerAlign Whether the title should be center-aligned. Defaults to `true`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroveAppBarComposable(
    modifier: Modifier = Modifier,
    title: GroveAppBarTitle? = null,
    navigation: GroveAppBarNavAction? = null,
    actions: List<GroveAppBarAction>? = null,
    centerAlign: Boolean = true,
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Colors.background,
        scrolledContainerColor = Colors.background,
        navigationIconContentColor = Colors.onBackground,
        titleContentColor = Colors.onBackground,
        actionIconContentColor = Colors.onBackground,
    )
    if (centerAlign) {
        CenterAlignedTopAppBar(
            modifier = modifier
                .fillMaxWidth()
                .systemBarsPadding(),
            title = { title?.Content() },
            navigationIcon = { navigation?.Content() },
            actions = { actions?.forEach { it.Content() } },
            colors = colors,
            expandedHeight = GroveAppBarDefaults.expandedHeight,
        )
    } else {
        TopAppBar(
            modifier = modifier
                .fillMaxWidth()
                .systemBarsPadding(),
            title = { title?.Content() },
            navigationIcon = { navigation?.Content() },
            actions = { actions?.forEach { it.Content() } },
            colors = colors,
            expandedHeight = GroveAppBarDefaults.expandedHeight,
        )
    }
}

/**
 * Base sealed interface for all widgets that can appear inside a [GroveAppBar].
 */
@Stable
sealed interface GroveAppBarWidget : ComposableContent

/**
 * Sealed interface representing the title slot of a [GroveAppBar].
 */
@Stable
sealed interface GroveAppBarTitle : GroveAppBarWidget

/**
 * Sealed interface representing the navigation icon slot of a [GroveAppBar].
 */
@Stable
sealed interface GroveAppBarNavAction : GroveAppBarWidget

/**
 * Sealed interface representing an action icon in the trailing slot of a [GroveAppBar].
 */
@Stable
sealed interface GroveAppBarAction : GroveAppBarWidget

/**
 * Scope to build a [GroveAppBar].
 */
class GroveAppBarBuilderScope internal constructor() {
    private var title: GroveAppBarTitle? = null
    private var navigation: GroveAppBarNavAction? = null
    private val actions = mutableListOf<GroveAppBarAction>()
    private var centerAlign: Boolean = true

    /**
     * Sets the title to the given plain string.
     *
     * @param string The title text.
     */
    fun title(string: String) {
        title = GroveAppBarWidgets.title(string)
    }

    /**
     * Sets the title using a [StringResource].
     *
     * @param stringResource The title string resource.
     */
    fun title(stringResource: StringResource) {
        title = GroveAppBarWidgets.title(stringResource)
    }

    /**
     * Sets the title using a string resource id
     *
     * @param resId String resource id
     */
    fun title(@StringRes resId: Int) {
        title = GroveAppBarWidgets.title(resId = resId)
    }

    /**
     * Sets the title to an arbitrary [ComposableContent].
     *
     * @param content The composable content to display as the title.
     */
    fun title(content: ComposableContent) {
        title = GroveAppBarWidgets.title(content)
    }

    /**
     * Sets the navigation action widget directly.
     *
     * @param navigation The navigation action.
     */
    fun navigation(navigation: ComposableContent) {
        this.navigation = GroveAppBarWidgets.navigation(navigation)
    }

    /**
     * Sets the navigation action widget directly.
     *
     * @param navigation The navigation action.
     */
    fun navigation(navigation: GroveAppBarNavAction) {
        this.navigation = navigation
    }

    /**
     * Sets the navigation icon from an [imageResource] and click callback.
     *
     * @param imageResource The icon image resource.
     * @param onClick Callback invoked when the icon is tapped.
     */
    fun navigation(
        imageResource: ImageResource,
        onClick: OnActionVoid,
    ) {
        navigation = GroveAppBarWidgets.navigation(imageResource, onClick)
    }

    /**
     * Sets the navigation icon from a [GroveIconButton].
     *
     * @param icon The icon button to use for navigation.
     */
    fun navigation(icon: GroveIconButton) {
        navigation = GroveAppBarWidgets.navigation(icon)
    }

    /**
     * Adds an action widget directly to the trailing actions list.
     *
     * @param action The action widget to add.
     */
    fun action(action: GroveAppBarAction) {
        actions.add(action)
    }

    /**
     * Adds an action icon from an [imageResource] and click callback.
     *
     * @param imageResource The icon image resource.
     * @param onClick Callback invoked when the icon is tapped.
     */
    fun action(
        imageResource: ImageResource,
        onClick: OnActionVoid,
    ) {
        actions.add(GroveAppBarWidgets.action(imageResource, onClick))
    }

    /**
     * Adds a [GroveIconButton] as a trailing action.
     *
     * @param icon The icon button to use as an action.
     */
    fun action(icon: GroveIconButton) {
        actions.add(GroveAppBarWidgets.action(icon))
    }

    /**
     * Adds an arbitrary [ComposableContent] as a trailing action.
     *
     * @param content The composable content to use as an action.
     */
    fun action(content: ComposableContent) {
        actions.add(GroveAppBarWidgets.action(content))
    }

    /**
     * Sets a back-navigation icon that invokes [onClick] when pressed.
     *
     * @param onClick Callback invoked when the back button is tapped.
     */
    fun back(onClick: OnActionVoid) {
        navigation = GroveAppBarWidgets.back(onClick)
    }

    /**
     * Sets a close-navigation icon that invokes [onClick] when pressed.
     *
     * @param onClick Callback invoked when the close button is tapped.
     */
    fun close(onClick: OnActionVoid) {
        navigation = GroveAppBarWidgets.close(onClick)
    }

    /**
     * Sets the navigation icon based on the given [DismissStyle].
     *
     * @param style The dismiss style determining the navigation icon.
     * @param onClick Callback invoked when the navigation icon is tapped.
     */
    fun dismiss(style: DismissStyle, onClick: OnActionVoid) {
        when (style) {
            DismissStyle.CLOSE -> close(onClick)
            DismissStyle.BACK -> back(onClick)
            DismissStyle.NONE -> navigation = null
        }
    }

    /**
     * Sets whether a center aligned app bar will be built.
     */
    fun centerAlign(value: Boolean) {
        this.centerAlign = value
    }

    /**
     * Builds and returns the configured [GroveAppBar], then resets the builder state.
     *
     * @return The constructed [GroveAppBar].
     */
    internal fun build(): GroveAppBar {
        return GroveAppBar(
            title = title,
            navigation = navigation,
            actions = actions.toList(),
            centerAlign = centerAlign,
        )
    }
}

/**
 * Builds a [GroveAppBar].
 *
 * This is the preferred entry point when constructing app bars in screen code.
 *
 * Example:
 * ```kotlin
 * val appBar = groveAppBar {
 *     title("Profile")
 *     back { navigator.popBackStack() }
 *     action(ImageResource(Icons.Default.Settings)) { openSettings() }
 * }
 * ```
 */
fun groveAppBar(scope: GroveAppBarBuilderScope.() -> Unit): GroveAppBar = GroveAppBarBuilderScope().apply(scope).build()

/**
 * Remembers and builds a [GroveAppBar].
 *
 * @param key Optional key to rebuild the app bar when its identity should change.
 * @param scope Builder scope configuring the app bar.
 */
@Composable
fun rememberGroveAppBar(key: Any? = null, scope: GroveAppBarBuilderScope.() -> Unit): GroveAppBar {
    return remember(key) {
        GroveAppBarBuilderScope().apply(scope).build()
    }
}

/**
 * Factory object providing convenience methods for creating [GroveAppBarWidget] instances.
 *
 * Use these helpers to create titles, navigation icons, and action icons in a concise way.
 */
data object GroveAppBarWidgets {

    /**
     * Creates a text title widget from a plain string.
     *
     * @param string The title text.
     */
    fun title(string: String): GroveAppBarTitle = title(StringResource(string))

    /**
     * Creates a text title widget from a string resource id
     *
     * @param resId String resource id
     */
    fun title(@StringRes resId: Int): GroveAppBarTitle = title(StringResource(id = resId))

    /**
     * Creates a text title widget from a [StringResource].
     *
     * @param stringResource The title string resource.
     */
    fun title(stringResource: StringResource): GroveAppBarTitle = TextTitle(stringResource)

    /**
     * Creates a title widget from arbitrary [ComposableContent].
     *
     * @param content The composable content to use as the title.
     */
    fun title(content: ComposableContent): GroveAppBarTitle = ComposableContentWrapper(content)

    /**
     * Creates a trailing action icon widget.
     *
     * @param imageResource The icon image resource.
     * @param onClick Callback invoked when the action icon is tapped.
     */
    fun action(
        imageResource: ImageResource,
        onClick: OnActionVoid,
    ): GroveAppBarAction = action(icon = GroveIconButton(image = imageResource, onClick = onClick))

    /**
     * Creates a trailing action icon widget from a [GroveIconButton].
     *
     * @param icon The icon button used as an action.
     */
    fun action(icon: GroveIconButton): GroveAppBarAction = ComposableContentWrapper(icon)

    /**
     * Creates a navigation icon widget.
     *
     * @param imageResource The icon image resource.
     * @param onClick Callback invoked when the navigation icon is tapped.
     */
    fun navigation(
        imageResource: ImageResource,
        onClick: OnActionVoid,
    ): GroveAppBarNavAction = navigation(icon = GroveIconButton(image = imageResource, onClick = onClick))

    /**
     * Creates a navigation icon widget from a [GroveIconButton].
     *
     * @param icon The icon button used as navigation action.
     */
    fun navigation(icon: GroveIconButton): GroveAppBarNavAction = ComposableContentWrapper(icon)

    /**
     * Creates a close navigation icon widget.
     *
     * @param onClick Callback invoked when the close icon is tapped.
     */
    fun close(onClick: OnActionVoid): GroveAppBarNavAction = navigation(icon = GroveIconButton.close(onClick))

    /**
     * Creates a back navigation icon widget.
     *
     * @param onClick Callback invoked when the back icon is tapped.
     */
    fun back(onClick: OnActionVoid): GroveAppBarNavAction = navigation(icon = GroveIconButton.back(onClick))

    /**
     * Creates a trailing action widget from arbitrary [ComposableContent].
     *
     * @param content The composable content to use as the action.
     */
    fun action(content: ComposableContent): GroveAppBarAction = ComposableContentWrapper(content)

    /**
     * Creates a navigation widget from arbitrary [ComposableContent].
     *
     * @param content The composable content to use as the navigation icon.
     */
    fun navigation(content: ComposableContent): GroveAppBarNavAction = ComposableContentWrapper(content)
}

/**
 * Default values and tokens used by [GroveAppBar] and [GroveAppBarComposable].
 */
object GroveAppBarDefaults {

    /** The default expanded height for the app bar, sourced from Material 3 defaults. */
    @OptIn(ExperimentalMaterial3Api::class)
    val expandedHeight
        @Composable get() = TopAppBarDefaults.TopAppBarExpandedHeight
}

@Immutable
private data class ComposableContentWrapper(
    private val content: ComposableContent,
) : GroveAppBarTitle, GroveAppBarAction, GroveAppBarNavAction {
    @Composable
    override fun Content(modifier: Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) { content.Content() }
    }
}

@Immutable
private data class TextTitle(private val text: StringResource) : GroveAppBarTitle {
    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            modifier = modifier,
            textAlign = TextAlign.Center,
            text = text.text(),
            style = TextStyles.bodyLarge.medium(),
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val item = groveAppBar {
        title("Title")
        back { }
        action(ImageResource(Icons.Default.Favorite)) { }
    }

    GroveTheme {
        item.Content(modifier = Modifier.fillMaxWidth())
    }
}
