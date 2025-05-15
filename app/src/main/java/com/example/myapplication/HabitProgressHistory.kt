package com.example.myapplication

import android.content.Context
import android.widget.Toast

class HabitProgressHistory(private val context: Context) {
    private val progressRecords = mutableListOf<ProgressRecord>()
    
    data class ProgressRecord(
        val position: Int,
        val count: Int,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    fun addProgressRecord(position: Int, count: Int, timestamp: Long = System.currentTimeMillis()) {
        progressRecords.add(ProgressRecord(position, count, timestamp))
    }
    
    fun getRecordsForHabit(position: Int): List<ProgressRecord> {
        return progressRecords.filter { it.position == position }
    }
    
    fun clearRecords() {
        progressRecords.clear()
    }
}