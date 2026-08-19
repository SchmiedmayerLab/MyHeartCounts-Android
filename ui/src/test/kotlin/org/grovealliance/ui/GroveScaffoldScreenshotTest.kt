//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.medium
import org.junit.Test

class GroveScaffoldScreenshotTest : ScreenshotTest() {

    @Test
    fun `GroveScaffold with app bar screenshot`() {
        screenshot(state = scaffoldState(appBar = appBar()))
    }

    @Test
    fun `GroveScaffold with app bar and back navigation screenshot`() {
        screenshot(state = scaffoldState(appBar = appBar(withBack = true)))
    }

    @Test
    fun `GroveScaffold with toast screenshot`() {
        screenshot(
            state = scaffoldState(
                appBar = appBar(),
                toast = GroveToast(
                    image = ImageResource(Icons.Default.Error),
                    message = StringResource("Something went wrong. Please try again."),
                    onClick = {},
                )
            )
        )
    }

    @Test
    fun `GroveScaffold with bottom sheet screenshot`() {
        screenshot(
            state = scaffoldState(
                appBar = appBar(),
                bottomSheet = PreviewBottomSheet,
            )
        )
    }

    @Test
    fun `GroveScaffold with loading overlay`() {
        screenshot(
            state = scaffoldState(
                appBar = appBar(),
                overlay = LoadingLayout(message = StringResource("Loading..."), style = LoadingLayoutStyle.Overlay),
            )
        )
    }

    @Test
    fun `GroveScaffold without app bar screenshot`() {
        screenshot(state = scaffoldState())
    }

    private fun screenshot(state: GroveScaffoldState) {
        screenshot {
            GroveScaffold(state) { ScreenContent() }
        }
    }

    private fun appBar(withBack: Boolean = false) = groveAppBar {
        title("My Screen")
        action(ImageResource(Icons.Default.MoreVert))
        if (withBack) back { }
    }

    private fun scaffoldState(
        appBar: GroveAppBar? = null,
        toast: GroveToast? = null,
        bottomSheet: BottomSheetComposableContent? = null,
        overlay: ComposableContent? = null,
    ) = GroveScaffoldState(
        appBar = appBar,
        toast = toast,
        bottomSheet = bottomSheet,
        overlay = overlay,
    )

    private object PreviewBottomSheet : BottomSheetComposableContent {
        @Composable
        override fun Content(modifier: Modifier) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(Spacings.large),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Sheet Title",
                    style = TextStyles.titleMedium.medium(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "This is a bottom sheet with some content.",
                    style = TextStyles.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    private data class Item(val name: String, val subtitle: String, val icon: ImageResource)

    private val items = listOf(
        Item("Leland Stanford", "leland@stanford.edu", ImageResource(Icons.Default.Person)),
        Item("Jane Doe", "jane.doe@stanford.edu", ImageResource(Icons.Default.Star)),
        Item("John Smith", "john.smith@stanford.edu", ImageResource(Icons.Default.Favorite)),
        Item("Alice Johnson", "alice.j@stanford.edu", ImageResource(Icons.Default.Settings)),
        Item("Bob Williams", "bob.w@stanford.edu", ImageResource(Icons.Default.Person)),
    )

    @Composable
    private fun ScreenContent() {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = Spacings.small),
        ) {
            items(items) { item ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacings.medium, vertical = Spacings.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
                    ) {
                        item.icon.Content(modifier = Modifier.size(24.dp))
                        Column {
                            Text(text = item.name, style = TextStyles.bodyLarge.medium())
                            Text(text = item.subtitle, style = TextStyles.bodySmall)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                }
            }
        }
    }
}
