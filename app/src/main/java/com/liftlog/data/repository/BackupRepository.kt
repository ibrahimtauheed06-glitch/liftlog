package com.liftlog.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.liftlog.data.db.LiftLogDatabase
import com.liftlog.data.entity.BodyweightEntry
import com.liftlog.data.entity.Exercise
import com.liftlog.data.entity.ProteinEntry
import com.liftlog.data.entity.RunEntry
import com.liftlog.data.entity.SetEntry
import com.liftlog.data.entity.Template
import com.liftlog.data.entity.TemplateSlot
import com.liftlog.data.entity.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupPayload(
    val version: Int = 2,
    val exportDate: String,
    val timestamp: Long,
    val exercises: List<Exercise>,
    val templates: List<Template>,
    val templateSlots: List<TemplateSlot>,
    val workouts: List<Workout>,
    val setEntries: List<SetEntry>,
    val proteinEntries: List<ProteinEntry>,
    val bodyweightEntries: List<BodyweightEntry>,
    val runEntries: List<RunEntry>
)

enum class ImportMode {
    MERGE,
    REPLACE
}

class BackupRepository(
    private val context: Context,
    private val database: LiftLogDatabase,
    private val settingsRepository: SettingsRepository
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun createBackupPayload(): BackupPayload = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(now))

        val exercises = database.exerciseDao().getAllExercises().first()
        val templates = database.templateDao().getAllTemplates().first()
        val allSlots = mutableListOf<TemplateSlot>()
        for (t in templates) {
            allSlots.addAll(database.templateDao().getSlotsForTemplateSync(t.id))
        }
        val workouts = database.workoutDao().getAllWorkouts().first()
        val sets = database.setEntryDao().getAllSetsSync()
        val protein = database.proteinDao().getAllEntriesSync()
        val bodyweight = database.bodyweightDao().getAllEntriesSync()
        val runs = database.runDao().getAllRunsSync()

        BackupPayload(
            exportDate = dateStr,
            timestamp = now,
            exercises = exercises,
            templates = templates,
            templateSlots = allSlots,
            workouts = workouts,
            setEntries = sets,
            proteinEntries = protein,
            bodyweightEntries = bodyweight,
            runEntries = runs
        )
    }

    suspend fun exportToFolder(folderUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, folderUri)
                ?: return@withContext Result.failure(Exception("Cannot open folder"))

            val payload = createBackupPayload()
            val jsonString = gson.toJson(payload)
            val dateStr = payload.exportDate

            // 1. Full JSON backup file
            val jsonFile = rootDoc.createFile("application/json", "liftlog_backup_$dateStr.json")
            if (jsonFile != null) {
                context.contentResolver.openOutputStream(jsonFile.uri)?.use { os ->
                    os.write(jsonString.toByteArray())
                }
            }

            // 2. sets.csv
            val setsFile = rootDoc.createFile("text/csv", "sets_$dateStr.csv")
            if (setsFile != null) {
                context.contentResolver.openOutputStream(setsFile.uri)?.use { os ->
                    val sb = java.lang.StringBuilder()
                    sb.append("id,workoutId,exerciseId,timestamp,weightKg,reps,type,rpe,flagSwung,flagPain,note\n")
                    for (s in payload.setEntries) {
                        sb.append("${s.id},${s.workoutId},${s.exerciseId},${s.ts},${s.weightKg},${s.reps},${s.type},${s.rpe ?: ""},${s.flagSwung},${s.flagPain},\"${s.note ?: ""}\"\n")
                    }
                    os.write(sb.toString().toByteArray())
                }
            }

            // 3. protein.csv
            val proteinFile = rootDoc.createFile("text/csv", "protein_$dateStr.csv")
            if (proteinFile != null) {
                context.contentResolver.openOutputStream(proteinFile.uri)?.use { os ->
                    val sb = java.lang.StringBuilder()
                    sb.append("id,grams,timestamp\n")
                    for (p in payload.proteinEntries) {
                        sb.append("${p.id},${p.grams},${p.ts}\n")
                    }
                    os.write(sb.toString().toByteArray())
                }
            }

            // 4. bodyweight.csv
            val bwFile = rootDoc.createFile("text/csv", "bodyweight_$dateStr.csv")
            if (bwFile != null) {
                context.contentResolver.openOutputStream(bwFile.uri)?.use { os ->
                    val sb = java.lang.StringBuilder()
                    sb.append("id,kg,timestamp\n")
                    for (b in payload.bodyweightEntries) {
                        sb.append("${b.id},${b.kg},${b.ts}\n")
                    }
                    os.write(sb.toString().toByteArray())
                }
            }

            settingsRepository.updateLastExportTs(System.currentTimeMillis())
            Result.success("Backup exported successfully ($dateStr)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromJsonUri(fileUri: Uri, mode: ImportMode): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonText = context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                InputStreamReader(inputStream).readText()
            } ?: return@withContext Result.failure(Exception("Unable to read file"))

            val payload = gson.fromJson(jsonText, BackupPayload::class.java)
                ?: return@withContext Result.failure(Exception("Invalid backup JSON format"))

            if (mode == ImportMode.REPLACE) {
                database.clearAllTables()
            }

            // Insert records
            database.exerciseDao().insertExercises(payload.exercises)
            database.templateDao().insertTemplates(payload.templates)
            database.templateDao().insertSlots(payload.templateSlots)
            database.workoutDao().insertWorkouts(payload.workouts)
            database.setEntryDao().insertSets(payload.setEntries)
            database.proteinDao().insertAll(payload.proteinEntries)
            database.bodyweightDao().insertAll(payload.bodyweightEntries)
            database.runDao().insertAll(payload.runEntries)

            Result.success("Import successful (${payload.setEntries.size} sets, ${payload.exercises.size} exercises)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isBackupOverdueFlow(): Flow<Boolean> = settingsRepository.settingsFlow.map { settings ->
        val lastExport = settings.lastExportTs
        if (lastExport <= 0L) true
        else (System.currentTimeMillis() - lastExport) >= (14L * 24 * 60 * 60 * 1000)
    }

    suspend fun isBackupOverdue(): Boolean {
        val settings = settingsRepository.settingsFlow.first()
        val lastExport = settings.lastExportTs
        if (lastExport <= 0L) return true
        val fourteenDaysMs = 14L * 24 * 60 * 60 * 1000
        return (System.currentTimeMillis() - lastExport) >= fourteenDaysMs
    }

    /**
     * Performs a local backup once a month, replacing the older backup each time.
     * Silent, automated, 100% offline.
     */
    suspend fun performAutoMonthlyBackupIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.settingsFlow.first()
            val now = System.currentTimeMillis()
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L

            if (settings.lastAutoBackupTs > 0L && (now - settings.lastAutoBackupTs) < thirtyDaysMs) {
                return@withContext false
            }

            val payload = createBackupPayload()
            val jsonString = gson.toJson(payload)

            val backupDir = context.getExternalFilesDir("backups") ?: context.filesDir
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val backupFile = java.io.File(backupDir, "liftlog_auto_backup.json")
            backupFile.writeText(jsonString)

            settingsRepository.updateLastAutoBackupTs(now)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getAutoBackupInfo(): Pair<Boolean, Long>? {
        val backupDir = context.getExternalFilesDir("backups") ?: context.filesDir
        val backupFile = java.io.File(backupDir, "liftlog_auto_backup.json")
        return if (backupFile.exists()) {
            Pair(true, backupFile.lastModified())
        } else null
    }
}
