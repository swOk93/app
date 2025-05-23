package com.example.myapplication

import android.content.Context
import android.widget.Toast
import androidx.core.content.edit

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
    
    fun addProgressRecord(position: Int, count: Int, timestamp: Long = System.currentTimeMillis()) {
        progressRecords.add(ProgressRecord(position, count, timestamp))
        // Сохраняем историю после добавления новой записи
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