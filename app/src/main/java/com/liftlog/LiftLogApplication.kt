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
                val activeId = container.settingsRepository.settingsFlow.first().activeWorkoutId ?: -1L
                container.workoutRepository.cleanAllEmptyWorkouts(activeId)
            } catch (_: Exception) {}
        }
    }
}
