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

    @Query("SELECT * FROM workouts WHERE isDiscarded = 0 ORDER BY startTs DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE isDiscarded = 1 ORDER BY startTs DESC")
    fun getDiscardedWorkoutsFlow(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): Workout?

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getWorkoutByIdFlow(id: Long): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE endTs IS NULL AND isDiscarded = 0 ORDER BY startTs DESC LIMIT 1")
    suspend fun getActiveWorkout(): Workout?

    @Query("SELECT * FROM workouts WHERE endTs IS NULL AND isDiscarded = 0 ORDER BY startTs DESC LIMIT 1")
    fun getActiveWorkoutFlow(): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE startTs >= :startTs AND startTs <= :endTs AND endTs IS NOT NULL AND isDiscarded = 0")
    suspend fun getCompletedWorkoutsBetween(startTs: Long, endTs: Long): List<Workout>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<Workout>): List<Long>

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Query("UPDATE workouts SET endTs = :endTs, note = :note WHERE id = :id")
    suspend fun completeWorkout(id: Long, endTs: Long, note: String?)

    @Query("UPDATE workouts SET isDiscarded = 1, endTs = :discardTs WHERE id = :id")
    suspend fun discardWorkout(id: Long, discardTs: Long)

    @Query("UPDATE workouts SET isDiscarded = 0 WHERE id = :id")
    suspend fun restoreWorkout(id: Long)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)
}
