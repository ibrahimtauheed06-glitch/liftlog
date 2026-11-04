package com.liftlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.liftlog.data.entity.SetEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SetEntryDao {

    @Query("SELECT * FROM set_entries WHERE workoutId = :workoutId ORDER BY ts ASC, id ASC")
    fun getSetsForWorkout(workoutId: Long): Flow<List<SetEntry>>

    @Query("SELECT * FROM set_entries WHERE workoutId = :workoutId ORDER BY ts ASC, id ASC")
    suspend fun getSetsForWorkoutSync(workoutId: Long): List<SetEntry>

    @Query("SELECT * FROM set_entries WHERE exerciseId = :exerciseId ORDER BY ts DESC")
    fun getSetsForExercise(exerciseId: Long): Flow<List<SetEntry>>

    @Query("SELECT * FROM set_entries WHERE exerciseId = :exerciseId ORDER BY ts DESC")
    suspend fun getSetsForExerciseSync(exerciseId: Long): List<SetEntry>

    @Query("SELECT * FROM set_entries WHERE exerciseId IN (:exerciseIds) ORDER BY ts DESC")
    suspend fun getSetsForExercisesSync(exerciseIds: List<Long>): List<SetEntry>

    @Query("SELECT * FROM set_entries WHERE ts >= :startTs AND ts <= :endTs")
    suspend fun getSetsBetween(startTs: Long, endTs: Long): List<SetEntry>

    @Query("SELECT * FROM set_entries ORDER BY ts DESC")
    suspend fun getAllSetsSync(): List<SetEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<SetEntry>): List<Long>

    @Update
    suspend fun updateSet(set: SetEntry)

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("UPDATE set_entries SET exerciseId = :targetExerciseId WHERE exerciseId = :sourceExerciseId")
    suspend fun reassignExerciseSets(sourceExerciseId: Long, targetExerciseId: Long)
}
