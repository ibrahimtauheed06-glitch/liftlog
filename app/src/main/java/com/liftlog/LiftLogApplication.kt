package com.liftlog

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LiftLogApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Clean up any empty orphan workouts from previous versions
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.database.workoutDao().cleanEmptyUnfinishedWorkouts()
                val activeId = container.settingsRepository.settingsFlow.first().activeWorkoutId
                if (activeId != null) {
                    val sets = container.database.setEntryDao().getSetsForWorkoutSync(activeId)
                    if (sets.isEmpty()) {
                        container.database.workoutDao().deleteWorkout(activeId)
                        container.settingsRepository.setActiveWorkoutId(null)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
