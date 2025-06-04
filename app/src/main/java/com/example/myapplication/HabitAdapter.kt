package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.AutoCompleteTextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ArrayAdapter

class HabitAdapter(val habits: MutableList<Habit>) : 
    RecyclerView.Adapter<HabitAdapter.HabitViewHolder>(), 
    java.util.Comparator<Habit> {
    
    // Храним полный список привычек и отфильтрованный список
    private val allHabits = mutableListOf<Habit>()
    private val filteredHabits = mutableListOf<Habit>()
    
    init {
        // Инициализируем списки
        allHabits.addAll(habits)
        filteredHabits.addAll(habits)
    }
    
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
        val sectionSpinner: AutoCompleteTextView? = sliderLayout.findViewById(R.id.sectionSpinner)
        
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
            
            // Обработчик нажатия на иконку перетаскивания (без задержки)
            dragHandleImageView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    dragListener?.onStartDrag(this)
                }
            }

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
        val habit = filteredHabits[position]
        
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
        
        // Настройка выпадающего списка разделов
        setupSectionSpinner(holder, habit)
        
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
    
    override fun getItemCount() = filteredHabits.size
    
    /**
     * Фильтрует привычки по указанному разделу
     */
    fun filterBySection(section: HabitSection) {
        // Очищаем текущий список
        filteredHabits.clear()
        
        // Добавляем только привычки выбранного раздела
        filteredHabits.addAll(allHabits.filter { it.section == section })
        
        // Обновляем отображение
        notifyDataSetChanged()
    }
    
    /**
     * Показывает все привычки
     */
    fun showAllHabits() {
        // Очищаем текущий список
        filteredHabits.clear()
        
        // Добавляем все привычки
        filteredHabits.addAll(allHabits)
        
        // Обновляем отображение
        notifyDataSetChanged()
    }
    
    /**
     * Добавляет привычку в список
     */
    fun addHabit(habit: Habit) {
        // Добавляем в оба списка
        allHabits.add(habit)
        
        // Проверяем фильтры перед добавлением в отображаемый список
        if (filteredHabits.size < allHabits.size) {
            // Есть фильтрация, проверяем соответствие
            val lastFilter = if (filteredHabits.isEmpty()) null else filteredHabits[0].section
            if (lastFilter == null || lastFilter == HabitSection.ALL || lastFilter == habit.section) {
                filteredHabits.add(habit)
            }
        } else {
            // Нет фильтрации, добавляем
            filteredHabits.add(habit)
        }
        
        notifyItemInserted(filteredHabits.size - 1)
    }
    
    /**
     * Обновляет привычку по указанной позиции
     */
    fun updateHabit(position: Int, updatedHabit: Habit) {
        if (position >= 0 && position < filteredHabits.size) {
            val originalHabit = filteredHabits[position]
            
            // Находим индекс в полном списке
            val allIndex = allHabits.indexOfFirst { it.id == originalHabit.id }
            if (allIndex >= 0) {
                allHabits[allIndex] = updatedHabit
            }
            
            // Обновляем в отфильтрованном списке
            filteredHabits[position] = updatedHabit
            
            notifyItemChanged(position)
        }
    }
    
    /**
     * Удаляет привычку по указанной позиции
     */
    fun removeHabit(position: Int) {
        if (position >= 0 && position < filteredHabits.size) {
            val habitToRemove = filteredHabits[position]
            
            // Удаляем из обоих списков
            allHabits.removeAll { it.id == habitToRemove.id }
            filteredHabits.removeAt(position)
            
            notifyItemRemoved(position)
        }
    }
    
    /**
     * Перемещает привычку с одной позиции на другую
     */
    fun moveHabit(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                filteredHabits[i] = filteredHabits.set(i + 1, filteredHabits[i])
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                filteredHabits[i] = filteredHabits.set(i - 1, filteredHabits[i])
            }
        }
        
        // Обновляем allHabits, чтобы сохранить порядок
        allHabits.clear()
        allHabits.addAll(filteredHabits)
        
        notifyItemMoved(fromPosition, toPosition)
    }
    
    override fun compare(h1: Habit, h2: Habit): Int {
        // Сравниваем по дате создания (сначала новые)
        return (h2.createdDate.time - h1.createdDate.time).toInt()
    }
    
    /**
     * Настройка выпадающего списка разделов в слайдере
     */
    private fun setupSectionSpinner(holder: HabitViewHolder, habit: Habit) {
        holder.sectionSpinner?.let { spinner ->
            // Создаем список названий разделов
            val sections = HabitSection.values().map { it.displayName }.toTypedArray()
            
            // Создаем адаптер
            val adapter = ArrayAdapter(
                holder.itemView.context, 
                android.R.layout.simple_dropdown_item_1line, 
                sections
            )
            
            // Устанавливаем адаптер
            spinner.setAdapter(adapter)
            
            // Устанавливаем текущий выбранный раздел
            spinner.setText(habit.section.displayName, false)
            
            // Устанавливаем обработчик выбора элемента
            spinner.setOnItemClickListener { _, _, position, _ ->
                val selectedSection = HabitSection.values()[position]
                
                // Получаем позицию привычки в списке
                val habitPosition = holder.adapterPosition
                if (habitPosition != RecyclerView.NO_POSITION) {
                    // Создаем обновленную привычку с новым разделом
                    val updatedHabit = habit.copy(section = selectedSection)
                    
                    // Обновляем привычку в обоих списках
                    val allIndex = allHabits.indexOfFirst { it.id == habit.id }
                    if (allIndex >= 0) {
                        allHabits[allIndex] = updatedHabit
                    }
                    
                    // Обновляем в отфильтрованном списке
                    filteredHabits[habitPosition] = updatedHabit
                    
                    // Уведомляем об изменении
                    notifyItemChanged(habitPosition)
                }
            }
        }
    }
}