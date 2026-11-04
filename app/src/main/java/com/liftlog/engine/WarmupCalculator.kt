package com.liftlog.engine

data class WarmupSetTarget(
    val weightKg: Double,
    val reps: Int,
    val percentage: Int
)

object WarmupCalculator {

    /**
     * Calculates warm-up sets for an exercise.
     * Rule 8: The first compound of each workout gets one prefilled warm-up set at 60% of target weight.
     * Phase 2: 2-set warm-up ramp (50% and 75%).
     * All weights rounded to incrementKg.
     */
    fun calculateWarmupSets(
        targetWeightKg: Double,
        incrementKg: Double = 2.5,
        twoSetRamp: Boolean = false,
        targetRepLo: Int = 5
    ): List<WarmupSetTarget> {
        if (targetWeightKg <= 0.0) return emptyList()

        return if (twoSetRamp) {
            val weight1 = E1RMCalculator.roundTo(targetWeightKg * 0.50, incrementKg)
            val weight2 = E1RMCalculator.roundTo(targetWeightKg * 0.75, incrementKg)
            listOf(
                WarmupSetTarget(weightKg = maxOf(0.0, weight1), reps = targetRepLo, percentage = 50),
                WarmupSetTarget(weightKg = maxOf(0.0, weight2), reps = maxOf(2, targetRepLo - 2), percentage = 75)
            )
        } else {
            val weight = E1RMCalculator.roundTo(targetWeightKg * 0.60, incrementKg)
            listOf(
                WarmupSetTarget(weightKg = maxOf(0.0, weight), reps = targetRepLo, percentage = 60)
            )
        }
    }
}
