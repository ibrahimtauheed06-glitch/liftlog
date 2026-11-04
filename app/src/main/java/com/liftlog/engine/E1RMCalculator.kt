package com.liftlog.engine

import kotlin.math.round

object E1RMCalculator {

    /**
     * e1RM = load * (1 + (reps + (10 - RPE)) / 30)
     * RPE null counts as 9.
     * load = weightKg + bwFactor * (latest 7-day avg bodyweight).
     * Only for reps <= 12; above that returns null (compare weight and reps directly).
     */
    fun calculateE1RM(
        weightKg: Double,
        reps: Int,
        rpe: Double?,
        bwFactor: Double = 0.0,
        bodyweight7DayAvg: Double = 0.0
    ): Double? {
        if (reps <= 0) return null
        if (reps > 12) return null

        val effectiveRpe = rpe ?: 9.0
        val clampedRpe = effectiveRpe.coerceIn(5.0, 10.0)
        val load = weightKg + (bwFactor * bodyweight7DayAvg)
        if (load <= 0.0) return 0.0

        val e1rm = load * (1.0 + (reps + (10.0 - clampedRpe)) / 30.0)
        return roundTo(e1rm, 0.1)
    }

    /**
     * Helper to round a double to nearest step (e.g. incrementKg).
     * If step <= 0.0, returns the value.
     */
    fun roundTo(value: Double, step: Double): Double {
        if (step <= 0.0) return value
        val factor = 1.0 / step
        return round(value * factor) / factor
    }
}
