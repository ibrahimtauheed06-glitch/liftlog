package com.liftlog.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class E1RMCalculatorTest {

    @Test
    fun testStandardFormulaWithExplicitRPE() {
        // load = 100, reps = 5, rpe = 9
        // e1RM = 100 * (1 + (5 + (10 - 9)) / 30) = 100 * (1 + 6/30) = 100 * 1.2 = 120.0
        val e1rm = E1RMCalculator.calculateE1RM(100.0, 5, 9.0)
        assertEquals(120.0, e1rm!!, 0.01)
    }

    @Test
    fun testNullRpeDefaultsToNine() {
        // load = 100, reps = 5, rpe = null -> default 9 -> e1RM = 120.0
        val e1rm = E1RMCalculator.calculateE1RM(100.0, 5, null)
        assertEquals(120.0, e1rm!!, 0.01)
    }

    @Test
    fun testRpeTen() {
        // load = 100, reps = 10, rpe = 10
        // e1RM = 100 * (1 + (10 + 0) / 30) = 100 * 1.3333... = 133.3
        val e1rm = E1RMCalculator.calculateE1RM(100.0, 10, 10.0)
        assertEquals(133.3, e1rm!!, 0.1)
    }

    @Test
    fun testRepsAboveTwelveReturnsNull() {
        // High reps (> 12) must return null to compare weight and reps directly
        val e1rm = E1RMCalculator.calculateE1RM(60.0, 13, 9.0)
        assertNull(e1rm)

        val e1rm20 = E1RMCalculator.calculateE1RM(50.0, 20, 8.0)
        assertNull(e1rm20)
    }

    @Test
    fun testBodyweightFactor() {
        // Added weight = 20kg, bwFactor = 1.0, 7-day avg bw = 80kg -> load = 100kg
        // reps = 5, rpe = 9 -> e1RM = 120.0
        val e1rm = E1RMCalculator.calculateE1RM(
            weightKg = 20.0,
            reps = 5,
            rpe = 9.0,
            bwFactor = 1.0,
            bodyweight7DayAvg = 80.0
        )
        assertEquals(120.0, e1rm!!, 0.01)
    }

    @Test
    fun testRoundingHelper() {
        assertEquals(82.5, E1RMCalculator.roundTo(81.7, 2.5), 0.01)
        assertEquals(80.0, E1RMCalculator.roundTo(81.2, 2.5), 0.01)
        assertEquals(85.0, E1RMCalculator.roundTo(84.0, 5.0), 0.01)
        assertEquals(80.0, E1RMCalculator.roundTo(82.0, 5.0), 0.01)
    }
}
