package edu.stanford.spezi.ui.account.internal

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.TextStyles

/**
 * Composable Section title in any of the account screens
 *
 * @param title Title of the section
 */
@Composable
internal fun AccountSectionTitle(title: StringResource) {
    Text(
        text = title.text().uppercase(),
        style = TextStyles.bodySmall,
        color = Colors.onSurfaceVariant,
    )
}
