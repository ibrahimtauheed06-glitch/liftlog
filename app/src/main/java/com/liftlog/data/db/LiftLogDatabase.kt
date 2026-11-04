package com.liftlog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.liftlog.data.entity.BodyweightEntry
import com.liftlog.data.entity.Exercise
import com.liftlog.data.entity.ProteinEntry
import com.liftlog.data.entity.RunEntry
import com.liftlog.data.entity.SetEntry
import com.liftlog.data.entity.Template
import com.liftlog.data.entity.TemplateSlot
import com.liftlog.data.entity.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Exercise::class,
        Template::class,
        TemplateSlot::class,
        Workout::class,
        SetEntry::class,
        ProteinEntry::class,
        BodyweightEntry::class,
        RunEntry::class
    ],
    version = 3,
    exportSchema = true
)
abstract class LiftLogDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun setEntryDao(): SetEntryDao
    abstract fun proteinDao(): ProteinDao
    abstract fun bodyweightDao(): BodyweightDao
    abstract fun runDao(): RunDao

    companion object {
        @Volatile
        private var INSTANCE: LiftLogDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `run_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateTs` INTEGER NOT NULL,
                        `minutes` INTEGER NOT NULL,
                        `intensity` TEXT NOT NULL,
                        `note` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workouts` ADD COLUMN `isDiscarded` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `defaultWeightKg` REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getInstance(context: Context): LiftLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LiftLogDatabase::class.java,
                    "liftlog.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).seedInitialData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedInitialData() {
        if (exerciseDao().getCount() > 0) return

        val exercises = listOf(
            // Upper A
            Exercise(
                name = "Bench Press",
                equipment = "barbell",
                incrementKg = 2.5,
                repLo = 5,
                repHi = 8,
                defaultSets = 3,
                restSec = 150,
                isCompound = true,
                bwFactor = 0.0,
                primaryMuscles = "Chest",
                secondaryMuscles = "Triceps, Front Delts",
                setupNote = "Retract scapulae, slight arch, eyes directly under the bar"
            ),
            Exercise(
                name = "Weighted Pull-up",
                equipment = "bodyweight",
                incrementKg = 2.5,
                repLo = 5,
                repHi = 8,
                defaultSets = 3,
                restSec = 150,
                isCompound = true,
                bwFactor = 1.0,
                primaryMuscles = "Lats",
                secondaryMuscles = "Biceps, Upper Back",
                setupNote = "Attach weight belt firmly around hips, pull chest to bar"
            ),
            Exercise(
                name = "Seated/Chest-Supported Row",
                equipment = "cable",
                incrementKg = 5.0,
                repLo = 8,
                repHi = 12,
                defaultSets = 3,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Upper Back",
                secondaryMuscles = "Lats, Biceps",
                setupNote = "Chest flat on pad, pull with elbows, full stretch forward"
            ),
            Exercise(
                name = "Cuffed Lateral Raise",
                equipment = "cable",
                plateKg = 5.0,
                incrementKg = 5.0,
                repLo = 10,
                repHi = 20,
                defaultSets = 3,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Side Delts",
                secondaryMuscles = "",
                setupNote = "Cuff at wrist height, lean slightly forward, raise out in scapular plane"
            ),
            Exercise(
                name = "Cuffed Triceps Extension",
                equipment = "cable",
                plateKg = 5.0,
                incrementKg = 5.0,
                repLo = 8,
                repHi = 12,
                defaultSets = 2,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Triceps",
                secondaryMuscles = "",
                setupNote = "Cuff at wrist, keep upper arm locked in line with cable"
            ),
            Exercise(
                name = "Strict Pad Curl",
                equipment = "dumbbell",
                incrementKg = 2.5,
                repLo = 8,
                repHi = 12,
                defaultSets = 2,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                homeCapKg = 10.0,
                primaryMuscles = "Biceps",
                secondaryMuscles = "Forearms",
                setupNote = "Elbow pinned against preacher pad, no swinging"
            ),

            // Upper B
            Exercise(
                name = "Smith Overhead Press",
                equipment = "machine",
                incrementKg = 2.5,
                repLo = 6,
                repHi = 10,
                defaultSets = 3,
                restSec = 150,
                isCompound = true,
                bwFactor = 0.0,
                primaryMuscles = "Front Delts",
                secondaryMuscles = "Triceps, Upper Chest",
                setupNote = "Seat upright or 85 deg, lower to upper chest/chin"
            ),
            Exercise(
                name = "Weighted Dip",
                equipment = "bodyweight",
                incrementKg = 2.5,
                repLo = 6,
                repHi = 10,
                defaultSets = 3,
                restSec = 150,
                isCompound = true,
                bwFactor = 1.0,
                primaryMuscles = "Chest",
                secondaryMuscles = "Triceps, Front Delts",
                setupNote = "Slight forward torso lean for chest emphasis, 90 deg elbow depth"
            ),
            Exercise(
                name = "Lat Pulldown",
                equipment = "cable",
                incrementKg = 5.0,
                repLo = 8,
                repHi = 12,
                defaultSets = 3,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Lats",
                secondaryMuscles = "Biceps",
                setupNote = "Thigh pad snug, pull to clavicle with elbows tucked"
            ),
            Exercise(
                name = "Rear Delt Fly/Face Pull",
                equipment = "cable",
                incrementKg = 5.0,
                repLo = 12,
                repHi = 20,
                defaultSets = 2,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Rear Delts",
                secondaryMuscles = "Upper Back",
                setupNote = "Pull rope toward eyes/forehead, rotate wrists out"
            ),
            Exercise(
                name = "Hammer Curl",
                equipment = "dumbbell",
                incrementKg = 2.5,
                repLo = 8,
                repHi = 12,
                defaultSets = 2,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                homeCapKg = 10.0,
                primaryMuscles = "Biceps",
                secondaryMuscles = "Forearms",
                setupNote = "Neutral palms, thumbs pointing up throughout curl"
            ),
            Exercise(
                name = "Overhead Triceps Extension",
                equipment = "cable",
                incrementKg = 5.0,
                repLo = 8,
                repHi = 12,
                defaultSets = 2,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Triceps",
                secondaryMuscles = "",
                setupNote = "Set cable at shoulder height, extend forward overhead"
            ),

            // Lower A
            Exercise(
                name = "Hack Squat",
                equipment = "machine",
                incrementKg = 5.0,
                repLo = 6,
                repHi = 10,
                defaultSets = 3,
                restSec = 150,
                isCompound = true,
                bwFactor = 0.0,
                primaryMuscles = "Quads",
                secondaryMuscles = "Glutes",
                setupNote = "Feet low-to-mid platform, deep knee flexion, back pressed to pad"
            ),
            Exercise(
                name = "Romanian Deadlift",
                equipment = "barbell",
                incrementKg = 2.5,
                repLo = 6,
                repHi = 10,
                defaultSets = 3,
                restSec = 150,
                isCompound = true,
                bwFactor = 0.0,
                primaryMuscles = "Hamstrings",
                secondaryMuscles = "Glutes, Lower Back",
                setupNote = "Soft knees, hinge hips back until deep hamstring stretch"
            ),
            Exercise(
                name = "Calf Raise",
                equipment = "machine",
                incrementKg = 5.0,
                repLo = 8,
                repHi = 15,
                defaultSets = 3,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Calves",
                secondaryMuscles = "",
                setupNote = "2s pause at deep bottom stretch, 1s peak squeeze"
            ),
            Exercise(
                name = "Hanging Leg Raise",
                equipment = "bodyweight",
                incrementKg = 0.0,
                repLo = 8,
                repHi = 15,
                defaultSets = 2,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Abs",
                secondaryMuscles = "",
                setupNote = "Curl pelvis toward ribs, avoid excessive body swing"
            ),
            Exercise(
                name = "Leg Extension",
                equipment = "machine",
                incrementKg = 5.0,
                repLo = 10,
                repHi = 15,
                defaultSets = 2,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Quads",
                secondaryMuscles = "",
                setupNote = "Align knee axis with machine pivot point",
                archived = true
            ),

            // Lower B
            Exercise(
                name = "Deadlift",
                equipment = "barbell",
                incrementKg = 2.5,
                repLo = 5,
                repHi = 5,
                defaultSets = 2,
                restSec = 180,
                isCompound = true,
                bwFactor = 0.0,
                primaryMuscles = "Hamstrings",
                secondaryMuscles = "Glutes, Back",
                setupNote = "Bar over mid-foot, brace lats and core before pull"
            ),
            Exercise(
                name = "Leg Press",
                equipment = "machine",
                incrementKg = 5.0,
                repLo = 8,
                repHi = 12,
                defaultSets = 3,
                restSec = 150,
                isCompound = true,
                bwFactor = 0.0,
                primaryMuscles = "Quads",
                secondaryMuscles = "Glutes",
                setupNote = "Full depth without pelvis lifting off back pad"
            ),
            Exercise(
                name = "Reverse Hamstring Curl",
                equipment = "machine",
                incrementKg = 5.0,
                repLo = 8,
                repHi = 12,
                defaultSets = 3,
                restSec = 75,
                isCompound = false,
                bwFactor = 0.0,
                primaryMuscles = "Hamstrings",
                secondaryMuscles = "",
                setupNote = "Keep hips flat against pad, curl heels to glutes"
            )
        )

        val insertedExerciseIds = mutableMapOf<String, Long>()
        exercises.forEach { ex ->
            val id = exerciseDao().insertExercise(ex)
            insertedExerciseIds[ex.name] = id
        }

        // Templates: Upper A (Mon), Lower A (Tue), Upper B (Thu), Lower B (Fri)
        val tUpperAId = templateDao().insertTemplate(Template(name = "Upper A", orderIndex = 0))
        val tLowerAId = templateDao().insertTemplate(Template(name = "Lower A", orderIndex = 1))
        val tUpperBId = templateDao().insertTemplate(Template(name = "Upper B", orderIndex = 2))
        val tLowerBId = templateDao().insertTemplate(Template(name = "Lower B", orderIndex = 3))

        // Upper A slots:
        // Bench Press | Weighted Pull-up | Seated/Chest-Supported Row SS1 | Cuffed Lateral Raise SS1 | Cuffed Triceps Extension SS2 | Strict Pad Curl SS2
        val upperASlots = listOf(
            TemplateSlot(templateId = tUpperAId, exerciseId = insertedExerciseIds["Bench Press"]!!, slotOrder = 0),
            TemplateSlot(templateId = tUpperAId, exerciseId = insertedExerciseIds["Weighted Pull-up"]!!, slotOrder = 1),
            TemplateSlot(templateId = tUpperAId, exerciseId = insertedExerciseIds["Seated/Chest-Supported Row"]!!, slotOrder = 2, supersetGroup = 1),
            TemplateSlot(templateId = tUpperAId, exerciseId = insertedExerciseIds["Cuffed Lateral Raise"]!!, slotOrder = 3, supersetGroup = 1),
            TemplateSlot(templateId = tUpperAId, exerciseId = insertedExerciseIds["Cuffed Triceps Extension"]!!, slotOrder = 4, supersetGroup = 2),
            TemplateSlot(templateId = tUpperAId, exerciseId = insertedExerciseIds["Strict Pad Curl"]!!, slotOrder = 5, supersetGroup = 2)
        )
        templateDao().insertSlots(upperASlots)

        // Lower A slots:
        // Hack Squat | Romanian Deadlift | Calf Raise | Hanging Leg Raise | Leg Extension (archived)
        val lowerASlots = listOf(
            TemplateSlot(templateId = tLowerAId, exerciseId = insertedExerciseIds["Hack Squat"]!!, slotOrder = 0),
            TemplateSlot(templateId = tLowerAId, exerciseId = insertedExerciseIds["Romanian Deadlift"]!!, slotOrder = 1),
            TemplateSlot(templateId = tLowerAId, exerciseId = insertedExerciseIds["Calf Raise"]!!, slotOrder = 2),
            TemplateSlot(templateId = tLowerAId, exerciseId = insertedExerciseIds["Hanging Leg Raise"]!!, slotOrder = 3),
            TemplateSlot(templateId = tLowerAId, exerciseId = insertedExerciseIds["Leg Extension"]!!, slotOrder = 4)
        )
        templateDao().insertSlots(lowerASlots)

        // Upper B slots:
        // Smith Overhead Press | Weighted Dip | Lat Pulldown | Cuffed Lateral Raise SS1 | Rear Delt Fly/Face Pull SS1 | Hammer Curl SS2 | Overhead Triceps Extension SS2
        val upperBSlots = listOf(
            TemplateSlot(templateId = tUpperBId, exerciseId = insertedExerciseIds["Smith Overhead Press"]!!, slotOrder = 0),
            TemplateSlot(templateId = tUpperBId, exerciseId = insertedExerciseIds["Weighted Dip"]!!, slotOrder = 1),
            TemplateSlot(templateId = tUpperBId, exerciseId = insertedExerciseIds["Lat Pulldown"]!!, slotOrder = 2),
            TemplateSlot(templateId = tUpperBId, exerciseId = insertedExerciseIds["Cuffed Lateral Raise"]!!, slotOrder = 3, supersetGroup = 1),
            TemplateSlot(templateId = tUpperBId, exerciseId = insertedExerciseIds["Rear Delt Fly/Face Pull"]!!, slotOrder = 4, supersetGroup = 1),
            TemplateSlot(templateId = tUpperBId, exerciseId = insertedExerciseIds["Hammer Curl"]!!, slotOrder = 5, supersetGroup = 2),
            TemplateSlot(templateId = tUpperBId, exerciseId = insertedExerciseIds["Overhead Triceps Extension"]!!, slotOrder = 6, supersetGroup = 2)
        )
        templateDao().insertSlots(upperBSlots)

        // Lower B slots:
        // Deadlift | Leg Press | Reverse Hamstring Curl | Calf Raise (shared)
        val lowerBSlots = listOf(
            TemplateSlot(templateId = tLowerBId, exerciseId = insertedExerciseIds["Deadlift"]!!, slotOrder = 0),
            TemplateSlot(templateId = tLowerBId, exerciseId = insertedExerciseIds["Leg Press"]!!, slotOrder = 1),
            TemplateSlot(templateId = tLowerBId, exerciseId = insertedExerciseIds["Reverse Hamstring Curl"]!!, slotOrder = 2),
            TemplateSlot(templateId = tLowerBId, exerciseId = insertedExerciseIds["Calf Raise"]!!, slotOrder = 3)
        )
        templateDao().insertSlots(lowerBSlots)
    }
}
