//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.testing.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.grovealliance.ui.ComposableBlock
import org.grovealliance.ui.theme.GroveTheme

class ComposeContentActivity : AppCompatActivity() {

    private val content = MutableStateFlow<ComposableBlock?>(null)

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GroveTheme {
                val content by content.collectAsState()
                content?.invoke()
            }
        }
    }

    fun setScreen(content: ComposableBlock) {
        this.content.update { content }
    }
}
