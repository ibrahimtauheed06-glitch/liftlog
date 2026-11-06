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

    /**
     * Generates a progressive multi-step warm-up ramp for any target working weight.
     * Can be tapped in-gym to quickly load ramping warmup sets into the input field.
     */
    fun generateWarmupRamp(
        targetWeightKg: Double,
        incrementKg: Double = 2.5,
        barWeight: Double = 20.0,
        isBarbell: Boolean = false
    ): List<WarmupSetTarget> {
        if (targetWeightKg <= 0.0) return emptyList()

        return if (isBarbell && targetWeightKg >= 40.0) {
            val step1Weight = minOf(targetWeightKg * 0.40, barWeight)
            val step2Weight = E1RMCalculator.roundTo(targetWeightKg * 0.60, incrementKg)
            val step3Weight = E1RMCalculator.roundTo(targetWeightKg * 0.80, incrementKg)
            listOf(
                WarmupSetTarget(weightKg = maxOf(0.0, step1Weight), reps = 10, percentage = ((step1Weight / targetWeightKg) * 100).toInt()),
                WarmupSetTarget(weightKg = maxOf(0.0, step2Weight), reps = 5, percentage = 60),
                WarmupSetTarget(weightKg = maxOf(0.0, step3Weight), reps = 2, percentage = 80)
            )
        } else {
            val step1Weight = E1RMCalculator.roundTo(targetWeightKg * 0.50, incrementKg)
            val step2Weight = E1RMCalculator.roundTo(targetWeightKg * 0.75, incrementKg)
            listOf(
                WarmupSetTarget(weightKg = maxOf(0.0, step1Weight), reps = 8, percentage = 50),
                WarmupSetTarget(weightKg = maxOf(0.0, step2Weight), reps = 4, percentage = 75)
            )
        }
    }
}
