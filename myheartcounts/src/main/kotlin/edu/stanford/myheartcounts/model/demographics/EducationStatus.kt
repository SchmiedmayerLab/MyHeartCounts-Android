@file:Suppress("SpacingBetweenDeclarationsWithAnnotations")

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A participant's educational level (US).
 */
@Serializable
enum class EducationStatusUS {
    @SerialName("notSet") NOT_SET,
    @SerialName("preferNotToState") PREFER_NOT_TO_STATE,
    @SerialName("didNotAttendSchool") DID_NOT_ATTEND_SCHOOL,
    @SerialName("gradeSchool") GRADE_SCHOOL,
    @SerialName("highSchool") HIGH_SCHOOL,
    @SerialName("someCollege") SOME_COLLEGE,
    @SerialName("bachelor") BACHELOR,
    @SerialName("master") MASTER,
    @SerialName("doctoralDegree") DOCTORAL_DEGREE,
}

/**
 * A participant's educational level (UK).
 */
@Serializable
enum class EducationStatusUK {
    @SerialName("notSet") NOT_SET,
    @SerialName("preferNotToState") PREFER_NOT_TO_STATE,
    @SerialName("didNotAttendSchool") DID_NOT_ATTEND_SCHOOL,
    @SerialName("highSchool") HIGH_SCHOOL,
    @SerialName("vocationalTraining") VOCATIONAL_TRAINING,
    @SerialName("someCollege") SOME_COLLEGE,
    @SerialName("master") MASTER,
    @SerialName("doctoralDegree") DOCTORAL_DEGREE,
}
