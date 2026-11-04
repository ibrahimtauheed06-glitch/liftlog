package com.liftlog.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun testStandardBarbellLoad_sixtyKg() {
        // Target 60kg on 20kg bar -> 40kg plates needed -> 20kg per side (one 20kg plate per side)
        val result = PlateCalculator.calculateBarbellLoad(60.0, 20.0, listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25))
        assertEquals(60.0, result.loadedWeightKg, 0.01)
        assertEquals(0.0, result.remainderKg, 0.01)
        assertEquals(1, result.platesPerSide.size)
        assertEquals(20.0, result.platesPerSide[0].plateWeightKg, 0.01)
        assertEquals(1, result.platesPerSide[0].countPerSide)
    }

    @Test
    fun testBarbellLoad_complexPlates() {
        // Target 102.5kg on 20kg bar -> 82.5kg plates -> 41.25kg per side
        // Per side: 20x2 (40kg) + 1.25x1 (1.25kg) = 41.25kg
        val result = PlateCalculator.calculateBarbellLoad(102.5, 20.0, listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25))
        assertEquals(102.5, result.loadedWeightKg, 0.01)
        assertEquals(0.0, result.remainderKg, 0.01)

        val p20 = result.platesPerSide.find { it.plateWeightKg == 20.0 }
        val p1_25 = result.platesPerSide.find { it.plateWeightKg == 1.25 }
        assertEquals(2, p20?.countPerSide)
        assertEquals(1, p1_25?.countPerSide)
    }

    @Test
    fun testStackDisplayFormatting() {
        // plateKg=5 shows "3 plates (15kg)"
        val formatted = PlateCalculator.formatStackDisplay(15.0, 5.0)
        assertEquals("3 plates (15kg)", formatted)

        val formatted2 = PlateCalculator.formatStackDisplay(25.0, 5.0)
        assertEquals("5 plates (25kg)", formatted2)

        val standard = PlateCalculator.formatStackDisplay(80.0, null)
        assertEquals("80kg", standard)
    }
}
