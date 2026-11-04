package com.liftlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.liftlog.data.entity.ProteinEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ProteinDao {

    @Query("SELECT * FROM protein_entries WHERE ts >= :startTs AND ts < :endTs ORDER BY ts DESC")
    fun getEntriesBetween(startTs: Long, endTs: Long): Flow<List<ProteinEntry>>

    @Query("SELECT * FROM protein_entries WHERE ts >= :startTs AND ts < :endTs ORDER BY ts DESC")
    suspend fun getEntriesBetweenSync(startTs: Long, endTs: Long): List<ProteinEntry>

    @Query("SELECT * FROM protein_entries ORDER BY ts DESC")
    fun getAllEntries(): Flow<List<ProteinEntry>>

    @Query("SELECT * FROM protein_entries ORDER BY ts DESC")
    suspend fun getAllEntriesSync(): List<ProteinEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ProteinEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ProteinEntry>): List<Long>

    @Update
    suspend fun update(entry: ProteinEntry)

    @Query("DELETE FROM protein_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
