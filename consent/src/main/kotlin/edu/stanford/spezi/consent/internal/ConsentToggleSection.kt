package edu.stanford.spezi.consent.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.SpeziCard
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A consent form section presenting a labelled boolean toggle.
 *
 * When [expectedValue] is non-null the toggle must match it for the form to be considered complete.
 */
internal data class ConsentToggleSection(
    val id: String,
    val text: String,
    val initialValue: Boolean,
    val expectedValue: Boolean?,
    val checked: Flow<Boolean>,
    val onValueChanged: (Boolean) -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        SpeziCard(modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = text,
                    style = TextStyles.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Spacings.medium),
                )
                Switch(
                    checked = checked.collectAsStateWithLifecycle(false).value,
                    onCheckedChange = onValueChanged,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ConsentToggleContentPreview() {
    SpeziTheme {
        ConsentToggleSection(
            id = "future-studies",
            text = "May we contact you about future studies that may be of interest to you?",
            initialValue = true,
            expectedValue = null,
            checked = MutableStateFlow(true),
            onValueChanged = {},
        ).Content(modifier = Modifier.fillMaxWidth())
    }
}
