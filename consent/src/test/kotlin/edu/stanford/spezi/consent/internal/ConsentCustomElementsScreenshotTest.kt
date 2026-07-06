package edu.stanford.spezi.consent.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.consent.SignatureMetadata
import edu.stanford.spezi.consent.SignaturePoint
import edu.stanford.spezi.consent.SignatureStroke
import edu.stanford.spezi.markdown.MarkdownTextBlock
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.theme.Spacings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

@Suppress("MaxLineLength")
class ConsentCustomElementsScreenshotTest : ScreenshotTest() {

    @Test
    fun `Consent custom elements screenshot`() {
        screenshot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            ) {
                item {
                    ToggleElement()
                }
                item {
                    SelectElement()
                }
                item {
                    SignatureElement()
                }
            }
        }
    }

    @Composable
    private fun ToggleElement() {
        ConsentToggleSection(
            id = "future-studies",
            text = "May we contact you about future studies that may be of interest to you?",
            initialValue = true,
            expectedValue = null,
            checked = MutableStateFlow(true),
            onValueChanged = {},
        ).Content(modifier = Modifier.fillMaxWidth())
    }

    @Composable
    private fun SelectElement() {
        ConsentSelectSection(
            id = "short-term-physical-activity-trial",
            text = "Would you like to join the short term physical activity promoting trial?",
            footnote = "If you select **yes**, you will go straight from the baseline monitoring week into the randomized crossover trial.\nIf you select **no**, you will still be able to use the base My Heart Counts application.",
            options = listOf(
                SelectionOption("short-term-physical-activity-trial-yes", "Yes"),
                SelectionOption("short-term-physical-activity-trial-no", "No"),
            ),
            initialValue = "",
            expectedSelection = ExpectedSelection.Anything(allowEmptySelection = true),
            selectedId = MutableStateFlow("short-term-physical-activity-trial-yes"),
            onOptionSelected = {},
        ).Content(modifier = Modifier.fillMaxWidth())
    }

    @Composable
    private fun SignatureElement() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            MarkdownTextBlock(
                text = "Signing your name means you agree to be in this study and that you will receive a copy of this signed and dated consent form. You will be asked to provide an electronic signature. Providing your name means you agree to be in this study and that you will receive a copy of this signed and dated consent form.",
            )
            ConsentSignatureSection(
                id = "sig",
                dateText = "06/07/2026",
                metadata = flowOf(signatureMetadata),
                onFirstNameChanged = {},
                onLastNameChanged = {},
                onSignatureStrokesChanged = {},
            ).Content(modifier = Modifier.fillMaxWidth())
        }
    }

    private val signatureMetadata = SignatureMetadata(
        givenName = "Leland",
        familyName = "Stanford",
        strokes = listOf(
            SignatureStroke(
                points = listOf(
                    SignaturePoint(x = 32f, y = 86f),
                    SignaturePoint(x = 72f, y = 64f),
                    SignaturePoint(x = 122f, y = 88f),
                    SignaturePoint(x = 172f, y = 56f),
                    SignaturePoint(x = 226f, y = 82f),
                )
            )
        ),
    )
}
