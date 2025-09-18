package com.example.myapplication

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ActivityMainBinding
import java.util.Date
import androidx.fragment.app.commit
import androidx.core.content.edit
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView

class MainActivity : AppCompatActivity(), HabitAdapter.HabitListener, HabitAdapter.OnStartDragListener, AddSectionFragment.OnSectionAddedListener {

    private lateinit var binding: ActivityMainBinding
    public lateinit var habitAdapter: HabitAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    public lateinit var progressHistory: HabitProgressHistory
    
    // Текущий выбранный раздел (по умолчанию - все привычки)
    private var currentSection: HabitSectionBase = HabitSection.ALL
    
    // Публичные методы для управления видимостью контейнеров
    /**
     * Показывает список привычек и скрывает контейнер фрагментов
     */
    fun showHabitList() {
        binding.fragmentContainer.visibility = View.GONE
        binding.habitsRecyclerView.visibility = View.VISIBLE
        binding.addTaskButton.visibility = View.VISIBLE // Показываем кнопку добавления привычки
        binding.sectionSpinnerLayout.visibility = View.VISIBLE // Показываем контейнер с разделами
        // Кнопки навигации остаются видимыми
        binding.TypeGroup.visibility = View.VISIBLE
    }
    
    /**
     * Показывает контейнер фрагментов и скрывает список привычек
     */
    fun showFragmentContainer() {
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.habitsRecyclerView.visibility = View.GONE
        binding.addTaskButton.visibility = View.GONE // Скрываем кнопку добавления привычки
        // Кнопки навигации остаются видимыми
        binding.TypeGroup.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Устанавливаем текущий раздел на "Все привычки и задачи" при запуске
        currentSection = HabitSection.ALL
        
        // Загружаем пользовательские разделы
        loadCustomSections()
        
        // Принудительно устанавливаем "Все привычки и задачи" как текущий раздел
        currentSection = HabitSection.ALL
        
        // Настройка списка разделов
        setupSectionsList()
        
        // Инициализация RecyclerView
        setupRecyclerView()

        // Настройка кнопки добавления задачи
        binding.addTaskButton.setOnClickListener {
            // Показываем AddHabitFragment как диалог
            val addHabitFragment = AddHabitFragment()
            addHabitFragment.show(supportFragmentManager, "AddHabitFragment")
        }
        
        // Настройка кнопок переключения между разделами
        setupNavigationButtons()
        
        // Привычки уже загружены или созданы в setupRecyclerView()
        
        // Проверяем, нужно ли сбросить прогресс привычек
        checkAndResetHabits()
        
        // Применяем фильтр по текущему разделу (по умолчанию - Все привычки и задачи)
        filterHabitsBySection(currentSection)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                        Toast.makeText(this, getString(R.string.settings), Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Настройка списка разделов
     */
    fun setupSectionsList() {
        // Получаем ссылки на компоненты
        val sectionsListLayout = binding.sectionSpinnerLayout.findViewById<View>(R.id.sectionsListLayout)
        val sectionHeaderTextView = sectionsListLayout.findViewById<TextView>(R.id.sectionHeaderTextView)
        val expandSectionsButton = sectionsListLayout.findViewById<ImageButton>(R.id.expandSectionsButton)
        
        // Устанавливаем текст текущего раздела
        sectionHeaderTextView.text = currentSection.displayName
        
        // Инициализируем PopupWindow для отображения списка разделов
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_sections_list, null)
        val sectionsRecyclerView = popupView.findViewById<RecyclerView>(R.id.sectionsRecyclerView)
        
        // Настройка RecyclerView
        sectionsRecyclerView.layoutManager = LinearLayoutManager(this)
        
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
            addNewSectionText = getString(R.string.add_new_section),
            onSectionClick = { section ->
                // Обрабатываем выбор раздела
                currentSection = section
                sectionHeaderTextView.text = section.displayName
                filterHabitsBySection(section)
                
                // Закрываем popup
                popupWindow.dismiss()
            },
            onDeleteClick = { section ->
                // Обрабатываем нажатие на кнопку удаления
                // Проверяем, является ли раздел встроенным
                val sectionName = section.displayName
                val isBuiltInSection = HabitSection.entries.any { it.displayName == sectionName }
                
                if (isBuiltInSection) {
                    Toast.makeText(this, "Нельзя удалить встроенный раздел", Toast.LENGTH_SHORT).show()
                } else {
                    // Показываем диалог подтверждения
                    AlertDialog.Builder(this)
                        .setTitle("Удаление раздела")
                        .setMessage("Вы уверены, что хотите удалить раздел \"$sectionName\"?")
                        .setPositiveButton("Да") { _, _ ->
                            // Получаем все привычки в этом разделе
                            val habitsInSection = habitAdapter.getAllHabits().filter { 
                                it.section.displayName == sectionName 
                            }
                            
                            // Все привычки останутся, но открепятся от удаляемого раздела
                            // (переместятся в "Другое")
                            habitsInSection.forEach { habit ->
                                val updatedHabit = habit.copy(section = HabitSection.OTHER)
                                habitAdapter.updateHabitInAllList(habit.id, updatedHabit)
                            }
                            
                            // Удаляем пользовательский раздел из списка
                            val customSections = HabitSection.getCustomSectionNames().toMutableList()
                            customSections.remove(sectionName)
                            HabitSection.loadCustomSections(customSections)
                            // Применяем обновление UI единым способом
                            applySectionsChange(selectSection = null, deletedSectionName = sectionName)
                            // Закрываем popup
                            popupWindow.dismiss()
                        }
                        .setNegativeButton("Нет", null)
                        .show()
                }
            },
            onAddSectionClick = {
                // Показываем диалог добавления нового раздела
                val addSectionFragment = AddSectionFragment.newInstance()
                addSectionFragment.sectionAddedListener = this@MainActivity
                addSectionFragment.show(supportFragmentManager, "AddSectionFragment")
                
                // Закрываем popup
                popupWindow.dismiss()
            }
        )
        
        // Устанавливаем адаптер
        sectionsRecyclerView.adapter = sectionsAdapter
        
        // Настраиваем кнопку раскрытия списка
        expandSectionsButton.setOnClickListener {
            if (!popupWindow.isShowing) {
                // Определяем положение для отображения popup
                val location = IntArray(2)
                binding.sectionSpinnerLayout.getLocationOnScreen(location)
                
                // Устанавливаем ширину PopupWindow равной ширине sectionsListLayout
                popupWindow.width = sectionsListLayout.width
                
                // Показываем popup точно под кнопкой
                popupWindow.showAsDropDown(
                    sectionsListLayout,
                    0, 
                    0
                )
                
                // Меняем иконку на стрелку вверх
                expandSectionsButton.setImageResource(android.R.drawable.arrow_up_float)
            } else {
                // Закрываем popup
                popupWindow.dismiss()
                
                // Меняем иконку на стрелку вниз
                expandSectionsButton.setImageResource(android.R.drawable.arrow_down_float)
            }
        }
        
        // Настраиваем нажатие на заголовок
        sectionHeaderTextView.setOnClickListener {
            expandSectionsButton.performClick()
        }
        
        // Обработчик отмены PopupWindow
        popupWindow.setOnDismissListener {
            // Меняем иконку на стрелку вниз
            expandSectionsButton.setImageResource(android.R.drawable.arrow_down_float)
        }
    }
    
    /**
     * Настройка списка разделов для переданного view
     * Используется в AddHabitFragment
     */
    fun setupSectionsList(view: View) {
        // Получаем ссылки на компоненты
        val sectionHeaderTextView = view.findViewById<TextView>(R.id.sectionHeaderTextView)
        val expandSectionsButton = view.findViewById<ImageButton>(R.id.expandSectionsButton)
        
        // Устанавливаем текст текущего раздела
        sectionHeaderTextView.text = HabitSection.ALL.displayName
        
        // Инициализируем PopupWindow для отображения списка разделов
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_sections_list, null)
        val sectionsRecyclerView = popupView.findViewById<RecyclerView>(R.id.sectionsRecyclerView)
        
        // Настройка RecyclerView
        sectionsRecyclerView.layoutManager = LinearLayoutManager(this)
        
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
            addNewSectionText = getString(R.string.add_new_section),
            onSectionClick = { section ->
                // Обрабатываем выбор раздела
                sectionHeaderTextView.text = section.displayName
                
                // Закрываем popup
                popupWindow.dismiss()
                
                // Обновляем currentHabitSection в AddHabitFragment
                val addHabitFragment = supportFragmentManager.findFragmentByTag("AddHabitFragment") as? AddHabitFragment
                addHabitFragment?.updateSelectedSection(section)
            },
            onDeleteClick = { section ->
                // Обрабатываем нажатие на кнопку удаления
                // Проверяем, является ли раздел встроенным
                val sectionName = section.displayName
                val isBuiltInSection = HabitSection.entries.any { it.displayName == sectionName }
                
                if (isBuiltInSection) {
                    Toast.makeText(this, "Нельзя удалить встроенный раздел", Toast.LENGTH_SHORT).show()
                } else {
                    // Показываем диалог подтверждения
                    AlertDialog.Builder(this)
                        .setTitle("Удаление раздела")
                        .setMessage("Вы уверены, что хотите удалить раздел \"$sectionName\"?")
                        .setPositiveButton("Да") { _, _ ->
                            // Получаем все привычки в этом разделе
                            val habitsInSection = habitAdapter.getAllHabits().filter { 
                                it.section.displayName == sectionName 
                            }
                            
                            // Все привычки останутся, но открепятся от удаляемого раздела
                            // (переместятся в "Другое")
                            habitsInSection.forEach { habit ->
                                val updatedHabit = habit.copy(section = HabitSection.OTHER)
                                habitAdapter.updateHabitInAllList(habit.id, updatedHabit)
                            }
                            
                            // Удаляем пользовательский раздел из списка
                            val customSections = HabitSection.getCustomSectionNames().toMutableList()
                            customSections.remove(sectionName)
                            HabitSection.loadCustomSections(customSections)
                            // Persist changes: save sections and updated habits
                            saveCustomSections()
                            saveHabits()
                            
                            // Применяем обновление UI единым способом
                            applySectionsChange(selectSection = null, deletedSectionName = sectionName)
                            // Закрываем popup
                            popupWindow.dismiss()
                        }
                        .setNegativeButton("Нет", null)
                        .show()
                }
            },
            onAddSectionClick = {
                // Показываем диалог добавления нового раздела
                val addSectionFragment = AddSectionFragment.newInstance()
                addSectionFragment.sectionAddedListener = this@MainActivity
                addSectionFragment.show(supportFragmentManager, "AddSectionFragment")
                
                // Закрываем popup
                popupWindow.dismiss()
            }
        )
        
        // Устанавливаем адаптер
        sectionsRecyclerView.adapter = sectionsAdapter
        
        // Настраиваем кнопку раскрытия списка
        expandSectionsButton.setOnClickListener {
            if (!popupWindow.isShowing) {
                // Определяем положение для отображения popup
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                
                // Устанавливаем ширину PopupWindow равной ширине view
                popupWindow.width = view.width
                
                // Показываем popup точно под кнопкой
                popupWindow.showAsDropDown(
                    view,
                    0, 
                    0
                )
                
                // Меняем иконку на стрелку вверх
                expandSectionsButton.setImageResource(android.R.drawable.arrow_up_float)
            } else {
                // Закрываем popup
                popupWindow.dismiss()
                
                // Меняем иконку на стрелку вниз
                expandSectionsButton.setImageResource(android.R.drawable.arrow_down_float)
            }
        }
        
        // Настраиваем нажатие на заголовок
        sectionHeaderTextView.setOnClickListener {
            expandSectionsButton.performClick()
        }
        
        // Обработчик отмены PopupWindow
        popupWindow.setOnDismissListener {
            // Меняем иконку на стрелку вниз
            expandSectionsButton.setImageResource(android.R.drawable.arrow_down_float)
        }
    }
    
    /**
     * Обновляет список разделов
     */
    private fun updateSectionsList() {
        setupSectionsList()
    }

    /**
     * Применяет изменения разделов: сохраняет, обновляет UI и выбор везде.
     * selectSection — какой раздел выбрать после изменения (если null — сохраняем текущий/ALL при удалении текущего)
     * deletedSectionName — имя удалённого раздела, чтобы при необходимости сбросить выбор
     */
    private fun applySectionsChange(selectSection: HabitSectionBase? = null, deletedSectionName: String? = null) {
        // Сохраняем персистентно
        saveCustomSections()
        saveHabits()

        // Обновляем список разделов в шапке
        setupSectionsList()

        // Переключение текущего раздела при удалении выбранного
        if (deletedSectionName != null && currentSection.displayName == deletedSectionName) {
            currentSection = HabitSection.ALL
        }

        val sectionsListLayout = binding.sectionSpinnerLayout.findViewById<View>(R.id.sectionsListLayout)
        val sectionHeaderTextView = sectionsListLayout.findViewById<TextView>(R.id.sectionHeaderTextView)

        val targetSection = selectSection ?: currentSection
        sectionHeaderTextView.text = targetSection.displayName
        filterHabitsBySection(targetSection)

        // Обновляем AddHabitFragment, если он открыт
        val addHabitFragment = supportFragmentManager.findFragmentByTag("AddHabitFragment") as? AddHabitFragment
        addHabitFragment?.updateSelectedSection(targetSection)
        val addHabitView = addHabitFragment?.view
        val addHabitSectionsInclude = addHabitView?.findViewById<View>(R.id.sectionsListLayout)
        if (addHabitSectionsInclude != null) {
            setupSectionsList(addHabitSectionsInclude)
            val addHeader = addHabitSectionsInclude.findViewById<TextView>(R.id.sectionHeaderTextView)
            addHeader.text = targetSection.displayName
        }
    }
    
    /**
     * Фильтрует привычки по выбранному разделу
     */
    private fun filterHabitsBySection(section: HabitSectionBase) {
        if (section == HabitSection.ALL) {
            // Показываем все привычки
            habitAdapter.showAllHabits()
        } else {
            // Фильтруем привычки по разделу
            habitAdapter.filterBySection(section)
        }
    }
    
    private fun setupRecyclerView() {
        // Инициализируем историю прогресса
        progressHistory = HabitProgressHistory(this)
        
        // Инициализируем адаптер
        habitAdapter = HabitAdapter(mutableListOf())
        habitAdapter.listener = this
        binding.habitsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = habitAdapter
        }
        
        // Настраиваем ItemTouchHelper для перетаскивания элементов
        setupItemTouchHelper()
        
        // Загружаем сохраненные привычки или создаем стартовые, если это первый запуск
        val habitsLoaded = loadHabits()
        if (!habitsLoaded) {
            // Если привычки не были загружены (первый запуск), создаем стартовые
            createSampleHabits()
        }
        
        // Показываем все привычки при запуске
        habitAdapter.showAllHabits()
    }
    
    fun saveHabits() {
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        
        sharedPreferences.edit {
            // Сохраняем количество привычек
            putInt("habits_count", habitAdapter.getAllHabitsCount())
            
            // Сохраняем каждую привычку
            habitAdapter.getAllHabits().forEachIndexed { index, habit ->
                putString("habit_${index}_name", habit.name)
                putInt("habit_${index}_type", habit.type.ordinal)
                putInt("habit_${index}_target", habit.target)
                putInt("habit_${index}_current", habit.current)
                putLong("habit_${index}_date", habit.createdDate.time)
                putString("habit_${index}_unit", habit.unit) // Сохраняем единицу измерения
                putString("habit_${index}_section_name", habit.section.displayName) // Сохраняем имя раздела
            }
            
            // Сохраняем дату последнего запуска приложения
            putLong("last_launch_date", System.currentTimeMillis())
        }
    }
    
    private fun loadHabits(): Boolean {
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        val habitsCount = sharedPreferences.getInt("habits_count", 0)
        
        // Если это первый запуск, сохраняем текущую дату
        if (!sharedPreferences.contains("last_launch_date")) {
            sharedPreferences.edit {
                putLong("last_launch_date", System.currentTimeMillis())
            }
        }
        
        if (habitsCount == 0) {
            return false
        }
        
        for (i in 0 until habitsCount) {
            val name = sharedPreferences.getString("habit_${i}_name", "") ?: ""
            val typeOrdinal = sharedPreferences.getInt("habit_${i}_type", 0)
            val target = sharedPreferences.getInt("habit_${i}_target", 0)
            val current = sharedPreferences.getInt("habit_${i}_current", 0)
            val date = sharedPreferences.getLong("habit_${i}_date", System.currentTimeMillis())
            val unit = sharedPreferences.getString("habit_${i}_unit", "") ?: "" // Загружаем единицу измерения
            
            // Загружаем имя раздела и получаем объект раздела
            val sectionName = sharedPreferences.getString("habit_${i}_section_name", HabitSection.ALL.displayName) ?: HabitSection.ALL.displayName
            val section = HabitSection.getSectionByName(sectionName)
            
            val type = HabitType.entries[typeOrdinal]
            
            val habit = Habit(name = name, type = type, target = target, current = current, 
                              createdDate = Date(date), unit = unit, section = section)
            habitAdapter.addHabit(habit)
        }
        
        return true
    }
    
    fun addHabit(name: String, type: HabitType, target: Int, unit: String = "", section: HabitSectionBase = HabitSection.ALL) {
        val habit = Habit(name = name, type = type, target = target, unit = unit, section = section)
        habitAdapter.addHabit(habit)
        saveHabits()
        Toast.makeText(this, getString(R.string.habit_added_format, name), Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Обновляет существующую привычку
     */
    fun updateHabit(position: Int, name: String, type: HabitType, target: Int, unit: String = "", section: HabitSectionBase = HabitSection.ALL) {
        if (position >= 0 && position < habitAdapter.getItemCount()) {
            val oldHabit = habitAdapter.getHabitAt(position)
            // Сохраняем текущий прогресс и дату создания
            val updatedHabit = Habit(
                id = oldHabit.id,
                name = name,
                type = type,
                target = target,
                current = oldHabit.current,
                createdDate = oldHabit.createdDate,
                unit = unit,
                section = section
            )
            habitAdapter.updateHabit(position, updatedHabit)
            saveHabits()
        }
    }
    
    
    // Метод markSimpleHabitAsCompleted используется в onUpdateProgress
    fun markSimpleHabitAsCompleted(position: Int, isCompleted: Boolean) {
        val habit = habitAdapter.getHabitAt(position)
        if (habit.type == HabitType.SIMPLE) {
            val updatedHabit = habit.copy(current = if (isCompleted) 1 else 0)
            habitAdapter.updateHabit(position, updatedHabit)
            saveHabits()
            
            // Добавляем запись в историю прогресса
            progressHistory.addProgressRecord(position, if (isCompleted) 1 else 0)
            
            val message = if (isCompleted) getString(R.string.habit_marked_completed) else getString(R.string.habit_marked_uncompleted)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Проверяет, изменился ли день с момента последнего запуска приложения,
     * и если да, сбрасывает прогресс всех привычек
     */
    private fun checkAndResetHabits() {
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        val lastLaunchDate = sharedPreferences.getLong("last_launch_date", System.currentTimeMillis())
        
        val lastCalendar = java.util.Calendar.getInstance()
        lastCalendar.timeInMillis = lastLaunchDate
        
        val currentCalendar = java.util.Calendar.getInstance()
        
        // Проверяем, изменился ли день
        val lastDay = lastCalendar.get(java.util.Calendar.DAY_OF_YEAR)
        val lastYear = lastCalendar.get(java.util.Calendar.YEAR)
        val currentDay = currentCalendar.get(java.util.Calendar.DAY_OF_YEAR)
        val currentYear = currentCalendar.get(java.util.Calendar.YEAR)
        
        if (currentDay != lastDay || currentYear != lastYear) {
            // День изменился, сбрасываем прогресс
            resetHabitsProgress()
            Toast.makeText(this, getString(R.string.habits_progress_reset), Toast.LENGTH_SHORT).show()
        }
        
        // Обновляем дату последнего запуска
        sharedPreferences.edit {
            putLong("last_launch_date", System.currentTimeMillis())
        }
    }
    
    /**
     * Сбрасывает прогресс всех привычек на 0
     */
    private fun resetHabitsProgress() {
        for (i in 0 until habitAdapter.getAllHabitsCount()) {
            val habit = habitAdapter.getAllHabits()[i]
            val updatedHabit = habit.copy(current = 0)
            // Находим индекс в отфильтрованном списке
            val filteredIndex = habitAdapter.getFilteredIndexById(habit.id)
            if (filteredIndex >= 0) {
                habitAdapter.updateHabit(filteredIndex, updatedHabit)
            } else {
                // Если привычка не в отфильтрованном списке, обновляем только в полном списке
                habitAdapter.updateHabitInAllList(habit.id, updatedHabit)
            }
        }
        saveHabits()
    }
    
    /**
     * Создает тестовые привычки разных типов с историей за неделю
     */
    private fun createSampleHabits() {
        // Создаем пользовательские разделы, если их еще нет
        val healthSection = HabitSection.addCustomSection("Здоровье")
        val sportSection = HabitSection.addCustomSection("Спорт")
        
        // Создаем привычки разных типов
        val timeHabit = Habit(
            name = getString(R.string.meditation), 
            type = HabitType.TIME, 
            target = 15, 
            current = 0, 
            createdDate = Date(),
            section = healthSection
        ) // 15 минут
        
        val repeatHabit = Habit(
            name = getString(R.string.pushups), 
            type = HabitType.REPEAT, 
            target = 20, 
            current = 0, 
            createdDate = Date(),
            unit = getString(R.string.times),
            section = sportSection
        ) // 20 повторений
        
        val simpleHabit = Habit(
            name = getString(R.string.drink_water), 
            type = HabitType.SIMPLE, 
            target = 1, 
            current = 0, 
            createdDate = Date(),
            section = healthSection
        ) // Просто выполнено/не выполнено
        
        // Добавляем привычки в адаптер
        habitAdapter.addHabit(timeHabit)
        habitAdapter.addHabit(repeatHabit)
        habitAdapter.addHabit(simpleHabit)
        
        // Генерируем историю за неделю для каждой привычки
        generateWeekHistory()
        
        // Сохраняем привычки и разделы
        saveHabits()
        saveCustomSections()
    }
    
    /**
     * Генерирует историю прогресса за последние 5 месяцев для всех привычек
     */
    private fun generateWeekHistory() {
        val random = java.util.Random()
        val calendar = java.util.Calendar.getInstance()
        
        // Для каждой привычки
        for (position in 0 until habitAdapter.getAllHabitsCount()) {
            val habit = habitAdapter.getAllHabits()[position]
            
            // Генерируем данные за последние 150 дней (примерно 5 месяцев)
            for (day in 149 downTo 0) {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -day) // день в прошлом
                val timestamp = calendar.timeInMillis
                
                // Генерируем случайное значение в зависимости от типа привычки
                val progress = when (habit.type) {
                    HabitType.TIME -> random.nextInt(habit.target * 2) // от 0 до 2*target минут
                    HabitType.REPEAT -> random.nextInt(habit.target * 2) // от 0 до 2*target повторений
                    HabitType.SIMPLE -> if (random.nextFloat() > 0.3f) 1 else 0 // 70% вероятность выполнения
                }
                
                // Добавляем запись в историю
                progressHistory.addProgressRecord(position, progress, timestamp)
            }
        }
    }
    
    // Реализация интерфейса HabitAdapter.HabitListener
    override fun onDeleteHabit(position: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_habit_title))
            .setMessage(getString(R.string.delete_habit_confirmation))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                habitAdapter.removeHabit(position)
                saveHabits()
                Toast.makeText(this, getString(R.string.habit_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
    
    override fun onEditHabit(position: Int) {
        if (position >= 0 && position < habitAdapter.getItemCount()) {
            val habit = habitAdapter.getHabitAt(position)
            // Создаем и показываем AddHabitFragment в режиме редактирования
            val addHabitFragment = AddHabitFragment.newInstance(position, habit)
            addHabitFragment.show(supportFragmentManager, "AddHabitFragment")
        }
    }
    
    override fun onUpdateProgress(position: Int, count: Int) {
        val habit = habitAdapter.getHabitAt(position)
        
        when (habit.type) {
            HabitType.TIME -> {
                val updatedHabit = habit.copy(current = count)
                habitAdapter.updateHabit(position, updatedHabit)
                saveHabits()
                // Добавляем запись в историю прогресса
                progressHistory.addProgressRecord(position, count)
                Toast.makeText(this, getString(R.string.progress_updated_minutes, count), Toast.LENGTH_SHORT).show()
            }
            HabitType.REPEAT -> {
                // Для повторений используем значение count как количество повторений
                val updatedHabit = habit.copy(current = count)
                habitAdapter.updateHabit(position, updatedHabit)
                saveHabits()
                // Добавляем запись в историю прогресса
                progressHistory.addProgressRecord(position, count)
                val unitText = if (habit.unit.isNotEmpty()) habit.unit else getString(R.string.quantity)
                Toast.makeText(this, getString(R.string.progress_updated_format, count, unitText), Toast.LENGTH_SHORT).show()
            }
            HabitType.SIMPLE -> {
                // Используем метод markSimpleHabitAsCompleted для обработки простых привычек
                markSimpleHabitAsCompleted(position, count > 0)
            }
        }
    }
    
    override fun onShowChart(position: Int) {
        // Проверяем, что позиция валидна
        if (position < 0 || position >= habitAdapter.getItemCount()) {
            Toast.makeText(this, getString(R.string.error_invalid_position), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Скрываем весь контейнер с разделами
        binding.sectionSpinnerLayout.visibility = View.GONE
        
        val habitChartFragment = HabitChartFragment.newInstance(position)
        supportFragmentManager.commit {
            replace(R.id.fragment_container, habitChartFragment)
            addToBackStack("chart")
        }
        // Показываем контейнер фрагмента через публичный метод
        showFragmentContainer()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            // Получаем имя текущего фрагмента в стеке
            val fragmentTag = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name
            
            // Если это фрагмент задач или заметок, возвращаемся к списку привычек
            if (fragmentTag == "tasks" || fragmentTag == "notes") {
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                showHabitList()
                // Устанавливаем кнопку "Привычки" как выбранную
                binding.TypeGroup.check(R.id.Habits)
            } else {
                // Для других фрагментов (например, графика привычки) просто возвращаемся назад
                supportFragmentManager.popBackStack()
                // Если больше нет фрагментов в стеке, показываем список привычек и возвращаем раздел
                if (supportFragmentManager.backStackEntryCount == 0) {
                    showHabitList()
                    
                    // Возвращаем видимость контейнера с разделами
                    binding.sectionSpinnerLayout.visibility = View.VISIBLE
                }
            }
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onPause() {
        super.onPause()
        saveHabits()
        saveCustomSections()
        
        // Сохраняем текущий раздел
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        sharedPreferences.edit {
            putString("current_section", HabitSection.ALL.displayName)
        }
    }
    
    // Настройка ItemTouchHelper для перетаскивания элементов
    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // Направления для перетаскивания
            0 // Направления для свайпа (не используем)
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                
                // Перемещаем элемент в адаптере
                habitAdapter.moveHabit(fromPosition, toPosition)
                
                // Сохраняем изменения
                saveHabits()
                
                return true
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Не используем свайп
            }
            
            // Метод для изменения внешнего вида элемента при перетаскивании
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.7f
                }
            }
            
            // Метод для восстановления внешнего вида элемента после перетаскивания
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                
                viewHolder.itemView.alpha = 1.0f
            }
        }
        
        // Прикрепляем ItemTouchHelper к RecyclerView
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.habitsRecyclerView)
        
        // Устанавливаем слушатель перетаскивания
        habitAdapter.dragListener = this
    }
    
    // Реализация метода из интерфейса OnStartDragListener
    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        // Запускаем перетаскивание
        itemTouchHelper.startDrag(viewHolder)
    }
    
    /**
     * Настраивает кнопки навигации между разделами приложения
     */
    private fun setupNavigationButtons() {
        // По умолчанию выбрана кнопка "Привычки"
        binding.TypeGroup.check(R.id.Habits)
        
        // Кнопка "Привычки"
        binding.Habits.setOnClickListener {
            // Показываем список привычек
            showHabitList()
            // Если есть фрагменты в стеке, очищаем их
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            // Устанавливаем кнопку "Привычки" как выбранную
            binding.TypeGroup.check(R.id.Habits)
            
            // Показываем контейнер с разделами
            binding.sectionSpinnerLayout.visibility = View.VISIBLE
        }
        
        // Кнопка "Заметки"
        binding.Notes.setOnClickListener {
            // Скрываем контейнер с разделами
            binding.sectionSpinnerLayout.visibility = View.GONE
            
            // Создаем и показываем фрагмент заметок
            val notesFragment = NotesFragment.newInstance()
            supportFragmentManager.commit {
                replace(R.id.fragment_container, notesFragment)
                addToBackStack("notes")
            }
            // Показываем контейнер фрагмента
            showFragmentContainer()
            // Устанавливаем кнопку "Заметки" как выбранную
            binding.TypeGroup.check(R.id.Notes)
        }
    }

    /**
     * Реализация интерфейса OnSectionAddedListener
     */
    override fun onSectionAdded(sectionName: String) {
        // Добавляем новый раздел
        val newSection = HabitSection.addCustomSection(sectionName)
        
        // Сохраняем пользовательские разделы
        applySectionsChange(selectSection = newSection, deletedSectionName = null)
        // Показываем сообщение
        Toast.makeText(this, getString(R.string.section_added), Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Сохраняет пользовательские разделы
     */
    fun saveCustomSections() {
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        val sectionNames = HabitSection.getCustomSectionNames()
        
        sharedPreferences.edit {
            // Сохраняем количество пользовательских разделов
            putInt("custom_sections_count", sectionNames.size)
            
            // Сохраняем каждый раздел
            sectionNames.forEachIndexed { index, name ->
                putString("custom_section_${index}", name)
            }
        }
    }
    
    /**
     * Загружает пользовательские разделы
     */
    private fun loadCustomSections() {
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        val count = sharedPreferences.getInt("custom_sections_count", -1)
        
        if (count > 0) {
            // Если есть сохраненные разделы, загружаем их
            val sectionNames = mutableListOf<String>()
            
            for (i in 0 until count) {
                val name = sharedPreferences.getString("custom_section_${i}", null)
                if (name != null) {
                    sectionNames.add(name)
                }
            }
            
            // Загружаем пользовательские разделы
            HabitSection.loadCustomSections(sectionNames)
        } else if (count == -1) {
            // Это первый запуск, создаем стандартные разделы
            HabitSection.createDefaultSections()
            
            // Сохраняем пользовательские разделы
            saveCustomSections()
            
            // Устанавливаем флаг, что это уже не первый запуск
            sharedPreferences.edit {
                putInt("custom_sections_count", HabitSection.getCustomSectionNames().size)
            }
        }
    }

}