package com.example.myapplication

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ActivityMainBinding
import java.util.Date
import androidx.fragment.app.commit
import androidx.core.content.edit
import android.view.View

class MainActivity : AppCompatActivity(), HabitAdapter.HabitListener, HabitAdapter.OnStartDragListener {

    private lateinit var binding: ActivityMainBinding
    public lateinit var habitAdapter: HabitAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    public lateinit var progressHistory: HabitProgressHistory
    
    // Публичные методы для управления видимостью контейнеров
    /**
     * Показывает список привычек и скрывает контейнер фрагментов
     */
    fun showHabitList() {
        binding.fragmentContainer.visibility = View.GONE
        binding.habitsRecyclerView.visibility = View.VISIBLE
        binding.addTaskButton.visibility = View.VISIBLE // Показываем кнопку добавления привычки
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

        // Инициализация RecyclerView
        setupRecyclerView()

        // Настройка кнопки добавления задачи
        binding.addTaskButton.setOnClickListener {
            // Показываем SecondFragment как диалог
            val secondFragment = SecondFragment()
            secondFragment.show(supportFragmentManager, "SecondFragment")
        }
        
        // Настройка кнопок переключения между разделами
        setupNavigationButtons()
        
        // Привычки уже загружены или созданы в setupRecyclerView()
        
        // Проверяем, нужно ли сбросить прогресс привычек
        checkAndResetHabits()
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
    }
    
    private fun saveHabits() {
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        
        sharedPreferences.edit {
            // Сохраняем количество привычек
            putInt("habits_count", habitAdapter.habits.size)
            
            // Сохраняем каждую привычку
            habitAdapter.habits.forEachIndexed { index, habit ->
                putString("habit_${index}_name", habit.name)
                putInt("habit_${index}_type", habit.type.ordinal)
                putInt("habit_${index}_target", habit.target)
                putInt("habit_${index}_current", habit.current)
                putLong("habit_${index}_date", habit.createdDate.time)
                putString("habit_${index}_unit", habit.unit) // Сохраняем единицу измерения
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
            
            val type = HabitType.entries[typeOrdinal]
            
            val habit = Habit(name = name, type = type, target = target, current = current, createdDate = Date(date), unit = unit)
            habitAdapter.addHabit(habit)
        }
        
        return true
    }
    
    fun addHabit(name: String, type: HabitType, target: Int, unit: String = "") {
        val habit = Habit(name = name, type = type, target = target, unit = unit)
        habitAdapter.addHabit(habit)
        saveHabits()
        Toast.makeText(this, getString(R.string.habit_added_format, name), Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Обновляет существующую привычку
     */
    fun updateHabit(position: Int, name: String, type: HabitType, target: Int, unit: String = "") {
        if (position >= 0 && position < habitAdapter.habits.size) {
            val oldHabit = habitAdapter.habits[position]
            // Сохраняем текущий прогресс и дату создания
            val updatedHabit = Habit(
                name = name,
                type = type,
                target = target,
                current = oldHabit.current,
                createdDate = oldHabit.createdDate,
                unit = unit
            )
            habitAdapter.updateHabit(position, updatedHabit)
            saveHabits()
        }
    }
    
    
    // Метод markSimpleHabitAsCompleted используется в onUpdateProgress
    fun markSimpleHabitAsCompleted(position: Int, isCompleted: Boolean) {
        val habit = habitAdapter.habits[position]
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
        for (i in 0 until habitAdapter.habits.size) {
            val habit = habitAdapter.habits[i]
            val updatedHabit = habit.copy(current = 0)
            habitAdapter.updateHabit(i, updatedHabit)
        }
        saveHabits()
    }
    
    /**
     * Создает три тестовые привычки разных типов с историей за неделю
     */
    private fun createSampleHabits() {
        // Создаем привычки разных типов
        val timeHabit = Habit(
            name = getString(R.string.meditation), 
            type = HabitType.TIME, 
            target = 15, 
            current = 0, 
            createdDate = Date()
        ) // 15 минут
        
        val repeatHabit = Habit(
            name = getString(R.string.pushups), 
            type = HabitType.REPEAT, 
            target = 20, 
            current = 0, 
            createdDate = Date(),
            unit = getString(R.string.times)
        ) // 20 повторений
        
        val simpleHabit = Habit(
            name = getString(R.string.drink_water), 
            type = HabitType.SIMPLE, 
            target = 1, 
            current = 0, 
            createdDate = Date()
        ) // Просто выполнено/не выполнено
        
        // Добавляем привычки в адаптер
        habitAdapter.addHabit(timeHabit)
        habitAdapter.addHabit(repeatHabit)
        habitAdapter.addHabit(simpleHabit)
        
        // Генерируем историю за неделю для каждой привычки
        generateWeekHistory()
        
        // Сохраняем привычки
        saveHabits()
    }
    
    /**
     * Генерирует историю прогресса за последние 5 месяцев для всех привычек
     */
    private fun generateWeekHistory() {
        val random = java.util.Random()
        val calendar = java.util.Calendar.getInstance()
        
        // Для каждой привычки
        for (position in 0 until habitAdapter.habits.size) {
            val habit = habitAdapter.habits[position]
            
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
        if (position >= 0 && position < habitAdapter.habits.size) {
            val habit = habitAdapter.habits[position]
            // Создаем и показываем SecondFragment в режиме редактирования
            val secondFragment = SecondFragment.newInstance(position, habit)
            secondFragment.show(supportFragmentManager, "SecondFragment")
        }
    }
    
    override fun onUpdateProgress(position: Int, count: Int) {
        val habit = habitAdapter.habits[position]
        
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
        val habitChartFragment = HabitChartFragment.newInstance(position)
        supportFragmentManager.commit {
            replace(R.id.fragment_container, habitChartFragment)
            addToBackStack("chart")
        }
        // Показываем контейнер фрагмента через публичный метод
        showFragmentContainer()
    }
    
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
                // Если больше нет фрагментов в стеке, показываем список привычек
                if (supportFragmentManager.backStackEntryCount == 0) {
                    showHabitList()
                }
            }
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onPause() {
        super.onPause()
        saveHabits()
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
        }
        
        // Кнопка "Заметки"
        binding.Notes.setOnClickListener {
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

}