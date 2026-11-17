//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

/**
 * A [SignedConsentFormSource] returning the results set through [setForms] and [setDownload] (no
 * forms, and a download of the requested form, by default), recording what it was asked for.
 */
class FakeSignedConsentFormSource : SignedConsentFormSource {
    private var forms: Result<List<SignedConsentForm>> = Result.success(emptyList())
    private var download: Result<DownloadedConsentForm>? = null
    private val _discarded = mutableListOf<DownloadedConsentForm>()

    /**
     * How often the forms were read.
     */
    var reads = 0
        private set

    /**
     * Every download discarded so far, in order.
     */
    val discarded: List<DownloadedConsentForm> get() = _discarded

    /**
     * Makes every following read of the forms return [forms].
     */
    fun setForms(forms: Result<List<SignedConsentForm>>) {
        this.forms = forms
    }

    /**
     * Makes every following download return [download].
     */
    fun setDownload(download: Result<DownloadedConsentForm>) {
        this.download = download
    }

    override suspend fun forms(): Result<List<SignedConsentForm>> {
        reads++
        return forms
    }

    override suspend fun download(form: SignedConsentForm): Result<DownloadedConsentForm> =
        download ?: Result.success(DownloadedConsentFormFixtures.create(form = form))

    override fun discard(download: DownloadedConsentForm) {
        _discarded += download
    }
}
