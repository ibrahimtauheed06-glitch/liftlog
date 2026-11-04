package com.liftlog.data.repository

import com.liftlog.data.db.RunDao
import com.liftlog.data.entity.RunEntry
import kotlinx.coroutines.flow.Flow

class RunRepository(private val runDao: RunDao) {

    fun getAllRuns(): Flow<List<RunEntry>> = runDao.getAllRuns()

    suspend fun addRun(dateTs: Long, minutes: Int, intensity: String, note: String? = null): Long {
        return runDao.insert(
            RunEntry(
                dateTs = dateTs,
                minutes = minutes,
                intensity = intensity,
                note = note
            )
        )
    }

    suspend fun updateRun(run: RunEntry) {
        runDao.update(run)
    }

    suspend fun deleteRun(id: Long) {
        runDao.delete(id)
    }
}
