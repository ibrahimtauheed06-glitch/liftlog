package com.liftlog.data.repository

import com.liftlog.data.db.BodyweightDao
import com.liftlog.data.entity.BodyweightEntry
import kotlinx.coroutines.flow.Flow

class BodyweightRepository(private val bodyweightDao: BodyweightDao) {

    fun getAllEntries(): Flow<List<BodyweightEntry>> = bodyweightDao.getAllEntries()

    suspend fun getRecent7DayAverage(): Double {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        return bodyweightDao.getAverageWeightSince(sevenDaysAgo) ?: 0.0
    }

    suspend fun addEntry(kg: Double, ts: Long = System.currentTimeMillis()): Long {
        return bodyweightDao.insert(BodyweightEntry(kg = kg, ts = ts))
    }

    suspend fun updateEntry(entry: BodyweightEntry) {
        bodyweightDao.update(entry)
    }

    suspend fun deleteEntry(id: Long) {
        bodyweightDao.delete(id)
    }
}
