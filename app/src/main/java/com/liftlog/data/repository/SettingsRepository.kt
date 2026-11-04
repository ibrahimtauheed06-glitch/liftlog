package com.liftlog.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "liftlog_settings")

data class UserSettings(
    val proteinTarget: Double = 110.0,
    val dayRolloverHour: Int = 4,
    val weeklySetTarget: Int = 10,
    val sessionsPerWeekTarget: Int = 4,
    val phase: String = "GYM", // GYM | HOME
    val availablePlates: String = "20,15,10,5,2.5,1.25",
    val barWeight: Double = 20.0,
    val twoSetWarmup: Boolean = false,
    val activeWorkoutId: Long? = null,
    val lastDeloadDismissTs: Long = 0L,
    val deloadActiveUntilTs: Long = 0L,
    val lastExportTs: Long = 0L
) {
    fun parsePlates(): List<Double> {
        return availablePlates.split(",")
            .mapNotNull { it.trim().toDoubleOrNull() }
            .sortedDescending()
    }
}

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val PROTEIN_TARGET = doublePreferencesKey("protein_target")
        val DAY_ROLLOVER_HOUR = intPreferencesKey("day_rollover_hour")
        val WEEKLY_SET_TARGET = intPreferencesKey("weekly_set_target")
        val SESSIONS_PER_WEEK_TARGET = intPreferencesKey("sessions_per_week_target")
        val PHASE = stringPreferencesKey("phase")
        val AVAILABLE_PLATES = stringPreferencesKey("available_plates")
        val BAR_WEIGHT = doublePreferencesKey("bar_weight")
        val TWO_SET_WARMUP = booleanPreferencesKey("two_set_warmup")
        val ACTIVE_WORKOUT_ID = longPreferencesKey("active_workout_id")
        val LAST_DELOAD_DISMISS_TS = longPreferencesKey("last_deload_dismiss_ts")
        val DELOAD_ACTIVE_UNTIL_TS = longPreferencesKey("deload_active_until_ts")
        val LAST_EXPORT_TS = longPreferencesKey("last_export_ts")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val activeId = preferences[PreferencesKeys.ACTIVE_WORKOUT_ID]
        UserSettings(
            proteinTarget = preferences[PreferencesKeys.PROTEIN_TARGET] ?: 110.0,
            dayRolloverHour = preferences[PreferencesKeys.DAY_ROLLOVER_HOUR] ?: 4,
            weeklySetTarget = preferences[PreferencesKeys.WEEKLY_SET_TARGET] ?: 10,
            sessionsPerWeekTarget = preferences[PreferencesKeys.SESSIONS_PER_WEEK_TARGET] ?: 4,
            phase = preferences[PreferencesKeys.PHASE] ?: "GYM",
            availablePlates = preferences[PreferencesKeys.AVAILABLE_PLATES] ?: "20,15,10,5,2.5,1.25",
            barWeight = preferences[PreferencesKeys.BAR_WEIGHT] ?: 20.0,
            twoSetWarmup = preferences[PreferencesKeys.TWO_SET_WARMUP] ?: false,
            activeWorkoutId = if (activeId != null && activeId > 0) activeId else null,
            lastDeloadDismissTs = preferences[PreferencesKeys.LAST_DELOAD_DISMISS_TS] ?: 0L,
            deloadActiveUntilTs = preferences[PreferencesKeys.DELOAD_ACTIVE_UNTIL_TS] ?: 0L,
            lastExportTs = preferences[PreferencesKeys.LAST_EXPORT_TS] ?: 0L
        )
    }

    suspend fun updateProteinTarget(target: Double) {
        context.dataStore.edit { it[PreferencesKeys.PROTEIN_TARGET] = target }
    }

    suspend fun updateDayRolloverHour(hour: Int) {
        context.dataStore.edit { it[PreferencesKeys.DAY_ROLLOVER_HOUR] = hour }
    }

    suspend fun updateWeeklySetTarget(target: Int) {
        context.dataStore.edit { it[PreferencesKeys.WEEKLY_SET_TARGET] = target }
    }

    suspend fun updateSessionsPerWeekTarget(target: Int) {
        context.dataStore.edit { it[PreferencesKeys.SESSIONS_PER_WEEK_TARGET] = target }
    }

    suspend fun updatePhase(phase: String) {
        context.dataStore.edit { it[PreferencesKeys.PHASE] = phase }
    }

    suspend fun updateAvailablePlates(plates: String) {
        context.dataStore.edit { it[PreferencesKeys.AVAILABLE_PLATES] = plates }
    }

    suspend fun updateBarWeight(weight: Double) {
        context.dataStore.edit { it[PreferencesKeys.BAR_WEIGHT] = weight }
    }

    suspend fun updateTwoSetWarmup(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TWO_SET_WARMUP] = enabled }
    }

    suspend fun setActiveWorkoutId(id: Long?) {
        context.dataStore.edit {
            if (id != null) {
                it[PreferencesKeys.ACTIVE_WORKOUT_ID] = id
            } else {
                it.remove(PreferencesKeys.ACTIVE_WORKOUT_ID)
            }
        }
    }

    suspend fun dismissDeload() {
        context.dataStore.edit {
            it[PreferencesKeys.LAST_DELOAD_DISMISS_TS] = System.currentTimeMillis()
        }
    }

    suspend fun acceptDeload() {
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        context.dataStore.edit {
            it[PreferencesKeys.DELOAD_ACTIVE_UNTIL_TS] = System.currentTimeMillis() + sevenDaysMs
        }
    }

    suspend fun updateLastExportTs(ts: Long) {
        context.dataStore.edit { it[PreferencesKeys.LAST_EXPORT_TS] = ts }
    }
}
