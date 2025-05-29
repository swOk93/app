package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(val habits: MutableList<Habit>) : 
    RecyclerView.Adapter<HabitAdapter.HabitViewHolder>(), 
    java.util.Comparator<Habit> {
    
    // Интерфейс для обработки перетаскивания
    interface OnStartDragListener {
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }
    
    // Слушатель для перетаскивания
    var dragListener: OnStartDragListener? = null
    
    interface HabitListener {
        fun onDeleteHabit(position: Int)
        fun onUpdateProgress(position: Int, count: Int)
        fun onShowChart(position: Int)
        fun onEditHabit(position: Int)
    }
    
    var listener: HabitListener? = null
    
    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.habitNameTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.habitDateTextView)
        val progressTextView: TextView = itemView.findViewById(R.id.habitProgressTextView)
        val expandArrowButton: ImageButton = itemView.findViewById(R.id.expandArrowButton)
        val expandButton: CheckBox = itemView.findViewById(R.id.expandButton)
        val sliderLayout: View = itemView.findViewById(R.id.sliderLayout)
        val dragHandleImageView: View = itemView.findViewById(R.id.dragHandleImageView)
        
        // Элементы управления ползунками
        val hoursSeekBar: SeekBar = sliderLayout.findViewById(R.id.hoursSeekBar)
        val minutesSeekBar: SeekBar = sliderLayout.findViewById(R.id.minutesSeekBar)
        val hoursValueTextView: TextView = sliderLayout.findViewById(R.id.hoursValueTextView)
        val minutesValueTextView: TextView = sliderLayout.findViewById(R.id.minutesValueTextView)
        val plusButton: Button = sliderLayout.findViewById(R.id.plusButton)
        val minusButton: Button = sliderLayout.findViewById(R.id.minusButton)
        val sliderDeleteButton: ImageButton = sliderLayout.findViewById(R.id.sliderDeleteButton)
        val sliderEditButton: ImageButton = sliderLayout.findViewById(R.id.sliderEditButton)
        val chartButton: ImageButton = sliderLayout.findViewById(R.id.chartButton)
        
        private var isExpanded = false
        
        init {
            sliderDeleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onDeleteHabit(position)
                }
            }
            
            sliderEditButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onEditHabit(position)
                }
            }
            
            chartButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onShowChart(position)
                }
            }
            
            // Обработчик нажатия на всю карточку и стрелку
            val expandClickListener = View.OnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    toggleExpand()
                }
            }
            itemView.setOnClickListener(expandClickListener)
            expandArrowButton.setOnClickListener(expandClickListener)
            
            // Обработчик длительного нажатия на иконку перетаскивания
            dragHandleImageView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    dragListener?.onStartDrag(this)
                }
                true // Возвращаем true, чтобы показать, что событие обработано
            }
            
            // Предотвращаем открытие слайдера при нажатии на иконку перетаскивания
            dragHandleImageView.setOnClickListener { /* Ничего не делаем */ }

            // Обработчик нажатия на кнопку
            expandButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val habit = habits[position]
                    // Для всех типов привычек переключаем статус выполнения
                    when (habit.type) {
                        HabitType.TIME, HabitType.REPEAT -> {
                            val newValue = if (habit.isCompleted()) 0 else habit.target
                            listener?.onUpdateProgress(position, newValue)
                        }
                        HabitType.SIMPLE -> {
                            val newValue = if (habit.current > 0) 0 else 1
                            listener?.onUpdateProgress(position, newValue)
                        }
                    }
                    toggleExpand()
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
                    val currentValue = habit.current
                    when (habit.type) {
                        HabitType.TIME -> {
                            val hours = hoursSeekBar.progress
                            val minutes = minutesSeekBar.progress
                            if (hours > 0 || minutes > 0) {
                                // Сначала вычисляем новые значения
                                val addValue = hours * 60 + minutes
                                val newValue = currentValue + addValue
                                // Затем обновляем ползунки
                                hoursSeekBar.progress = 0
                                minutesSeekBar.progress = 0
                                // И вызываем обновление прогресса
                                listener?.onUpdateProgress(position, newValue)
                                toggleExpand()
                            }
                        }
                        HabitType.REPEAT -> {
                            // Добавляем к текущему значению привычки, а не просто устанавливаем
                            val addValue = hoursSeekBar.progress
                            val newValue = currentValue + addValue
                            hoursValueTextView.text = newValue.toString()
                            listener?.onUpdateProgress(position, newValue)
                        }
                        else -> {}
                    }
                }
            }
            
            minusButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val habit = habits[position]
                    val currentValue = habit.current
                    when (habit.type) {
                        HabitType.TIME -> {
                            val hours = hoursSeekBar.progress
                            val minutes = minutesSeekBar.progress
                            if (hours > 0 || minutes > 0) {
                                // Сначала вычисляем новые значения
                                val subtractValue = hours * 60 + minutes
                                val newValue = (currentValue - subtractValue).coerceAtLeast(0)
                                    // Затем обновляем ползунки
                                hoursSeekBar.progress = 0
                                minutesSeekBar.progress = 0
                                
                                // И вызываем обновление прогресса
                                listener?.onUpdateProgress(position, newValue)
                                toggleExpand()
                            }
                        }
                        HabitType.REPEAT -> {
                            // Вычитаем из текущего значения привычки значение с ползунка
                            val subtractValue = hoursSeekBar.progress
                            val newValue = (currentValue - subtractValue).coerceAtLeast(0)
                            hoursValueTextView.text = newValue.toString()
                            listener?.onUpdateProgress(position, newValue)
                        }
                        else -> {}
                    }
                }
            }
        }
        
        fun toggleExpand() {
            isExpanded = !isExpanded
            sliderLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            // Обновляем иконку стрелки
            expandArrowButton.setImageResource(
                if (isExpanded) android.R.drawable.arrow_up_float 
                else android.R.drawable.arrow_down_float
            )
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
        
        // Установка фона для индикатора прогресса
        val backgroundResource = if (habit.isCompleted()) {
            R.drawable.progress_indicator_background
        } else {
            R.drawable.progress_indicator_red_background
        }
        holder.progressTextView.setBackgroundResource(backgroundResource)
        
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
                hoursTextView.setText(R.string.hours_short)
                minutesTextView.setText(R.string.minutes_short)
                
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
                plusButton.visibility = View.VISIBLE
                minusButton.visibility = View.VISIBLE
                
                // Настраиваем кнопки
                plusButton.text = "+"
                minusButton.text = "-"
                
                // Устанавливаем состояние чекбокса в зависимости от статуса выполнения
                holder.expandButton.isChecked = habit.isCompleted()
            }
            HabitType.REPEAT -> {
                // Для привычек с повторениями показываем только ползунок повторений
                hoursTextView.setText(R.string.quantity)
                
                hoursSeekBar.max = 100 // Максимальное количество повторений
                hoursSeekBar.progress = habit.current.coerceAtMost(100)
                hoursValueTextView.text = hoursSeekBar.progress.toString()
                
                hoursSeekBar.visibility = View.VISIBLE
                minutesSeekBar.visibility = View.GONE
                hoursTextView.visibility = View.VISIBLE
                minutesTextView.visibility = View.GONE
                minutesValueTextView.visibility = View.GONE
                hoursValueTextView.visibility = View.VISIBLE
                plusButton.visibility = View.VISIBLE
                minusButton.visibility = View.VISIBLE
                
                // Настраиваем кнопки
                plusButton.text = "+"
                minusButton.text = "-"
                
                // Устанавливаем состояние чекбокса в зависимости от статуса выполнения
                holder.expandButton.isChecked = habit.isCompleted()
            }
            HabitType.SIMPLE -> {
                // Для простых привычек скрываем все элементы управления
                hoursSeekBar.visibility = View.GONE
                minutesSeekBar.visibility = View.GONE
                hoursTextView.visibility = View.GONE
                minutesTextView.visibility = View.GONE
                hoursValueTextView.visibility = View.GONE
                minutesValueTextView.visibility = View.GONE
                plusButton.visibility = View.GONE
                minusButton.visibility = View.GONE
                
                // Устанавливаем состояние чекбокса в зависимости от статуса
                holder.expandButton.isChecked = habit.current > 0
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
    
    // Метод для перемещения привычки из одной позиции в другую
    fun moveHabit(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            // Перемещение вниз
            for (i in fromPosition until toPosition) {
                habits[i] = habits[i + 1].also { habits[i + 1] = habits[i] }
            }
        } else {
            // Перемещение вверх
            for (i in fromPosition downTo toPosition + 1) {
                habits[i] = habits[i - 1].also { habits[i - 1] = habits[i] }
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }
    
    // Реализация метода compare интерфейса Comparator
    override fun compare(habit1: Habit, habit2: Habit): Int {
        // По умолчанию сравниваем по имени
        return habit1.name.compareTo(habit2.name)
    }
}