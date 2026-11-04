package com.liftlog.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("workoutId"),
        Index("exerciseId")
    ]
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val ts: Long,
    val weightKg: Double, // added weight for bodyweight lifts
    val reps: Int,
    val type: String = "working", // "warmup" | "working" | "drop"
    val rpe: Double? = null,
    val flagSwung: Boolean = false,
    val flagPain: Boolean = false,
    val note: String? = null
)
