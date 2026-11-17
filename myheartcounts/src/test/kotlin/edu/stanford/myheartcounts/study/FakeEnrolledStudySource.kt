//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An [EnrolledStudySource] whose enrolled study is set directly; not enrolled until then.
 */
class FakeEnrolledStudySource : EnrolledStudySource {
    private val enrolledStudy = MutableStateFlow<EnrolledStudy?>(value = null)

    override fun enrolledStudy(): Flow<EnrolledStudy?> = enrolledStudy

    /**
     * Sets the enrolled study [enrolledStudy] emits.
     */
    fun setEnrolledStudy(enrolledStudy: EnrolledStudy?) {
        this.enrolledStudy.value = enrolledStudy
    }
}
