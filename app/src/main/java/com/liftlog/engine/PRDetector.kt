package com.liftlog.engine

import kotlin.math.abs

data class PRResult(
    val isE1rmPR: Boolean = false,
    val previousBestE1rm: Double? = null,
    val newE1rm: Double? = null,
    val isRepPR: Boolean = false,
    val previousBestReps: Int? = null,
    val newReps: Int = 0
) {
    val isAnyPR: Boolean get() = isE1rmPR || isRepPR

    fun badgeText(): String? {
        val badges = mutableListOf<String>()
        if (isE1rmPR && newE1rm != null) {
            badges.add("e1RM PR: ${newE1rm}kg")
        }
        if (isRepPR) {
            badges.add("Rep PR: $newReps reps")
        }
        return if (badges.isNotEmpty()) badges.joinToString(" • ") else null
    }
}

object PRDetector {

    /**
     * Checks if a newly logged set is a PR compared to historical sets for that exercise.
     * Excludes pain and swung sets.
     */
    fun checkPR(
        newSet: SetData,
        historicalSets: List<SetData>,
        bwFactor: Double = 0.0,
        bodyweight7DayAvg: Double = 0.0
    ): PRResult {
        if (newSet.flagPain || newSet.flagSwung || newSet.type != "working") {
            return PRResult()
        }

        val validHistorical = historicalSets.filter { it.type == "working" && !it.flagPain && !it.flagSwung }

        // 1. e1RM PR check (only for reps <= 12)
        val newE1rm = E1RMCalculator.calculateE1RM(newSet.weightKg, newSet.reps, newSet.rpe, bwFactor, bodyweight7DayAvg)
        var isE1rmPR = false
        var previousBestE1rm: Double? = null

        if (newE1rm != null && newE1rm > 0.0) {
            val pastE1rms = validHistorical.mapNotNull {
                E1RMCalculator.calculateE1RM(it.weightKg, it.reps, it.rpe, bwFactor, bodyweight7DayAvg)
            }
            if (pastE1rms.isNotEmpty()) {
                previousBestE1rm = pastE1rms.maxOrNull()
                if (previousBestE1rm != null && newE1rm > previousBestE1rm) {
                    isE1rmPR = true
                }
            } else {
                isE1rmPR = true
            }
        }

        // 2. Rep PR at this specific weight
        val sameWeightSets = validHistorical.filter { abs(it.weightKg - newSet.weightKg) < 0.001 }
        var isRepPR = false
        var previousBestReps: Int? = null

        if (sameWeightSets.isNotEmpty()) {
            previousBestReps = sameWeightSets.maxOf { it.reps }
            if (newSet.reps > previousBestReps) {
                isRepPR = true
            }
        }

        return PRResult(
            isE1rmPR = isE1rmPR,
            previousBestE1rm = previousBestE1rm,
            newE1rm = newE1rm,
            isRepPR = isRepPR,
            previousBestReps = previousBestReps,
            newReps = newSet.reps
        )
    }
}
