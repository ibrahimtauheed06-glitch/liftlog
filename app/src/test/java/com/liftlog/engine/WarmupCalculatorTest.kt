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

    @Test
    fun testGenerateWarmupRamp_barbellHeavy() {
        val ramp = WarmupCalculator.generateWarmupRamp(
            targetWeightKg = 100.0,
            incrementKg = 2.5,
            barWeight = 20.0,
            isBarbell = true
        )
        assertEquals(3, ramp.size)
        // Step 1: bar (20kg x 10)
        assertEquals(20.0, ramp[0].weightKg, 0.01)
        assertEquals(10, ramp[0].reps)
        // Step 2: 60% (60kg x 5)
        assertEquals(60.0, ramp[1].weightKg, 0.01)
        assertEquals(5, ramp[1].reps)
        // Step 3: 80% (80kg x 2)
        assertEquals(80.0, ramp[2].weightKg, 0.01)
        assertEquals(2, ramp[2].reps)
    }

    @Test
    fun testGenerateWarmupRamp_dumbbellOrLight() {
        val ramp = WarmupCalculator.generateWarmupRamp(
            targetWeightKg = 30.0,
            incrementKg = 2.5,
            isBarbell = false
        )
        assertEquals(2, ramp.size)
        // Step 1: 50% (15kg x 8)
        assertEquals(15.0, ramp[0].weightKg, 0.01)
        assertEquals(8, ramp[0].reps)
        // Step 2: 75% (22.5kg x 4)
        assertEquals(22.5, ramp[1].weightKg, 0.01)
        assertEquals(4, ramp[1].reps)
    }

    @Test
    fun testGenerateWarmupRamp_zeroWeightReturnsEmpty() {
        val ramp = WarmupCalculator.generateWarmupRamp(0.0)
        assertEquals(0, ramp.size)
    }
}
