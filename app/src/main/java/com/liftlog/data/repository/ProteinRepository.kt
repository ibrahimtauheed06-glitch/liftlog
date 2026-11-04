package com.liftlog.data.repository

import com.liftlog.data.db.ProteinDao
import com.liftlog.data.entity.ProteinEntry
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class ProteinDayStats(
    val todayEntries: List<ProteinEntry>,
    val todayTotalGrams: Double,
    val targetGrams: Double,
    val remainingGrams: Double,
    val progressFraction: Float,
    val sevenDayAverageGrams: Double,
    val daysHitTargetCount: Int, // X/7
    val daysInWindow: Int = 7
)

class ProteinRepository(
    private val proteinDao: ProteinDao,
    private val settingsRepository: SettingsRepository
) {

    fun getAllEntries(): Flow<List<ProteinEntry>> = proteinDao.getAllEntries()

    suspend fun addEntry(grams: Double, timestamp: Long = System.currentTimeMillis()): Long {
        return proteinDao.insert(ProteinEntry(grams = grams, ts = timestamp))
    }

    suspend fun updateEntry(entry: ProteinEntry) {
        proteinDao.update(entry)
    }

    suspend fun deleteEntry(id: Long) {
        proteinDao.delete(id)
    }

    suspend fun getTodayStats(
        now: Long = System.currentTimeMillis(),
        dayRolloverHour: Int = 4,
        targetGrams: Double = 110.0
    ): ProteinDayStats {
        val (startOfToday, endOfToday) = getDayWindow(now, dayRolloverHour)
        val todayEntries = proteinDao.getEntriesBetweenSync(startOfToday, endOfToday)
        val todayTotal = todayEntries.sumOf { it.grams }
        val remaining = maxOf(0.0, targetGrams - todayTotal)
        val progress = if (targetGrams > 0) (todayTotal / targetGrams).coerceIn(0.0, 1.0).toFloat() else 0f

        // 7-day stats (last 7 rolling days including today)
        var totalInPast7Days = 0.0
        var daysHit = 0
        for (i in 0 until 7) {
            val dayTs = now - (i * 24L * 60 * 60 * 1000)
            val (dayStart, dayEnd) = getDayWindow(dayTs, dayRolloverHour)
            val entriesForDay = proteinDao.getEntriesBetweenSync(dayStart, dayEnd)
            val daySum = entriesForDay.sumOf { it.grams }
            totalInPast7Days += daySum
            if (daySum >= targetGrams) {
                daysHit++
            }
        }
        val avg7Day = totalInPast7Days / 7.0

        return ProteinDayStats(
            todayEntries = todayEntries,
            todayTotalGrams = todayTotal,
            targetGrams = targetGrams,
            remainingGrams = remaining,
            progressFraction = progress,
            sevenDayAverageGrams = avg7Day,
            daysHitTargetCount = daysHit,
            daysInWindow = 7
        )
    }

    companion object {
        fun getDayWindow(timestamp: Long, rolloverHour: Int): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = timestamp
            }
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            if (currentHour < rolloverHour) {
                // belongs to previous calendar day
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
            cal.set(Calendar.HOUR_OF_DAY, rolloverHour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startTs = cal.timeInMillis

            cal.add(Calendar.DAY_OF_MONTH, 1)
            val endTs = cal.timeInMillis
            return Pair(startTs, endTs)
        }
    }
}
