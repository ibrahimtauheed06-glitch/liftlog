package com.liftlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.liftlog.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY name ASC")
    fun getAllActiveExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getExercisesByIds(ids: List<Long>): List<Exercise>

    @Query("SELECT * FROM exercises WHERE variantOf = :parentId")
    suspend fun getVariants(parentId: Long): List<Exercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>): List<Long>

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("UPDATE exercises SET archived = 1 WHERE id = :id")
    suspend fun archiveExercise(id: Long)

    @Query("UPDATE exercises SET archived = 0 WHERE id = :id")
    suspend fun unarchiveExercise(id: Long)

    @Query("UPDATE exercises SET variantOf = :parentId WHERE id = :childId")
    suspend fun setVariant(childId: Long, parentId: Long?)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int
}
