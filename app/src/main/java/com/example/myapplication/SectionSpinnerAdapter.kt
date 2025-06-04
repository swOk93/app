package com.example.myapplication

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Кастомный адаптер для выпадающего списка разделов
 */
class SectionSpinnerAdapter(
    context: Context,
    private val items: List<String>,
    private val addNewSectionText: String
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, items) {
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Получаем значение элемента
        val item = items[position]
        
        // Создаем или переиспользуем представление элемента
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        
        // Находим TextView для отображения текста
        val textView = view.findViewById<TextView>(android.R.id.text1)
        
        // Устанавливаем текст
        textView.text = item
        
        // Если это пункт "Добавить новый раздел", выделяем его
        if (item == addNewSectionText) {
            // Применяем стиль для пункта "Добавить новый раздел"
            textView.setTypeface(textView.typeface, Typeface.BOLD)
            textView.setTextColor(ContextCompat.getColor(context, R.color.mint_accent))
            
            // Добавляем отступ перед элементом (визуальное разделение)
            view.setPadding(
                view.paddingLeft,
                view.paddingTop + 8,
                view.paddingRight,
                view.paddingBottom
            )
        } else {
            // Обычный стиль для других пунктов
            textView.setTypeface(textView.typeface, Typeface.NORMAL)
            textView.setTextColor(ContextCompat.getColor(context, R.color.mint_text_primary))
        }
        
        return view
    }
    
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Тот же стиль используется и для выпадающего списка
        return getView(position, convertView, parent)
    }
} 