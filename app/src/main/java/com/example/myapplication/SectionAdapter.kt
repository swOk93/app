package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SectionAdapter(
    private val sections: List<HabitSectionBase>,
    private val addNewSectionText: String,
    private val onSectionClick: (HabitSectionBase) -> Unit,
    private val onDeleteClick: (HabitSectionBase) -> Unit,
    private val onAddSectionClick: () -> Unit
) : RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {
    
    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sectionNameTextView: TextView = itemView.findViewById(R.id.sectionNameTextView)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section_dropdown, parent, false)
        return SectionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        // Последний элемент - "Добавить новый раздел"
        if (position == sections.size) {
            holder.sectionNameTextView.text = addNewSectionText
            holder.deleteButton.visibility = View.GONE
            
            holder.itemView.setOnClickListener {
                onAddSectionClick.invoke()
            }
        } else {
            val section = sections[position]
            holder.sectionNameTextView.text = section.displayName
            
            // Проверяем, является ли раздел встроенным
            val isBuiltIn = section is HabitSection
            
            // Скрываем кнопку удаления для всех встроенных разделов (HabitSection)
            holder.deleteButton.visibility = if (isBuiltIn) View.GONE else View.VISIBLE
            
            holder.itemView.setOnClickListener {
                onSectionClick.invoke(section)
            }
            
            holder.deleteButton.setOnClickListener {
                onDeleteClick.invoke(section)
            }
        }
    }
    
    override fun getItemCount(): Int = sections.size + 1 // +1 для "Добавить новый раздел"
} 