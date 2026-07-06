package edu.stanford.myheartcounts.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.LoadingLayout

/**
 * Loading screen shown while the app determines which top-level route to display.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    LoadingLayout(modifier = modifier.fillMaxSize())
}
