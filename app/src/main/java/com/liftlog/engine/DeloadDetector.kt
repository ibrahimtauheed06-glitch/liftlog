package com.liftlog.engine

object DeloadDetector {

    /**
     * Rule 4: Stall: no new best e1RM in the last 3 sessions of an exercise.
     * Sessions must be ordered descending by timestamp (newest first).
     * If an exercise has at least 3 sessions, and the max e1RM in the latest 3 sessions
     * did not exceed the e1RM achieved in or before those sessions,
     * it is considered stalled.
     */
    fun isExerciseStalled(
        sessions: List<SessionData>,
        bwFactor: Double = 0.0,
        bodyweight7DayAvg: Double = 0.0
    ): Boolean {
        if (sessions.size < 3) return false
        val last3 = sessions.take(3)
        val e1rms = last3.map { session ->
            session.sets
                .filter { it.type == "working" && !it.flagSwung && !it.flagPain }
                .mapNotNull { E1RMCalculator.calculateE1RM(it.weightKg, it.reps, it.rpe, bwFactor, bodyweight7DayAvg) }
                .maxOrNull() ?: 0.0
        }
        if (e1rms.any { it <= 0.0 }) return false

        val latest = e1rms[0]
        val prev1 = e1rms[1]
        val prev2 = e1rms[2]

        return latest <= prev1 && prev1 <= prev2
    }

    /**
     * Rule 5: Deload check.
     * If >= 3 exercises are stalled within 14 days, suggest a deload banner.
     * exercisesWithSessions: Map of exerciseId to list of sessions.
     */
    fun shouldSuggestDeload(
        exercisesWithSessions: Map<Long, List<SessionData>>,
        currentTimestamp: Long = System.currentTimeMillis(),
        lastDismissTimestamp: Long = 0L,
        bwFactors: Map<Long, Double> = emptyMap(),
        bodyweight7DayAvg: Double = 0.0
    ): Boolean {
        val fourteenDaysMs = 14L * 24 * 60 * 60 * 1000
        if (currentTimestamp - lastDismissTimestamp < fourteenDaysMs) {
            return false
        }

        var stalledCount = 0
        for ((exerciseId, sessions) in exercisesWithSessions) {
            val recentSession = sessions.firstOrNull() ?: continue
            if (currentTimestamp - recentSession.timestamp <= fourteenDaysMs) {
                val bwFactor = bwFactors[exerciseId] ?: 0.0
                if (isExerciseStalled(sessions, bwFactor, bodyweight7DayAvg)) {
                    stalledCount++
                }
            }
        }
        return stalledCount >= 3
    }

    /**
     * Rule 6: 2+ pain flags on one exercise in 14 days -> warning chip "consider lowering load or swapping"
     */
    fun hasPainWarning(
        sessions: List<SessionData>,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Boolean {
        val fourteenDaysMs = 14L * 24 * 60 * 60 * 1000
        val painSetsCount = sessions
            .filter { currentTimestamp - it.timestamp <= fourteenDaysMs }
            .flatMap { it.sets }
            .count { it.flagPain }
        return painSetsCount >= 2
    }
}
