package com.liftlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.liftlog.data.entity.RunEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {

    @Query("SELECT * FROM run_entries ORDER BY dateTs DESC")
    fun getAllRuns(): Flow<List<RunEntry>>

    @Query("SELECT * FROM run_entries ORDER BY dateTs DESC")
    suspend fun getAllRunsSync(): List<RunEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RunEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RunEntry>): List<Long>

    @Update
    suspend fun update(entry: RunEntry)

    @Query("DELETE FROM run_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
