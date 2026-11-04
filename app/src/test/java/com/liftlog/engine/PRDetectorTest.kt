package com.liftlog.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PRDetectorTest {

    @Test
    fun testE1rmPRDetected() {
        val pastSets = listOf(
            SetData(weightKg = 80.0, reps = 5, rpe = 9.0), // e1rm = 96.0
            SetData(weightKg = 80.0, reps = 6, rpe = 9.0)  // e1rm = 98.7
        )

        val newSet = SetData(weightKg = 85.0, reps = 6, rpe = 9.0) // e1rm = 104.8
        val result = PRDetector.checkPR(newSet, pastSets)

        assertTrue(result.isE1rmPR)
        assertEquals(104.8, result.newE1rm!!, 0.1)
    }

    @Test
    fun testRepPRAtSameWeightDetected() {
        val pastSets = listOf(
            SetData(weightKg = 100.0, reps = 5, rpe = 9.0),
            SetData(weightKg = 100.0, reps = 6, rpe = 9.0)
        )

        val newSet = SetData(weightKg = 100.0, reps = 7, rpe = 9.0)
        val result = PRDetector.checkPR(newSet, pastSets)

        assertTrue(result.isRepPR)
        assertEquals(7, result.newReps)
        assertEquals(6, result.previousBestReps)
    }

    @Test
    fun testPainAndSwungSetsExcludedFromPR() {
        val pastSets = listOf(
            SetData(weightKg = 80.0, reps = 5, rpe = 9.0)
        )

        val painSet = SetData(weightKg = 120.0, reps = 10, flagPain = true)
        val swungSet = SetData(weightKg = 120.0, reps = 10, flagSwung = true)

        assertFalse(PRDetector.checkPR(painSet, pastSets).isAnyPR)
        assertFalse(PRDetector.checkPR(swungSet, pastSets).isAnyPR)
    }
}
