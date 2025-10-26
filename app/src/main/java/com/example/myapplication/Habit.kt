package com.example.myapplication

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HabitType {
    TIME, REPEAT, SIMPLE
}

// Базовый интерфейс для всех разделов
interface HabitSectionBase {
    val displayName: String
}

// Перечисление для встроенных разделов привычек
enum class HabitSection(override val displayName: String) : HabitSectionBase {
    ALL("Все привычки и задачи"),
    OTHER("Другое");
    
    companion object {
        // Список пользовательских разделов
        private val customSections = mutableListOf<HabitSectionCustom>()
        
        // Метод для добавления пользовательского раздела
        fun addCustomSection(name: String): HabitSectionCustom {
            // Проверяем, не существует ли уже такой раздел
            val existingSection = customSections.find { it.displayName == name }
            if (existingSection != null) {
                return existingSection
            }
            
            val newSection = HabitSectionCustom(name)
            customSections.add(newSection)
            return newSection
        }
        
        // Метод для получения всех разделов (встроенные + пользовательские)
        fun getAllSections(): List<HabitSectionBase> {
            val result = mutableListOf<HabitSectionBase>()
            // Добавляем встроенные разделы
            entries.forEach { result.add(it) }
            // Добавляем пользовательские разделы
            result.addAll(customSections)
            return result
        }
        
        // Метод для получения раздела по его названию
        fun getSectionByName(name: String): HabitSectionBase {
            // Сначала ищем среди встроенных разделов
            val builtInSection = values().find { it.displayName == name }
            if (builtInSection != null) {
                return builtInSection
            }
            
            // Если не нашли, ищем среди пользовательских разделов
            val customSection = customSections.find { it.displayName == name }
            return customSection ?: ALL // Если ничего не нашли, возвращаем ALL
        }
        
        // Метод для загрузки пользовательских разделов
        fun loadCustomSections(names: List<String>) {
            customSections.clear()
            names.forEach { addCustomSection(it) }
        }
        
        // Метод для получения списка имен пользовательских разделов
        fun getCustomSectionNames(): List<String> {
            return customSections.map { it.displayName }
        }
        
        // Метод для создания стандартных разделов при первом запуске
        fun createDefaultSections() {
            addCustomSection("Спорт")
            addCustomSection("Здоровье")
            addCustomSection("Работа")
        }
    }
}

// Класс для пользовательского раздела
data class HabitSectionCustom(override val displayName: String) : HabitSectionBase

data class Habit(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val type: HabitType,
    val target: Int = 0, // целевое значение (минуты или повторения)
    val current: Int = 0, // текущее значение
    val createdDate: Date = Date(),
    val unit: String = "", // пользовательская единица измерения
    val section: HabitSectionBase = HabitSection.ALL // раздел привычки
) {
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return "Создано: ${dateFormat.format(createdDate)}"
    }
    
    fun getProgressText(): String {
        return when (type) {
            HabitType.TIME -> "$current / $target мин"
            HabitType.REPEAT -> {
                val unitText = unit.ifEmpty { "раз" }
                "$current / $target $unitText"
            }
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