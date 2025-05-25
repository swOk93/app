package com.example.myapplication

import android.content.Context
import android.widget.Toast
import androidx.core.content.edit
import java.util.Calendar

class HabitProgressHistory(private val context: Context) {
    private val progressRecords = mutableListOf<ProgressRecord>()
    
    data class ProgressRecord(
        val position: Int,
        val count: Int,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    init {
        // Загружаем историю прогресса при создании объекта
        loadProgressHistory()
    }
    
    /**
     * Добавляет или обновляет запись о прогрессе привычки за текущий день
     * Если запись за текущий день уже существует, она будет обновлена
     */
    fun addProgressRecord(position: Int, count: Int, timestamp: Long = System.currentTimeMillis()) {
        // Получаем календарь для текущей даты записи
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        // Обнуляем время, оставляя только дату
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val recordDate = calendar.timeInMillis
        
        // Ищем существующую запись за этот день
        val existingRecordIndex = progressRecords.indexOfFirst { record -> 
            val recordCalendar = Calendar.getInstance()
            recordCalendar.timeInMillis = record.timestamp
            recordCalendar.set(Calendar.HOUR_OF_DAY, 0)
            recordCalendar.set(Calendar.MINUTE, 0)
            recordCalendar.set(Calendar.SECOND, 0)
            recordCalendar.set(Calendar.MILLISECOND, 0)
            
            record.position == position && recordCalendar.timeInMillis == recordDate
        }
        
        if (existingRecordIndex >= 0) {
            // Обновляем существующую запись
            progressRecords[existingRecordIndex] = ProgressRecord(position, count, timestamp)
        } else {
            // Добавляем новую запись
            progressRecords.add(ProgressRecord(position, count, timestamp))
        }
        
        // Сохраняем историю после добавления/обновления записи
        saveProgressHistory()
    }
    
    fun getRecordsForHabit(position: Int): List<ProgressRecord> {
        return progressRecords.filter { it.position == position }
    }
    
    fun clearRecords() {
        progressRecords.clear()
        // Очищаем сохраненную историю
        saveProgressHistory()
    }
    
    /**
     * Сохраняет историю прогресса в SharedPreferences
     */
    fun saveProgressHistory() {
        val sharedPreferences = context.getSharedPreferences("ProgressHistoryPrefs", Context.MODE_PRIVATE)
        
        sharedPreferences.edit {
            // Сохраняем количество записей
            putInt("records_count", progressRecords.size)
            
            // Сохраняем каждую запись
            progressRecords.forEachIndexed { index, record ->
                putInt("record_${index}_position", record.position)
                putInt("record_${index}_count", record.count)
                putLong("record_${index}_timestamp", record.timestamp)
            }
        }
    }
    
    /**
     * Загружает историю прогресса из SharedPreferences
     */
    fun loadProgressHistory() {
        val sharedPreferences = context.getSharedPreferences("ProgressHistoryPrefs", Context.MODE_PRIVATE)
        val recordsCount = sharedPreferences.getInt("records_count", 0)
        
        if (recordsCount == 0) {
            return
        }
        
        // Очищаем текущие записи перед загрузкой
        progressRecords.clear()
        
        for (i in 0 until recordsCount) {
            val position = sharedPreferences.getInt("record_${i}_position", 0)
            val count = sharedPreferences.getInt("record_${i}_count", 0)
            val timestamp = sharedPreferences.getLong("record_${i}_timestamp", System.currentTimeMillis())
            
            progressRecords.add(ProgressRecord(position, count, timestamp))
        }
    }
}