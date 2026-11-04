package com.liftlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val equipment: String, // barbell | dumbbell | cable | machine | bodyweight
    val incrementKg: Double = 2.5,
    val plateKg: Double? = null,
    val repLo: Int = 5,
    val repHi: Int = 8,
    val defaultSets: Int = 3,
    val defaultWeightKg: Double = 0.0,
    val restSec: Int = 150,
    val isCompound: Boolean = true,
    val bwFactor: Double = 0.0, // 0.0 or 1.0
    val primaryMuscles: String = "", // comma-separated e.g. "Chest, Triceps"
    val secondaryMuscles: String = "", // comma-separated e.g. "Shoulders"
    val setupNote: String? = null,
    val homeCapKg: Double? = null,
    val variantOf: Long? = null, // parent exercise ID for variants/merges
    val archived: Boolean = false
)
