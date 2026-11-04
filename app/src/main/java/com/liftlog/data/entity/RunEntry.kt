package com.liftlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_entries")
data class RunEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTs: Long,
    val minutes: Int,
    val intensity: String = "easy", // "easy" | "hard"
    val note: String? = null
)
