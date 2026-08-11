//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import edu.stanford.spezi.ui.BottomSheetComposableContent
import edu.stanford.spezi.ui.SpeziAppBar
import edu.stanford.spezi.ui.StaticSpeziScaffold
import edu.stanford.spezi.ui.theme.Colors

/**
 * A bottom sheet that loads [url] in a web view, presented beneath the given [appBar].
 */
data class WebViewSheet(
    val appBar: SpeziAppBar,
    val url: String,
) : BottomSheetComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        StaticSpeziScaffold(appBar = appBar) {
            val backgroundColor = Colors.background.toArgb()

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setBackgroundColor(backgroundColor)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(this@WebViewSheet.url)
                    }
                },
                update = { webView ->
                    if (webView.url != this@WebViewSheet.url) {
                        webView.loadUrl(this@WebViewSheet.url)
                    }
                },
            )
        }
    }
}
