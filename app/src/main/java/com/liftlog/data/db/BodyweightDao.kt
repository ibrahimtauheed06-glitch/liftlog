package com.liftlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.liftlog.data.entity.BodyweightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyweightDao {

    @Query("SELECT * FROM bodyweight_entries ORDER BY ts DESC")
    fun getAllEntries(): Flow<List<BodyweightEntry>>

    @Query("SELECT * FROM bodyweight_entries ORDER BY ts DESC")
    suspend fun getAllEntriesSync(): List<BodyweightEntry>

    @Query("SELECT * FROM bodyweight_entries WHERE ts >= :sinceTs ORDER BY ts DESC")
    fun getEntriesSince(sinceTs: Long): Flow<List<BodyweightEntry>>

    @Query("SELECT * FROM bodyweight_entries WHERE ts >= :sinceTs ORDER BY ts DESC")
    suspend fun getEntriesSinceSync(sinceTs: Long): List<BodyweightEntry>

    @Query("SELECT AVG(kg) FROM bodyweight_entries WHERE ts >= :sinceTs")
    suspend fun getAverageWeightSince(sinceTs: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BodyweightEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<BodyweightEntry>): List<Long>

    @Update
    suspend fun update(entry: BodyweightEntry)

    @Query("DELETE FROM bodyweight_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
