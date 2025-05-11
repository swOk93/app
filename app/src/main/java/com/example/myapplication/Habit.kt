package com.example.myapplication

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HabitType {
    TIME, REPEAT, SIMPLE
}

data class Habit(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val type: HabitType,
    val target: Int = 0, // целевое значение (минуты или повторения)
    val current: Int = 0, // текущее значение
    val createdDate: Date = Date()
) {
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return "Создано: ${dateFormat.format(createdDate)}"
    }
    
    fun getProgressText(): String {
        return when (type) {
            HabitType.TIME -> "$current / $target мин"
            HabitType.REPEAT -> "$current / $target раз"
            HabitType.SIMPLE -> if (current > 0) "Выполнено" else "Не выполнено"
        }
    }
    
    fun isCompleted(): Boolean {
        return when (type) {
            HabitType.TIME, HabitType.REPEAT -> current >= target
            HabitType.SIMPLE -> current > 0
        }
    }
}