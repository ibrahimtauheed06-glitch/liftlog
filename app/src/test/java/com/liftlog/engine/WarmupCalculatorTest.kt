package com.liftlog.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class WarmupCalculatorTest {

    @Test
    fun testFirstCompoundGetsSingleWarmupSetAtSixtyPercent() {
        // Target weight 100kg -> 60% = 60kg
        val warmups = WarmupCalculator.calculateWarmupSets(100.0, incrementKg = 2.5, twoSetRamp = false, targetRepLo = 5)
        assertEquals(1, warmups.size)
        assertEquals(60.0, warmups[0].weightKg, 0.01)
        assertEquals(5, warmups[0].reps)
        assertEquals(60, warmups[0].percentage)
    }

    @Test
    fun testTwoSetRampOption() {
        // Target weight 100kg -> 50% = 50kg, 75% = 75kg
        val warmups = WarmupCalculator.calculateWarmupSets(100.0, incrementKg = 2.5, twoSetRamp = true, targetRepLo = 6)
        assertEquals(2, warmups.size)
        assertEquals(50.0, warmups[0].weightKg, 0.01)
        assertEquals(6, warmups[0].reps)
        assertEquals(50, warmups[0].percentage)

        assertEquals(75.0, warmups[1].weightKg, 0.01)
        assertEquals(4, warmups[1].reps)
        assertEquals(75, warmups[1].percentage)
    }
}
