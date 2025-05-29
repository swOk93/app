package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "simple" или "onetime"
    val deadline: String?, // для разовых задач
    val importance: String, // "low", "medium", "high"
    var isCompleted: Boolean = false
)