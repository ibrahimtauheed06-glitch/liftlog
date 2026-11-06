package com.liftlog.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeloadDetectorTest {

    @Test
    fun testStall_noImprovementAcross3Sessions() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        // 3 sessions where e1RM drops or stays equal
        // session 2 (oldest): 100kg x 6 -> e1rm = 120.0
        // session 1: 100kg x 6 -> e1rm = 120.0
        // session 0 (newest): 100kg x 5 -> e1rm = 116.7
        val s0 = SessionData(now - (1 * day), listOf(SetData(100.0, 5, "working")))
        val s1 = SessionData(now - (4 * day), listOf(SetData(100.0, 6, "working")))
        val s2 = SessionData(now - (8 * day), listOf(SetData(100.0, 6, "working")))

        val isStalled = DeloadDetector.isExerciseStalled(listOf(s0, s1, s2))
        assertTrue(isStalled)
    }

    @Test
    fun testNotStalled_whenImproving() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        // session 0 improved to 7 reps
        val s0 = SessionData(now - (1 * day), listOf(SetData(100.0, 7, "working")))
        val s1 = SessionData(now - (4 * day), listOf(SetData(100.0, 6, "working")))
        val s2 = SessionData(now - (8 * day), listOf(SetData(100.0, 5, "working")))

        val isStalled = DeloadDetector.isExerciseStalled(listOf(s0, s1, s2))
        assertFalse(isStalled)
    }

    @Test
    fun testStall_loadZero_repsDoNotImproveAcross3Sessions() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        // Bodyweight exercise (bwFactor = 0, weightKg = 0) e.g. Decline Crunch
        // Session 2 (oldest): 0kg x 15 reps
        // Session 1: 0kg x 15 reps
        // Session 0 (latest): 0kg x 14 reps
        val s0 = SessionData(now - (1 * day), listOf(SetData(0.0, 14, "working")))
        val s1 = SessionData(now - (4 * day), listOf(SetData(0.0, 15, "working")))
        val s2 = SessionData(now - (8 * day), listOf(SetData(0.0, 15, "working")))

        val isStalled = DeloadDetector.isExerciseStalled(listOf(s0, s1, s2), bwFactor = 0.0)
        assertTrue(isStalled)
    }

    @Test
    fun testNotStalled_loadZero_whenRepsImprove() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        // Session 0 improved to 16 reps
        val s0 = SessionData(now - (1 * day), listOf(SetData(0.0, 16, "working")))
        val s1 = SessionData(now - (4 * day), listOf(SetData(0.0, 15, "working")))
        val s2 = SessionData(now - (8 * day), listOf(SetData(0.0, 14, "working")))

        val isStalled = DeloadDetector.isExerciseStalled(listOf(s0, s1, s2), bwFactor = 0.0)
        assertFalse(isStalled)
    }

    @Test
    fun testDeloadSuggestion_threeExercisesStalledWithin14Days() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        fun stalledHistory(): List<SessionData> = listOf(
            SessionData(now - (1 * day), listOf(SetData(80.0, 5, "working"))),
            SessionData(now - (4 * day), listOf(SetData(80.0, 6, "working"))),
            SessionData(now - (8 * day), listOf(SetData(80.0, 6, "working")))
        )

        val map = mapOf(
            1L to stalledHistory(),
            2L to stalledHistory(),
            3L to stalledHistory()
        )

        val shouldDeload = DeloadDetector.shouldSuggestDeload(map, currentTimestamp = now)
        assertTrue(shouldDeload)
    }

    @Test
    fun testDeloadDismiss_snoozesFor14Days() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        fun stalledHistory(): List<SessionData> = listOf(
            SessionData(now - (1 * day), listOf(SetData(80.0, 5, "working"))),
            SessionData(now - (4 * day), listOf(SetData(80.0, 6, "working"))),
            SessionData(now - (8 * day), listOf(SetData(80.0, 6, "working")))
        )

        val map = mapOf(
            1L to stalledHistory(),
            2L to stalledHistory(),
            3L to stalledHistory()
        )

        // Dismissed 5 days ago -> snoozed
        val shouldDeload = DeloadDetector.shouldSuggestDeload(
            map,
            currentTimestamp = now,
            lastDismissTimestamp = now - (5 * day)
        )
        assertFalse(shouldDeload)
    }

    @Test
    fun testPainWarning_twoPainSetsIn14Days() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        val sessions = listOf(
            SessionData(now - (2 * day), listOf(SetData(80.0, 5, "working", flagPain = true))),
            SessionData(now - (6 * day), listOf(SetData(80.0, 6, "working", flagPain = true)))
        )

        assertTrue(DeloadDetector.hasPainWarning(sessions, now))
    }

    @Test
    fun testPainWarning_onePainSet_noWarning() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        val sessions = listOf(
            SessionData(now - (2 * day), listOf(SetData(80.0, 5, "working", flagPain = true))),
            SessionData(now - (6 * day), listOf(SetData(80.0, 6, "working", flagPain = false)))
        )

        assertFalse(DeloadDetector.hasPainWarning(sessions, now))
    }
}
