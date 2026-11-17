//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import edu.stanford.myheartcounts.study.StudyVariantProvider

/**
 * Turns a request to leave the app into the [Intent] that starts the other app.
 */
interface ExternalAppIntentMapper {

    /**
     * The intent opening [app], startable from any context, including one outside an activity.
     */
    fun mapIntent(app: NavigationEvent.ExternalApp): Intent
}

/**
 * Default [ExternalAppIntentMapper] implementation, opening the study pages of the current variant
 * of the [studyVariants].
 */
class ExternalAppIntentMapperImpl(
    private val context: Context,
    private val studyVariants: StudyVariantProvider,
) : ExternalAppIntentMapper {

    override fun mapIntent(app: NavigationEvent.ExternalApp): Intent = when (app) {
        is NavigationEvent.LaunchAppSettings -> Intent(app.action).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        NavigationEvent.OpenMailApp -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
        is NavigationEvent.OpenUrl -> url(url = app.url)
        NavigationEvent.OpenStudyHomepage -> url(url = studyVariants.currentVariant.homepage)
        NavigationEvent.OpenPrivacyPolicy -> url(url = studyVariants.currentVariant.privacyPolicy)
        is NavigationEvent.ShareFile -> Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = app.mimeType
                putExtra(Intent.EXTRA_STREAM, Uri.parse(app.uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null,
        )
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun url(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))
}
