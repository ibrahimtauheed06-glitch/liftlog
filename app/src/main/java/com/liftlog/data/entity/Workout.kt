package com.liftlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long? = null,
    val startTs: Long,
    val endTs: Long? = null,
    val note: String? = null,
    val isDiscarded: Boolean = false
)
