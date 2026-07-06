@file:Suppress("SpacingBetweenDeclarationsWithAnnotations")

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A participant's behaviour-change stage.
 */
@Serializable
enum class StageOfChangeOption {
    @SerialName("0") NOT_SET,
    @SerialName("a") A,
    @SerialName("b") B,
    @SerialName("c") C,
    @SerialName("d") D,
    @SerialName("e") E,
    @SerialName("f") F,
    @SerialName("g") G,
    @SerialName("h") H,
    @SerialName("i") I,
}
