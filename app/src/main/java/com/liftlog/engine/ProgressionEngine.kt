package com.liftlog.engine

enum class WorkoutPhase {
    GYM,
    HOME
}

data class SetData(
    val weightKg: Double,
    val reps: Int,
    val type: String = "working",
    val rpe: Double? = null,
    val flagSwung: Boolean = false,
    val flagPain: Boolean = false,
    val timestamp: Long = 0L
)

data class SessionData(
    val timestamp: Long,
    val sets: List<SetData>
)

data class ExerciseConfig(
    val id: Long = 0L,
    val name: String = "",
    val equipment: String = "barbell",
    val incrementKg: Double = 2.5,
    val plateKg: Double? = null,
    val repLo: Int = 5,
    val repHi: Int = 8,
    val defaultSets: Int = 3,
    val restSec: Int = 150,
    val isCompound: Boolean = true,
    val bwFactor: Double = 0.0,
    val homeCapKg: Double? = null
)

data class NextTargetResult(
    val targetWeightKg: Double,
    val targetReps: Int,
    val setsCount: Int,
    val reason: String,
    val gapDays: Long? = null,
    val isDeload: Boolean = false,
    val isBackOff: Boolean = false,
    val homeHint: String? = null
)

object ProgressionEngine {

    /**
     * Determines the top set of a session.
     * "top set" = highest-e1RM working set with no pain/swung flag.
     * For reps > 12: compare weight and reps directly.
     */
    fun findTopSet(
        sets: List<SetData>,
        bwFactor: Double = 0.0,
        bodyweight7DayAvg: Double = 0.0
    ): SetData? {
        val validWorkingSets = sets.filter { it.type == "working" && !it.flagSwung && !it.flagPain }
        if (validWorkingSets.isEmpty()) return null

        return validWorkingSets.maxWithOrNull { a, b ->
            val e1rmA = E1RMCalculator.calculateE1RM(a.weightKg, a.reps, a.rpe, bwFactor, bodyweight7DayAvg)
            val e1rmB = E1RMCalculator.calculateE1RM(b.weightKg, b.reps, b.rpe, bwFactor, bodyweight7DayAvg)

            if (e1rmA != null && e1rmB != null) {
                e1rmA.compareTo(e1rmB)
            } else {
                val weightCompare = a.weightKg.compareTo(b.weightKg)
                if (weightCompare != 0) weightCompare else a.reps.compareTo(b.reps)
            }
        }
    }

    /**
     * Computes the next target (weight, reps, sets) according to rules 2, 3, 5, 9.
     */
    fun computeNextTarget(
        exercise: ExerciseConfig,
        sessions: List<SessionData>, // ordered desc by timestamp (newest first)
        currentPhase: WorkoutPhase = WorkoutPhase.GYM,
        isDeloadActive: Boolean = false,
        currentTimestamp: Long = System.currentTimeMillis(),
        returnToGymLastWeight: Double? = null,
        bodyweight7DayAvg: Double = 0.0
    ): NextTargetResult {
        val lastSession = sessions.firstOrNull()
        val defaultSetsCount = if (isDeloadActive) maxOf(1, exercise.defaultSets - 1) else exercise.defaultSets

        // If switching back from HOME to GYM via "Return to gym"
        if (returnToGymLastWeight != null && currentPhase == WorkoutPhase.GYM) {
            val weight = E1RMCalculator.roundTo(returnToGymLastWeight * 0.90, exercise.incrementKg)
            val finalWeight = if (isDeloadActive) E1RMCalculator.roundTo(weight * 0.90, exercise.incrementKg) else weight
            return NextTargetResult(
                targetWeightKg = maxOf(0.0, finalWeight),
                targetReps = exercise.repLo,
                setsCount = defaultSetsCount,
                reason = "Return to gym (90% of last gym weight)",
                isDeload = isDeloadActive
            )
        }

        if (lastSession == null) {
            // First time ever performing this exercise
            return NextTargetResult(
                targetWeightKg = 0.0,
                targetReps = exercise.repLo,
                setsCount = defaultSetsCount,
                reason = "Initial session: starting at base load",
                isDeload = isDeloadActive
            )
        }

        val validWorkingSets = lastSession.sets.filter { it.type == "working" && !it.flagSwung && !it.flagPain }
        if (validWorkingSets.isEmpty()) {
            val fallbackWeight = lastSession.sets.firstOrNull()?.weightKg ?: 0.0
            return NextTargetResult(
                targetWeightKg = fallbackWeight,
                targetReps = exercise.repLo,
                setsCount = defaultSetsCount,
                reason = "Previous sets flagged: repeating target",
                isDeload = isDeloadActive
            )
        }

        val lastWorkingWeight = validWorkingSets.first().weightKg
        val daysSinceLast = ((currentTimestamp - lastSession.timestamp) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)

        // Rule 3: Gap detection (10-20 days -> 95%, 21+ days -> 90%)
        if (daysSinceLast >= 10) {
            val factor = if (daysSinceLast in 10..20) 0.95 else 0.90
            var reducedWeight = E1RMCalculator.roundTo(lastWorkingWeight * factor, exercise.incrementKg)
            if (isDeloadActive) {
                reducedWeight = E1RMCalculator.roundTo(reducedWeight * 0.90, exercise.incrementKg)
            }
            return NextTargetResult(
                targetWeightKg = maxOf(0.0, reducedWeight),
                targetReps = exercise.repLo,
                setsCount = defaultSetsCount,
                reason = "Return after $daysSinceLast days (${(factor * 100).toInt()}% weight)",
                gapDays = daysSinceLast,
                isDeload = isDeloadActive
            )
        }

        // Rule 2(c): Top set below repLo in 2 consecutive sessions -> suggest -7.5% "Back off"
        if (sessions.size >= 2) {
            val topSet1 = findTopSet(sessions[0].sets, exercise.bwFactor, bodyweight7DayAvg)
            val topSet2 = findTopSet(sessions[1].sets, exercise.bwFactor, bodyweight7DayAvg)
            if (topSet1 != null && topSet2 != null && topSet1.reps < exercise.repLo && topSet2.reps < exercise.repLo) {
                var backOffWeight = E1RMCalculator.roundTo(lastWorkingWeight * 0.925, exercise.incrementKg)
                if (isDeloadActive) {
                    backOffWeight = E1RMCalculator.roundTo(backOffWeight * 0.90, exercise.incrementKg)
                }
                return NextTargetResult(
                    targetWeightKg = maxOf(0.0, backOffWeight),
                    targetReps = exercise.repLo,
                    setsCount = defaultSetsCount,
                    reason = "Back off (-7.5%): top set below repLo in 2 consecutive sessions",
                    isDeload = isDeloadActive,
                    isBackOff = true
                )
            }
        }

        // Rule 2(a) & 2(d): All working sets hit repHi
        val allHitRepHi = validWorkingSets.all { it.reps >= exercise.repHi }

        if (allHitRepHi) {
            // Reps-only exercise (incrementKg == 0)
            if (exercise.incrementKg <= 0.0) {
                return NextTargetResult(
                    targetWeightKg = lastWorkingWeight,
                    targetReps = exercise.repHi + 2,
                    setsCount = defaultSetsCount,
                    reason = "All sets hit repHi: extended rep ceiling to ${exercise.repHi + 2}",
                    isDeload = isDeloadActive
                )
            }

            // Phase HOME handling
            if (currentPhase == WorkoutPhase.HOME) {
                val effectiveCap = exercise.homeCapKg ?: if (exercise.equipment.equals("dumbbell", ignoreCase = true)) 10.0 else null
                val proposedWeight = lastWorkingWeight + exercise.incrementKg

                if (effectiveCap != null && proposedWeight > effectiveCap) {
                    // Cannot increase weight beyond cap! Push reps up to 30
                    val maxReps = validWorkingSets.maxOf { it.reps }
                    val nextReps = minOf(30, maxReps + 1)
                    val hint = if (nextReps >= 30) "3s lowering / single-arm / harder variation" else null
                    val finalWeight = if (isDeloadActive) E1RMCalculator.roundTo(minOf(effectiveCap, lastWorkingWeight) * 0.90, exercise.incrementKg) else minOf(effectiveCap, lastWorkingWeight)

                    return NextTargetResult(
                        targetWeightKg = finalWeight,
                        targetReps = nextReps,
                        setsCount = defaultSetsCount,
                        reason = "Home cap reached (${effectiveCap}kg): pushing reps ($nextReps/30)",
                        isDeload = isDeloadActive,
                        homeHint = hint
                    )
                }
            }

            // Normal progression: weight + incrementKg, target repLo
            var newWeight = lastWorkingWeight + exercise.incrementKg
            if (isDeloadActive) {
                newWeight = E1RMCalculator.roundTo(newWeight * 0.90, exercise.incrementKg)
            }
            return NextTargetResult(
                targetWeightKg = newWeight,
                targetReps = exercise.repLo,
                setsCount = defaultSetsCount,
                reason = "All sets hit repHi: progressing weight to ${newWeight}kg",
                isDeload = isDeloadActive
            )
        } else {
            // Rule 2(b): Same weight, target +1 rep on the weakest set
            val weakestReps = validWorkingSets.minOf { it.reps }
            val targetReps = minOf(exercise.repHi, weakestReps + 1)
            var finalWeight = lastWorkingWeight
            if (isDeloadActive) {
                finalWeight = E1RMCalculator.roundTo(finalWeight * 0.90, exercise.incrementKg)
            }

            return NextTargetResult(
                targetWeightKg = finalWeight,
                targetReps = targetReps,
                setsCount = defaultSetsCount,
                reason = "Target +1 rep on weakest set ($weakestReps -> $targetReps)",
                isDeload = isDeloadActive
            )
        }
    }
}
