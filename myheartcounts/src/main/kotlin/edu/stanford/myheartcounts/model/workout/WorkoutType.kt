package edu.stanford.myheartcounts.model.workout

/**
 * A selectable workout type. The stable [id] is its persisted identity (used by [WorkoutTypes]).
 */
enum class WorkoutType(val id: String) {
    WALK("walk"),
    RUN("run"),
    BICYCLE("bicycle"),
    SWIM("swim"),
    STRENGTH("strength"),
    HIIT("HIIT"),
    YOGA_PILATES("yoga/pilates"),
    SPORT("sport"),
    OTHER("other"),
    ;

    companion object {
        fun fromId(id: String): WorkoutType? = entries.firstOrNull { it.id == id }
    }
}
