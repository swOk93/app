package com.example.myapplication

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

/**
 * Пользовательский маркер для отображения значения при нажатии на точку графика
 */
class MarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    
    private val tvContent: TextView = findViewById(R.id.tvContent)
    
    // Вызывается при каждом обновлении позиции маркера
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val value = e.y.toInt()
            tvContent.text = "$value"
        }
        super.refreshContent(e, highlight)
    }
    
    // Смещение маркера (центрирование по точке)
    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 10)
    }
}