package com.liftlog.data.repository

import com.liftlog.data.db.ExerciseDao
import com.liftlog.data.db.SetEntryDao
import com.liftlog.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val setEntryDao: SetEntryDao
) {

    fun getAllActiveExercises(): Flow<List<Exercise>> = exerciseDao.getAllActiveExercises()

    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()

    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)

    suspend fun saveExercise(exercise: Exercise): Long = exerciseDao.insertExercise(exercise)

    suspend fun updateExercise(exercise: Exercise) = exerciseDao.updateExercise(exercise)

    suspend fun archiveExercise(id: Long) = exerciseDao.archiveExercise(id)

    suspend fun unarchiveExercise(id: Long) = exerciseDao.unarchiveExercise(id)

    /**
     * Merge exercise: marks source as variant of target (variantOf) and reassigns all past sets to target.
     * Never hard-deletes history.
     */
    suspend fun mergeExercise(sourceId: Long, targetId: Long, reassignPastSets: Boolean) {
        exerciseDao.setVariant(sourceId, targetId)
        exerciseDao.archiveExercise(sourceId)
        if (reassignPastSets) {
            setEntryDao.reassignExerciseSets(sourceId, targetId)
        }
    }
}
