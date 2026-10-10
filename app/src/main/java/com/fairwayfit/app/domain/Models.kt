package com.fairwayfit.app.domain

import kotlin.math.roundToInt

enum class Category(val label: String) {
    STABILITY("Stability"),
    POWER("Speed & Power"),
    MOBILITY("Mobility")
}

/**
 * One baseline fitness test. [floor] is the value that scores 0 and [target] the value that scores 100.
 * These are generic benchmarks for an active adult golfer, not clinical norms.
 */
data class FitnessTest(
    val key: String,
    val name: String,
    val category: Category,
    val unit: String,
    val timed: Boolean,
    val countdownSeconds: Int = 0,
    val min: Double,
    val max: Double,
    val step: Double,
    val start: Double,
    val floor: Double,
    val target: Double,
    val summary: String,
    val steps: List<String>,
    val why: String
)

data class Exercise(
    val id: String,
    val name: String,
    val category: Category,
    val focus: String,
    val equipment: String,
    val steps: List<String>,
    val cue: String,
    val why: String
)

/** One line in a workout. [seconds] > 0 means a timed hold; otherwise [reps] are counted. */
data class WorkoutItem(
    val exerciseId: String,
    val sets: Int,
    val reps: Int = 0,
    val seconds: Int = 0,
    val perSide: Boolean = false
) {
    init {
        require(sets > 0) { "sets must be positive" }
        require((reps > 0) != (seconds > 0)) { "exactly one of reps/seconds must be set for $exerciseId" }
    }
}

data class Workout(
    val id: String,
    val name: String,
    val badge: String,
    val tagline: String,
    val notes: String,
    val category: Category?,
    val restSeconds: Int,
    val items: List<WorkoutItem>
)

val Workout.estimatedMinutes: Int
    get() {
        var workSec = 0
        var setGroups = 0
        for (item in items) {
            val perSet = if (item.seconds > 0) item.seconds else item.reps * 3
            val sides = if (item.perSide) 2 else 1
            workSec += item.sets * sides * perSet
            setGroups += item.sets
        }
        val restSec = (setGroups - 1).coerceAtLeast(0) * restSeconds
        return ((workSec + restSec) / 60.0).roundToInt().coerceAtLeast(1)
    }

fun WorkoutItem.prescription(): String {
    val amount = if (seconds > 0) "$seconds s" else "$reps"
    val side = if (perSide) " / side" else ""
    return "$sets × $amount$side"
}

data class TestSession(
    val id: String,
    val dateMillis: Long,
    val values: Map<String, Double>
) {
    val overall: Int? get() = Scoring.overall(values)
}

object Scoring {
    fun score(test: FitnessTest, value: Double): Int {
        val fraction = (value - test.floor) / (test.target - test.floor)
        return (fraction.coerceIn(0.0, 1.0) * 100).roundToInt()
    }

    fun categoryScore(values: Map<String, Double>, category: Category): Int? {
        val scores = Content.tests
            .filter { it.category == category }
            .mapNotNull { test -> values[test.key]?.let { score(test, it) } }
        return if (scores.isEmpty()) null else scores.average().roundToInt()
    }

    /** Average of the category scores, so every category counts equally regardless of test count. */
    fun overall(values: Map<String, Double>): Int? {
        val categories = Category.entries.mapNotNull { categoryScore(values, it) }
        return if (categories.isEmpty()) null else categories.average().roundToInt()
    }
}
