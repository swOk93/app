package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(val habits: MutableList<Habit>) : 
    RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {
    
    interface HabitListener {
        fun onDeleteHabit(position: Int)
        fun onUpdateProgress(position: Int, hours: Int, minutes: Int)
    }
    
    var listener: HabitListener? = null
    
    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.habitNameTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.habitDateTextView)
        val progressTextView: TextView = itemView.findViewById(R.id.habitProgressTextView)
        val expandButton: ImageButton = itemView.findViewById(R.id.expandButton)
        val sliderLayout: View = itemView.findViewById(R.id.sliderLayout)
        
        // Элементы управления ползунками
        val hoursSeekBar: SeekBar = sliderLayout.findViewById(R.id.hoursSeekBar)
        val minutesSeekBar: SeekBar = sliderLayout.findViewById(R.id.minutesSeekBar)
        val hoursValueTextView: TextView = sliderLayout.findViewById(R.id.hoursValueTextView)
        val minutesValueTextView: TextView = sliderLayout.findViewById(R.id.minutesValueTextView)
        val plusButton: Button = sliderLayout.findViewById(R.id.plusButton)
        val minusButton: Button = sliderLayout.findViewById(R.id.minusButton)
        val sliderDeleteButton: ImageButton = sliderLayout.findViewById(R.id.sliderDeleteButton)
        
        private var isExpanded = false
        
        init {
            sliderDeleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onDeleteHabit(position)
                }
            }
            
            expandButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val habit = habits[position]
                    if (habit.type == HabitType.SIMPLE) {
                        // Для простых привычек переключаем статус выполнения
                        val newValue = if (habit.current > 0) 0 else 1
                        listener?.onUpdateProgress(position, newValue, 0)
                    } else {
                        toggleExpand()
                    }
                }
            }
            
            hoursSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    hoursValueTextView.text = progress.toString()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            
            minutesSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    minutesValueTextView.text = progress.toString()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            
            plusButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val habit = habits[position]
                    when (habit.type) {
                        HabitType.TIME -> {
                            val hours = hoursSeekBar.progress
                            val minutes = minutesSeekBar.progress
                            listener?.onUpdateProgress(position, hours, minutes)
                            toggleExpand()
                        }
                        HabitType.REPEAT -> {
                            // Добавляем к текущему значению привычки, а не просто устанавливаем
                            val currentValue = habit.current
                            val addValue = hoursSeekBar.progress
                            val newValue = currentValue + addValue
                            hoursValueTextView.text = newValue.toString()
                            listener?.onUpdateProgress(position, newValue, 0)
                        }
                        HabitType.SIMPLE -> {
                            listener?.onUpdateProgress(position, 1, 0)
                            toggleExpand()
                        }
                    }
                }
            }
            
            minusButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val habit = habits[position]
                    when (habit.type) {
                        HabitType.TIME -> {
                            val hours = hoursSeekBar.progress
                            val minutes = minutesSeekBar.progress
                            if (hours > 0 || minutes > 0) {
                                if (minutes > 0) {
                                    minutesSeekBar.progress = minutes - 1
                                } else if (hours > 0) {
                                    hoursSeekBar.progress = hours - 1
                                    minutesSeekBar.progress = 59
                                }
                                val newHours = hoursSeekBar.progress
                                val newMinutes = minutesSeekBar.progress
                                listener?.onUpdateProgress(position, newHours, newMinutes)
                            }
                        }
                        HabitType.REPEAT -> {
                            // Вычитаем из текущего значения привычки
                            val currentValue = habit.current
                            val newValue = (currentValue - 1).coerceAtLeast(0)
                            hoursValueTextView.text = newValue.toString()
                            listener?.onUpdateProgress(position, newValue, 0)
                        }
                        HabitType.SIMPLE -> {
                            listener?.onUpdateProgress(position, 0, 0)
                            toggleExpand()
                        }
                    }
                }
            }
        }
        
        fun toggleExpand() {
            isExpanded = !isExpanded
            sliderLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            // Получаем позицию и тип привычки
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val habit = habits[position]
                
                // Меняем иконку только для привычек не типа SIMPLE
                if (habit.type != HabitType.SIMPLE) {
                    expandButton.setImageResource(
                        if (isExpanded) android.R.drawable.arrow_up_float 
                        else android.R.drawable.arrow_down_float
                    )
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        
        holder.nameTextView.text = habit.name
        holder.dateTextView.text = habit.getFormattedDate()
        holder.progressTextView.text = habit.getProgressText()
        
        // Установка цвета фона для индикатора прогресса
        val backgroundColor = if (habit.isCompleted()) {
            ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
        } else {
            ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
        }
        holder.progressTextView.setBackgroundColor(backgroundColor)
        
        // Скрываем слайдер при привязке (чтобы не оставался открытым при переиспользовании ViewHolder)
        holder.sliderLayout.visibility = View.GONE
        
        // Настройка ползунков в зависимости от типа привычки
        val hoursSeekBar = holder.hoursSeekBar
        val minutesSeekBar = holder.minutesSeekBar
        val hoursTextView = holder.sliderLayout.findViewById<TextView>(R.id.hoursTextView)
        val minutesTextView = holder.sliderLayout.findViewById<TextView>(R.id.minutesTextView)
        val hoursValueTextView = holder.hoursValueTextView
        val minutesValueTextView = holder.minutesValueTextView
        val plusButton = holder.plusButton
        val minusButton = holder.minusButton
        
        when (habit.type) {
            HabitType.TIME -> {
                // Для привычек по времени показываем оба ползунка
                hoursTextView.text = "Часы"
                minutesTextView.text = "Минуты"
                
                // Устанавливаем начальные значения ползунков
                val hours = habit.current / 60
                val minutes = habit.current % 60
                
                hoursSeekBar.max = 10
                minutesSeekBar.max = 59
                hoursSeekBar.progress = hours.coerceAtMost(10)
                minutesSeekBar.progress = minutes
                
                hoursSeekBar.visibility = View.VISIBLE
                minutesSeekBar.visibility = View.VISIBLE
                hoursTextView.visibility = View.VISIBLE
                minutesTextView.visibility = View.VISIBLE
                hoursValueTextView.visibility = View.VISIBLE
                minutesValueTextView.visibility = View.VISIBLE
                
                // Настраиваем кнопки
                plusButton.text = "+"
                minusButton.text = "-"
                
                // Устанавливаем иконку стрелки для expandButton
                holder.expandButton.setImageResource(android.R.drawable.arrow_down_float)
            }
            HabitType.REPEAT -> {
                // Для привычек с повторениями показываем только ползунок повторений
                hoursTextView.text = "Повторения"
                
                hoursSeekBar.max = 100 // Максимальное количество повторений
                hoursSeekBar.progress = habit.current.coerceAtMost(100)
                hoursValueTextView.text = hoursSeekBar.progress.toString()
                
                hoursSeekBar.visibility = View.VISIBLE
                minutesSeekBar.visibility = View.GONE
                hoursTextView.visibility = View.VISIBLE
                minutesTextView.visibility = View.GONE
                minutesValueTextView.visibility = View.GONE
                hoursValueTextView.visibility = View.VISIBLE
                
                // Настраиваем кнопки
                plusButton.text = "+"
                minusButton.text = "-"
                
                // Устанавливаем иконку стрелки для expandButton
                holder.expandButton.setImageResource(android.R.drawable.arrow_down_float)
            }
            HabitType.SIMPLE -> {
                // Для простых привычек показываем только кнопку "Выполнено"
                hoursSeekBar.visibility = View.GONE
                minutesSeekBar.visibility = View.GONE
                hoursTextView.visibility = View.GONE
                minutesTextView.visibility = View.GONE
                hoursValueTextView.visibility = View.GONE
                minutesValueTextView.visibility = View.GONE
                
                // Меняем текст кнопок для простой привычки
                plusButton.text = "✓"
                minusButton.text = "✗"
                
                // Устанавливаем иконку галочки для expandButton в зависимости от статуса
                if (habit.current > 0) {
                    holder.expandButton.setImageResource(android.R.drawable.checkbox_on_background)
                } else {
                    holder.expandButton.setImageResource(android.R.drawable.checkbox_off_background)
                }
            }
        }
    }
    
    override fun getItemCount() = habits.size
    
    fun addHabit(habit: Habit) {
        habits.add(habit)
        notifyItemInserted(habits.size - 1)
    }
    
    fun removeHabit(position: Int) {
        if (position in 0 until habits.size) {
            habits.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    
    fun updateHabit(position: Int, habit: Habit) {
        if (position in 0 until habits.size) {
            habits[position] = habit
            notifyItemChanged(position)
        }
    }
}