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
    val createdDate: Date = Date(),
    val unit: String = "" // пользовательская единица измерения
) {
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return "Создано: ${dateFormat.format(createdDate)}"
    }
    
    fun getFormattedDate(context: android.content.Context): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return context.getString(R.string.created_date_format, dateFormat.format(createdDate))
    }
    
    fun getProgressText(): String {
        return when (type) {
            HabitType.TIME -> "$current / $target мин"
            HabitType.REPEAT -> {
                val unitText = if (unit.isNotEmpty()) unit else "раз"
                "$current / $target $unitText"
            }
            HabitType.SIMPLE -> if (current > 0) "Выполнено" else "Не выполнено"
        }
    }
    
    fun getProgressText(context: android.content.Context): String {
        return when (type) {
            HabitType.TIME -> context.getString(R.string.progress_format, current, target, context.getString(R.string.min))
            HabitType.REPEAT -> {
                val unitText = if (unit.isNotEmpty()) unit else context.getString(R.string.times)
                context.getString(R.string.progress_format, current, target, unitText)
            }
            HabitType.SIMPLE -> if (current > 0) context.getString(R.string.completed) else context.getString(R.string.not_completed)
        }
    }
    
    fun isCompleted(): Boolean {
        return when (type) {
            HabitType.TIME, HabitType.REPEAT -> current >= target
            HabitType.SIMPLE -> current > 0
        }
    }
}