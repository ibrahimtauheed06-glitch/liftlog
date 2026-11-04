package com.liftlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protein_entries")
data class ProteinEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grams: Double,
    val ts: Long
)
