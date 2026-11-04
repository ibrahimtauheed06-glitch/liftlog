package com.liftlog

import android.content.Context
import com.liftlog.data.db.LiftLogDatabase
import com.liftlog.data.repository.BackupRepository
import com.liftlog.data.repository.BodyweightRepository
import com.liftlog.data.repository.ExerciseRepository
import com.liftlog.data.repository.ProteinRepository
import com.liftlog.data.repository.RunRepository
import com.liftlog.data.repository.SettingsRepository
import com.liftlog.data.repository.WorkoutRepository

class AppContainer(val context: Context) {

    val database: LiftLogDatabase by lazy {
        LiftLogDatabase.getInstance(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(
            workoutDao = database.workoutDao(),
            setEntryDao = database.setEntryDao(),
            exerciseDao = database.exerciseDao(),
            templateDao = database.templateDao(),
            bodyweightDao = database.bodyweightDao(),
            settingsRepository = settingsRepository
        )
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(
            exerciseDao = database.exerciseDao(),
            setEntryDao = database.setEntryDao()
        )
    }

    val proteinRepository: ProteinRepository by lazy {
        ProteinRepository(
            proteinDao = database.proteinDao(),
            settingsRepository = settingsRepository
        )
    }

    val bodyweightRepository: BodyweightRepository by lazy {
        BodyweightRepository(
            bodyweightDao = database.bodyweightDao()
        )
    }

    val runRepository: RunRepository by lazy {
        RunRepository(
            runDao = database.runDao()
        )
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            context = context,
            database = database,
            settingsRepository = settingsRepository
        )
    }
}
