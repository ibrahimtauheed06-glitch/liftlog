package com.liftlog.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    private val benchPress = ExerciseConfig(
        id = 1L,
        name = "Bench Press",
        equipment = "barbell",
        incrementKg = 2.5,
        repLo = 5,
        repHi = 8,
        defaultSets = 3,
        restSec = 150,
        isCompound = true
    )

    @Test
    fun testDoubleProgression_allHitRepHi_advancesWeight() {
        val now = System.currentTimeMillis()
        val session = SessionData(
            timestamp = now - (3L * 24 * 60 * 60 * 1000), // 3 days ago
            sets = listOf(
                SetData(weightKg = 80.0, reps = 8, type = "working"),
                SetData(weightKg = 80.0, reps = 8, type = "working"),
                SetData(weightKg = 80.0, reps = 8, type = "working")
            )
        )

        val next = ProgressionEngine.computeNextTarget(
            exercise = benchPress,
            sessions = listOf(session),
            currentTimestamp = now
        )

        assertEquals(82.5, next.targetWeightKg, 0.01)
        assertEquals(5, next.targetReps)
        assertEquals(3, next.setsCount)
    }

    @Test
    fun testDoubleProgression_notAllHitRepHi_targetsPlusOneRepWeakestSet() {
        val now = System.currentTimeMillis()
        val session = SessionData(
            timestamp = now - (3L * 24 * 60 * 60 * 1000),
            sets = listOf(
                SetData(weightKg = 80.0, reps = 8, type = "working"),
                SetData(weightKg = 80.0, reps = 7, type = "working"),
                SetData(weightKg = 80.0, reps = 6, type = "working")
            )
        )

        val next = ProgressionEngine.computeNextTarget(
            exercise = benchPress,
            sessions = listOf(session),
            currentTimestamp = now
        )

        assertEquals(80.0, next.targetWeightKg, 0.01)
        // weakest set is 6 reps, so target is 6 + 1 = 7 reps
        assertEquals(7, next.targetReps)
    }

    @Test
    fun testDoubleProgression_backOffAfterTwoMissedSessions() {
        val now = System.currentTimeMillis()
        val session1 = SessionData(
            timestamp = now - (3L * 24 * 60 * 60 * 1000),
            sets = listOf(
                SetData(weightKg = 100.0, reps = 4, type = "working"), // below repLo 5
                SetData(weightKg = 100.0, reps = 3, type = "working")
            )
        )
        val session2 = SessionData(
            timestamp = now - (7L * 24 * 60 * 60 * 1000),
            sets = listOf(
                SetData(weightKg = 100.0, reps = 4, type = "working"), // below repLo 5
                SetData(weightKg = 100.0, reps = 4, type = "working")
            )
        )

        val next = ProgressionEngine.computeNextTarget(
            exercise = benchPress,
            sessions = listOf(session1, session2),
            currentTimestamp = now
        )

        // 100 * 0.925 = 92.5
        assertEquals(92.5, next.targetWeightKg, 0.01)
        assertEquals(5, next.targetReps)
        assertTrue(next.isBackOff)
    }

    @Test
    fun testRepsOnlyExercise_extendsRepHi() {
        val legRaise = ExerciseConfig(
            id = 2L,
            name = "Hanging Leg Raise",
            equipment = "bodyweight",
            incrementKg = 0.0,
            repLo = 8,
            repHi = 15,
            defaultSets = 2,
            bwFactor = 0.0
        )
        val now = System.currentTimeMillis()
        val session = SessionData(
            timestamp = now - (2L * 24 * 60 * 60 * 1000),
            sets = listOf(
                SetData(weightKg = 0.0, reps = 15, type = "working"),
                SetData(weightKg = 0.0, reps = 15, type = "working")
            )
        )

        val next = ProgressionEngine.computeNextTarget(
            exercise = legRaise,
            sessions = listOf(session),
            currentTimestamp = now
        )

        assertEquals(0.0, next.targetWeightKg, 0.01)
        assertEquals(17, next.targetReps) // repHi + 2
    }

    @Test
    fun testGap_tenToTwentyDays_usesNinetyFivePercent() {
        val now = System.currentTimeMillis()
        val session = SessionData(
            timestamp = now - (14L * 24 * 60 * 60 * 1000), // 14 days ago
            sets = listOf(
                SetData(weightKg = 100.0, reps = 8, type = "working"),
                SetData(weightKg = 100.0, reps = 8, type = "working")
            )
        )

        val next = ProgressionEngine.computeNextTarget(
            exercise = benchPress,
            sessions = listOf(session),
            currentTimestamp = now
        )

        assertEquals(95.0, next.targetWeightKg, 0.01)
        assertEquals(5, next.targetReps)
        assertEquals(14L, next.gapDays)
    }

    @Test
    fun testGap_twentyOnePlusDays_usesNinetyPercent() {
        val now = System.currentTimeMillis()
        val session = SessionData(
            timestamp = now - (25L * 24 * 60 * 60 * 1000), // 25 days ago
            sets = listOf(
                SetData(weightKg = 100.0, reps = 8, type = "working")
            )
        )

        val next = ProgressionEngine.computeNextTarget(
            exercise = benchPress,
            sessions = listOf(session),
            currentTimestamp = now
        )

        assertEquals(90.0, next.targetWeightKg, 0.01)
        assertEquals(5, next.targetReps)
        assertEquals(25L, next.gapDays)
    }

    @Test
    fun testDeloadActive_reducesWeightAndSets() {
        val now = System.currentTimeMillis()
        val session = SessionData(
            timestamp = now - (2L * 24 * 60 * 60 * 1000),
            sets = listOf(
                SetData(weightKg = 100.0, reps = 8, type = "working"),
                SetData(weightKg = 100.0, reps = 8, type = "working"),
                SetData(weightKg = 100.0, reps = 8, type = "working")
            )
        )

        // Normally 100 -> 102.5. Deload applies 90% -> 102.5 * 0.9 = 92.25 -> rounded to 92.5. Sets = 3 - 1 = 2
        val next = ProgressionEngine.computeNextTarget(
            exercise = benchPress,
            sessions = listOf(session),
            isDeloadActive = true,
            currentTimestamp = now
        )

        assertEquals(92.5, next.targetWeightKg, 0.01)
        assertEquals(2, next.setsCount)
        assertTrue(next.isDeload)
    }

    @Test
    fun testHomePhase_neverExceedsCap_pushesReps() {
        val dumbbellCurl = ExerciseConfig(
            id = 3L,
            name = "Pad Curl",
            equipment = "dumbbell",
            incrementKg = 2.5,
            repLo = 8,
            repHi = 12,
            defaultSets = 2,
            homeCapKg = 10.0 // capped at 10kg
        )
        val now = System.currentTimeMillis()
        val session = SessionData(
            timestamp = now - (2L * 24 * 60 * 60 * 1000),
            sets = listOf(
                SetData(weightKg = 10.0, reps = 12, type = "working"),
                SetData(weightKg = 10.0, reps = 12, type = "working")
            )
        )

        val next = ProgressionEngine.computeNextTarget(
            exercise = dumbbellCurl,
            sessions = listOf(session),
            currentPhase = WorkoutPhase.HOME,
            currentTimestamp = now
        )

        // Capped at 10kg, reps pushed from 12 -> 13
        assertEquals(10.0, next.targetWeightKg, 0.01)
        assertEquals(13, next.targetReps)
    }

    @Test
    fun testHomePhase_thirtyRepsShowsHint() {
        val dumbbellCurl = ExerciseConfig(
            id = 3L,
            name = "Pad Curl",
            equipment = "dumbbell",
            incrementKg = 2.5,
            repLo = 8,
            repHi = 12,
            defaultSets = 2,
            homeCapKg = 10.0
        )
        val now = System.currentTimeMillis()
        val session = SessionData(
            timestamp = now - (2L * 24 * 60 * 60 * 1000),
            sets = listOf(
                SetData(weightKg = 10.0, reps = 30, type = "working")
            )
        )

        val next = ProgressionEngine.computeNextTarget(
            exercise = dumbbellCurl,
            sessions = listOf(session),
            currentPhase = WorkoutPhase.HOME,
            currentTimestamp = now
        )

        assertEquals(30, next.targetReps)
        assertNotNull(next.homeHint)
        assertEquals("3s lowering / single-arm / harder variation", next.homeHint)
    }

    @Test
    fun testReturnToGymUsesNinetyPercent() {
        val next = ProgressionEngine.computeNextTarget(
            exercise = benchPress,
            sessions = emptyList(),
            currentPhase = WorkoutPhase.GYM,
            returnToGymLastWeight = 100.0
        )

        assertEquals(90.0, next.targetWeightKg, 0.01)
        assertEquals(5, next.targetReps)
    }
}
