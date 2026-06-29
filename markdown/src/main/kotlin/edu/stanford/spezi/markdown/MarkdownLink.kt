package edu.stanford.spezi.markdown

/**
 * A typed link discovered within markdown text.
 */
sealed interface MarkdownLink {
    /**
     * A web address with an `http`, `https`, or `www.` prefix.
     */
    data class Web(val url: String) : MarkdownLink

    /**
     * An email address.
     */
    data class Email(val address: String) : MarkdownLink

    /**
     * A telephone number taken from an explicit `tel:` link.
     */
    data class Phone(val number: String) : MarkdownLink
}

/**
 * How taps on a detected [MarkdownLink] within rendered markdown text are handled.
 */
sealed interface LinkClickStrategy {

    /**
     * Opens the link with the platform handler: web addresses launch the browser, `mailto:` links
     * the email app, and `tel:` links the dialer.
     */
    data object Default : LinkClickStrategy

    /**
     * Routes every tapped [MarkdownLink] to [onClick] instead of opening it with the platform handler.
     */
    data class OnClick(val onClick: (MarkdownLink) -> Unit) : LinkClickStrategy
}

/**
 * The canonical, openable URL for this link.
 *
 * Web addresses are returned with an `https://` scheme (a `www.`-prefixed host gains one); emails
 * become `mailto:` URIs and phone numbers `tel:` URIs. A value that already carries its scheme is
 * returned unchanged.
 */
fun MarkdownLink.href(): String = when (this) {
    is MarkdownLink.Web -> if (url.startsWith("www.", ignoreCase = true)) "https://$url" else url
    is MarkdownLink.Email -> address.ensurePrefix("mailto:")
    is MarkdownLink.Phone -> number.ensurePrefix("tel:")
}

private fun String.ensurePrefix(prefix: String): String =
    if (startsWith(prefix, ignoreCase = true)) this else prefix + this

/**
 * A [MarkdownLink] together with the index [range] and source [text] it was detected at.
 *
 * @property range The inclusive index range of [text] within the scanned string.
 * @property text The exact source substring the link was detected from.
 * @property link The typed link.
 */
data class DetectedLink(
    val range: IntRange,
    val text: String,
    val link: MarkdownLink,
)

private const val MIN_TEL_DIGITS = 3
private const val WEB_TRAILING = ".,;:!?"
private const val GROUP_MAILTO = 1
private const val GROUP_TEL = 2
private const val GROUP_WEB = 3

private const val EMAIL = """[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""
private val LINK_REGEX = Regex(
    "(mailto:$EMAIL)" +
        "|(tel:[+0-9().-]{$MIN_TEL_DIGITS,})" +
        "|((?:https?://|www\\.)[^\\s<>()\\[\\]]+)" +
        "|($EMAIL)"
)

/**
 * Detects web, email, and telephone links within [text], in order of appearance.
 *
 * Web addresses (`http`/`https`/`www.`) and email addresses are recognized inline; telephone numbers are
 * recognized only from explicit `tel:` links, never from bare digits.
 */
fun detectLinks(text: String): List<DetectedLink> =
    LINK_REGEX.findAll(text).map { it.toDetectedLink() }.toList()

private fun MatchResult.toDetectedLink(): DetectedLink = when {
    groupValues[GROUP_MAILTO].isNotEmpty() ->
        DetectedLink(range, value, MarkdownLink.Email(value.removePrefix("mailto:")))
    groupValues[GROUP_TEL].isNotEmpty() ->
        DetectedLink(range, value, MarkdownLink.Phone(value.removePrefix("tel:")))
    groupValues[GROUP_WEB].isNotEmpty() -> webLink(range, value)
    else -> DetectedLink(range, value, MarkdownLink.Email(value))
}

private fun webLink(range: IntRange, raw: String): DetectedLink {
    val trimmed = raw.trimEnd { it in WEB_TRAILING }
    return DetectedLink(range.first..(range.first + trimmed.length - 1), trimmed, MarkdownLink.Web(trimmed))
}
