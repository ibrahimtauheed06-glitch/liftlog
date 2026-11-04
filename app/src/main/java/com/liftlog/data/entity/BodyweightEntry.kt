package com.liftlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bodyweight_entries")
data class BodyweightEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kg: Double,
    val ts: Long
)
