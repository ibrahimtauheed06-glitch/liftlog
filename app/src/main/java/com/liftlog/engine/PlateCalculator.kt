package com.liftlog.engine

import kotlin.math.abs

data class PlateCount(
    val plateWeightKg: Double,
    val countPerSide: Int
)

data class BarbellLoadResult(
    val barWeightKg: Double,
    val targetWeightKg: Double,
    val platesPerSide: List<PlateCount>,
    val loadedWeightKg: Double,
    val remainderKg: Double
)

object PlateCalculator {

    /**
     * Calculates plate configuration per side for a barbell lift.
     */
    fun calculateBarbellLoad(
        targetWeightKg: Double,
        barWeightKg: Double = 20.0,
        availablePlates: List<Double> = listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    ): BarbellLoadResult {
        if (targetWeightKg <= barWeightKg) {
            return BarbellLoadResult(
                barWeightKg = barWeightKg,
                targetWeightKg = targetWeightKg,
                platesPerSide = emptyList(),
                loadedWeightKg = barWeightKg,
                remainderKg = maxOf(0.0, targetWeightKg - barWeightKg)
            )
        }

        var weightNeededPerSide = (targetWeightKg - barWeightKg) / 2.0
        val sortedPlates = availablePlates.filter { it > 0.0 }.sortedDescending()
        val resultPlates = mutableListOf<PlateCount>()

        for (plate in sortedPlates) {
            if (weightNeededPerSide >= plate - 0.001) {
                val count = (weightNeededPerSide / plate).toInt()
                if (count > 0) {
                    resultPlates.add(PlateCount(plateWeightKg = plate, countPerSide = count))
                    weightNeededPerSide -= (count * plate)
                }
            }
        }

        val totalPlatesWeightPerSide = resultPlates.sumOf { it.plateWeightKg * it.countPerSide }
        val loadedTotal = barWeightKg + (2 * totalPlatesWeightPerSide)
        val remainder = targetWeightKg - loadedTotal

        return BarbellLoadResult(
            barWeightKg = barWeightKg,
            targetWeightKg = targetWeightKg,
            platesPerSide = resultPlates,
            loadedWeightKg = loadedTotal,
            remainderKg = if (abs(remainder) < 0.01) 0.0 else remainder
        )
    }

    /**
     * Formats weight display for pin stack or plate machines.
     * E.g. plateKg = 5.0, weight = 15.0 -> "3 plates (15kg)"
     */
    fun formatStackDisplay(weightKg: Double, plateKg: Double?): String {
        if (plateKg != null && plateKg > 0.0) {
            val plates = (weightKg / plateKg).toInt()
            val cleanWeight = if (weightKg % 1.0 == 0.0) weightKg.toInt().toString() else weightKg.toString()
            return "$plates plates (${cleanWeight}kg)"
        }
        val clean = if (weightKg % 1.0 == 0.0) weightKg.toInt().toString() else weightKg.toString()
        return "${clean}kg"
    }
}
