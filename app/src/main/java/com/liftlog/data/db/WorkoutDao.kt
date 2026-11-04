package com.liftlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.liftlog.data.entity.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workouts ORDER BY startTs DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): Workout?

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getWorkoutByIdFlow(id: Long): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE endTs IS NULL ORDER BY startTs DESC LIMIT 1")
    suspend fun getActiveWorkout(): Workout?

    @Query("SELECT * FROM workouts WHERE endTs IS NULL ORDER BY startTs DESC LIMIT 1")
    fun getActiveWorkoutFlow(): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE startTs >= :startTs AND startTs <= :endTs AND endTs IS NOT NULL")
    suspend fun getCompletedWorkoutsBetween(startTs: Long, endTs: Long): List<Workout>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<Workout>): List<Long>

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Query("UPDATE workouts SET endTs = :endTs, note = :note WHERE id = :id")
    suspend fun completeWorkout(id: Long, endTs: Long, note: String?)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)
}
