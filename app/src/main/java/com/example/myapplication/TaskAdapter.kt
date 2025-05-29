package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemTaskBinding

class TaskAdapter : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    var onTaskCheckedChanged: ((Task, Boolean) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.apply {
                taskNameTextView.text = task.name
                taskCheckBox.isChecked = task.isCompleted

                // Установка цвета в зависимости от важности
                val importanceColor = when (task.importance) {
                    "high" -> itemView.context.getColor(android.R.color.holo_red_light)
                    "medium" -> itemView.context.getColor(android.R.color.holo_orange_light)
                    else -> itemView.context.getColor(android.R.color.holo_green_light)
                }
                importanceIndicator.setBackgroundColor(importanceColor)

                // Отображение дедлайна для разовых задач
                if (task.type == "onetime" && !task.deadline.isNullOrEmpty()) {
                    deadlineTextView.text = "Срок: ${task.deadline}"
                    deadlineTextView.visibility = android.view.View.VISIBLE
                } else {
                    deadlineTextView.visibility = android.view.View.GONE
                }

                taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    onTaskCheckedChanged?.invoke(task, isChecked)
                }
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}