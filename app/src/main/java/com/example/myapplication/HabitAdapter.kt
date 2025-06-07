package com.example.myapplication

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Comparator
import java.util.Date

class HabitAdapter(private val habits: MutableList<Habit> = mutableListOf()) : 
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
        
        // Элементы для выбора раздела (новая реализация)
        val sectionSpinnerLayout: View = sliderLayout.findViewById(R.id.sectionSpinnerLayout)
        val sectionsListLayout: View? = sliderLayout.findViewById(R.id.sectionsListLayout)
        val sectionHeaderTextView: TextView? = sectionsListLayout?.findViewById(R.id.sectionHeaderTextView)
        val expandSectionsButton: ImageButton? = sectionsListLayout?.findViewById(R.id.expandSectionsButton)
        
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
                    val habit = filteredHabits[position]
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
                    val habit = filteredHabits[position]
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
                    val habit = filteredHabits[position]
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
     * Перемещает привычку с одной позиции на другую
     */
    fun moveHabit(fromPosition: Int, toPosition: Int) {
        // Проверяем валидность позиций
        if (fromPosition < 0 || fromPosition >= filteredHabits.size || 
            toPosition < 0 || toPosition >= filteredHabits.size) {
            return
        }
        
        // Получаем ссылку на перемещаемую привычку
        val movingHabit = filteredHabits[fromPosition]
        
        // Удаляем привычку с исходной позиции
        filteredHabits.removeAt(fromPosition)
        
        // Вставляем привычку на новую позицию
        filteredHabits.add(toPosition, movingHabit)
        
        // Обновляем полный список, чтобы сохранить порядок
        // Синхронизируем полный список с отфильтрованным
        syncAllHabitsWithFiltered()
        
        // Уведомляем об изменении
        notifyItemMoved(fromPosition, toPosition)
    }
    
    /**
     * Синхронизирует полный список с отфильтрованным списком
     */
    private fun syncAllHabitsWithFiltered() {
        // Создаем временный список для хранения элементов, которые не в отфильтрованном списке
        val nonFilteredItems = allHabits.filter { habit -> 
            filteredHabits.none { it.id == habit.id } 
        }
        
        // Очищаем полный список
        allHabits.clear()
        
        // Сначала добавляем элементы из отфильтрованного списка в том же порядке
        allHabits.addAll(filteredHabits)
        
        // Затем добавляем элементы, которые не в отфильтрованном списке
        allHabits.addAll(nonFilteredItems)
    }
    
    /**
     * Фильтрует привычки по указанному разделу
     */
    fun filterBySection(section: HabitSectionBase) {
        // Сохраняем текущий список
        val previousItems = ArrayList(filteredHabits)
        
        // Очищаем текущий список
        filteredHabits.clear()
        
        // Добавляем только привычки выбранного раздела
        filteredHabits.addAll(allHabits.filter { it.section.displayName == section.displayName })
        
        // Используем DiffUtil для эффективного обновления RecyclerView
        notifyDataSetChanged()
    }
    
    /**
     * Показывает все привычки
     */
    fun showAllHabits() {
        // Сохраняем текущий список
        val previousItems = ArrayList(filteredHabits)
        
        // Очищаем текущий список
        filteredHabits.clear()
        
        // Добавляем все привычки
        filteredHabits.addAll(allHabits)
        
        // Используем DiffUtil для эффективного обновления RecyclerView
        notifyDataSetChanged()
    }
    
    /**
     * Добавляет привычку в список
     */
    fun addHabit(habit: Habit) {
        // Добавляем в полный список
        allHabits.add(habit)
        
        // Проверяем, нужно ли добавлять в отфильтрованный список
        val currentSection = if (filteredHabits.isNotEmpty() && filteredHabits.size < allHabits.size) {
            filteredHabits.firstOrNull()?.section ?: HabitSection.ALL
        } else {
            HabitSection.ALL // Если списки одинаковой длины, значит фильтрации нет
        }
        
        // Добавляем в отфильтрованный список, если нет фильтрации или привычка соответствует фильтру
        if (currentSection == HabitSection.ALL || currentSection.displayName == habit.section.displayName) {
            filteredHabits.add(habit)
            notifyItemInserted(filteredHabits.size - 1)
        } else {
            // Если привычка не соответствует текущему фильтру, просто уведомляем об изменении данных
            notifyDataSetChanged()
        }
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
            
            // Проверяем, соответствует ли обновленная привычка текущему фильтру
            val currentSection = if (filteredHabits.isNotEmpty() && filteredHabits.size < allHabits.size) {
                filteredHabits.firstOrNull()?.section ?: HabitSection.ALL
            } else {
                HabitSection.ALL // Если списки одинаковой длины, значит фильтрации нет
            }
            
            if (currentSection == HabitSection.ALL || currentSection.displayName == updatedHabit.section.displayName) {
                // Привычка соответствует фильтру, обновляем её в отфильтрованном списке
                filteredHabits[position] = updatedHabit
                notifyItemChanged(position)
            } else {
                // Привычка больше не соответствует фильтру, удаляем её из отфильтрованного списка
                filteredHabits.removeAt(position)
                notifyItemRemoved(position)
            }
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
            
            // Обновляем весь список, чтобы избежать проблем с позициями
            notifyDataSetChanged()
        }
    }
    
    override fun compare(h1: Habit, h2: Habit): Int {
        // Сравниваем по дате создания (сначала новые)
        return (h2.createdDate.time - h1.createdDate.time).toInt()
    }
    
    /**
     * Настройка выпадающего списка разделов в слайдере
     */
    private fun setupSectionSpinner(holder: HabitViewHolder, habit: Habit) {
        // Устанавливаем текущий раздел в заголовке
        holder.sectionHeaderTextView?.text = habit.section.displayName
        
        // Настраиваем выпадающий список
        val context = holder.itemView.context
        if (context is MainActivity) {
            // Инициализируем PopupWindow для отображения списка разделов
            val popupView = LayoutInflater.from(context).inflate(R.layout.popup_sections_list, null)
            val sectionsRecyclerView = popupView.findViewById<RecyclerView>(R.id.sectionsRecyclerView)
            
            // Настройка RecyclerView
            sectionsRecyclerView.layoutManager = LinearLayoutManager(context)
            
            // Создаем PopupWindow с временной шириной
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.elevation = 10f
            
            // Создаем адаптер
            val sectionsAdapter = SectionAdapter(
                sections = HabitSection.getAllSections(),
                addNewSectionText = context.getString(R.string.add_new_section),
                onSectionClick = { section ->
                    // Обновляем привычку с новым разделом
                    val habitPosition = holder.adapterPosition
                    if (habitPosition != RecyclerView.NO_POSITION) {
                        // Создаем обновленную привычку с новым разделом
                        val updatedHabit = habit.copy(section = section)
                        
                        // Обновляем привычку в обоих списках
                        val allIndex = allHabits.indexOfFirst { it.id == habit.id }
                        if (allIndex >= 0) {
                            allHabits[allIndex] = updatedHabit
                        }
                        
                        // Обновляем в отфильтрованном списке
                        filteredHabits[habitPosition] = updatedHabit
                        
                        // Обновляем заголовок с названием раздела
                        holder.sectionHeaderTextView?.text = section.displayName
                        
                        // Уведомляем об изменении
                        notifyItemChanged(habitPosition)
                    }
                    
                    // Закрываем popup
                    popupWindow.dismiss()
                },
                onDeleteClick = { section ->
                    // Обработка удаления раздела - делегируем в MainActivity
                    val sectionName = section.displayName
                    val isBuiltInSection = HabitSection.values().any { it.displayName == sectionName }
                    
                    if (isBuiltInSection) {
                        Toast.makeText(context, "Нельзя удалить встроенный раздел", Toast.LENGTH_SHORT).show()
                    } else {
                        // Показываем диалог подтверждения
                        AlertDialog.Builder(context)
                            .setTitle("Удаление раздела")
                            .setMessage("Вы уверены, что хотите удалить раздел \"$sectionName\"?")
                            .setPositiveButton("Да") { _, _ ->
                                // Получаем все привычки в этом разделе
                                val habitsInSection = getAllHabits().filter { 
                                    it.section.displayName == sectionName 
                                }
                                
                                // Все привычки останутся, но открепятся от удаляемого раздела
                                // (переместятся в "Все привычки и задачи")
                                habitsInSection.forEach { h ->
                                    val updatedHabit = h.copy(section = HabitSection.ALL)
                                    updateHabitInAllList(h.id, updatedHabit)
                                }
                                
                                // Удаляем пользовательский раздел из списка через MainActivity
                                val customSections = HabitSection.getCustomSectionNames().toMutableList()
                                customSections.remove(sectionName)
                                HabitSection.loadCustomSections(customSections)
                                
                                // Обновляем список
                                notifyDataSetChanged()
                                
                                // Если текущий раздел был удален, переключаемся на "Все привычки и задачи"
                                if (habit.section.displayName == sectionName) {
                                    // Обновляем привычку с новым разделом ALL
                                    val habitPosition = holder.adapterPosition
                                    if (habitPosition != RecyclerView.NO_POSITION) {
                                        // Создаем обновленную привычку с разделом ALL
                                        val updatedHabit = habit.copy(section = HabitSection.ALL)
                                        
                                        // Обновляем привычку в обоих списках
                                        val allIndex = allHabits.indexOfFirst { it.id == habit.id }
                                        if (allIndex >= 0) {
                                            allHabits[allIndex] = updatedHabit
                                        }
                                        
                                        // Обновляем в отфильтрованном списке
                                        filteredHabits[habitPosition] = updatedHabit
                                        
                                        // Обновляем заголовок с названием раздела
                                        holder.sectionHeaderTextView?.text = HabitSection.ALL.displayName
                                    }
                                }
                                
                                // Закрываем popup
                                popupWindow.dismiss()
                            }
                            .setNegativeButton("Нет", null)
                            .show()
                    }
                },
                onAddSectionClick = {
                    // Показываем диалог добавления нового раздела через MainActivity
                    val addSectionFragment = AddSectionFragment.newInstance()
                    if (context is AddSectionFragment.OnSectionAddedListener) {
                        addSectionFragment.sectionAddedListener = context
                    }
                    addSectionFragment.show(context.supportFragmentManager, "AddSectionFragment")
                    
                    // Закрываем popup
                    popupWindow.dismiss()
                }
            )
            
            // Устанавливаем адаптер
            sectionsRecyclerView.adapter = sectionsAdapter
            
            // Настраиваем кнопку раскрытия списка
            holder.expandSectionsButton?.setOnClickListener {
                if (!popupWindow.isShowing) {
                    // Устанавливаем ширину PopupWindow равной ширине sectionSpinnerLayout
                    popupWindow.width = holder.sectionSpinnerLayout.width
                    
                    // Показываем popup точно под кнопкой
                    popupWindow.showAsDropDown(
                        holder.sectionSpinnerLayout,
                        0, 
                        0
                    )
                    
                    // Меняем иконку на стрелку вверх
                    holder.expandSectionsButton.setImageResource(android.R.drawable.arrow_up_float)
                } else {
                    // Закрываем popup
                    popupWindow.dismiss()
                    
                    // Меняем иконку на стрелку вниз
                    holder.expandSectionsButton.setImageResource(android.R.drawable.arrow_down_float)
                }
            }
            
            // Настраиваем нажатие на заголовок
            holder.sectionHeaderTextView?.setOnClickListener {
                holder.expandSectionsButton?.performClick()
            }
            
            // Обработчик отмены PopupWindow
            popupWindow.setOnDismissListener {
                // Меняем иконку на стрелку вниз
                holder.expandSectionsButton?.setImageResource(android.R.drawable.arrow_down_float)
            }
        }
    }
    
    /**
     * Возвращает полный список привычек
     */
    fun getAllHabits(): List<Habit> {
        return allHabits.toList()
    }
    
    /**
     * Возвращает количество привычек в полном списке
     */
    fun getAllHabitsCount(): Int {
        return allHabits.size
    }
    
    /**
     * Возвращает привычку по указанной позиции в отфильтрованном списке
     */
    fun getHabitAt(position: Int): Habit {
        return filteredHabits[position]
    }
    
    /**
     * Находит индекс привычки в отфильтрованном списке по id
     * @return индекс привычки или -1, если не найдена
     */
    fun getFilteredIndexById(habitId: Long): Int {
        return filteredHabits.indexOfFirst { it.id == habitId }
    }
    
    /**
     * Обновляет привычку в полном списке по id
     */
    fun updateHabitInAllList(habitId: Long, updatedHabit: Habit) {
        val index = allHabits.indexOfFirst { it.id == habitId }
        if (index >= 0) {
            allHabits[index] = updatedHabit
        }
    }
}