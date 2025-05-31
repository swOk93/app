package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemTaskBinding

class TaskAdapter : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    var onTaskCheckedChanged: ((Task, Boolean) -> Unit)? = null
    var onTaskDelete: ((Task) -> Unit)? = null
    var onTaskEdit: ((Task) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        private var isExpanded = false

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

                // --- Слайдер ---
                val sliderLayout = binding.sliderLayout.root
                val minusButton = sliderLayout.findViewById<android.view.View>(R.id.minusButton)
                val plusButton = sliderLayout.findViewById<android.view.View>(R.id.plusButton)
                val hoursSeekBar = sliderLayout.findViewById<android.view.View>(R.id.hoursSeekBar)
                val hoursTextView = sliderLayout.findViewById<android.view.View>(R.id.hoursTextView)
                val hoursValueTextView = sliderLayout.findViewById<android.view.View>(R.id.hoursValueTextView)
                val minutesSeekBar = sliderLayout.findViewById<android.view.View>(R.id.minutesSeekBar)
                val minutesTextView = sliderLayout.findViewById<android.view.View>(R.id.minutesTextView)
                val minutesValueTextView = sliderLayout.findViewById<android.view.View>(R.id.minutesValueTextView)
                val chartButton = sliderLayout.findViewById<android.view.View>(R.id.chartButton)
                val sliderDeleteButton = sliderLayout.findViewById<android.widget.ImageButton>(R.id.sliderDeleteButton)
                val sliderEditButton = sliderLayout.findViewById<android.widget.ImageButton>(R.id.sliderEditButton)

                // Скрываем все кроме удалить/редактировать
                minusButton.visibility = android.view.View.GONE
                plusButton.visibility = android.view.View.GONE
                hoursSeekBar.visibility = android.view.View.GONE
                hoursTextView.visibility = android.view.View.GONE
                hoursValueTextView.visibility = android.view.View.GONE
                minutesSeekBar.visibility = android.view.View.GONE
                minutesTextView.visibility = android.view.View.GONE
                minutesValueTextView.visibility = android.view.View.GONE
                chartButton.visibility = android.view.View.GONE

                // Кнопки удалить/редактировать
                sliderDeleteButton.setOnClickListener {
                    onTaskDelete?.invoke(task)
                    sliderLayout.visibility = android.view.View.GONE
                    isExpanded = false
                }
                sliderEditButton.setOnClickListener {
                    onTaskEdit?.invoke(task)
                    sliderLayout.visibility = android.view.View.GONE
                    isExpanded = false
                }

                // --- Клик по задаче или стрелке ---
                val expandClickListener = android.view.View.OnClickListener {
                    isExpanded = !isExpanded
                    sliderLayout.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
                    expandArrowButton.setImageResource(
                        if (isExpanded) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float
                    )
                }
                itemView.setOnClickListener(expandClickListener)
                expandArrowButton.setOnClickListener(expandClickListener)

                // Скрываем слайдер при биндинге
                sliderLayout.visibility = android.view.View.GONE
                expandArrowButton.setImageResource(android.R.drawable.arrow_down_float)
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