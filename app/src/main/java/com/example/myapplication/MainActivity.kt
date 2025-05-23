package com.example.myapplication

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.SecondFragment
import java.util.Date
import androidx.fragment.app.commit
import androidx.core.content.edit
import android.view.View

class MainActivity : AppCompatActivity(), HabitAdapter.HabitListener {

    private lateinit var binding: ActivityMainBinding
    public lateinit var habitAdapter: HabitAdapter
    public lateinit var progressHistory: HabitProgressHistory

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
        
        // Настройка кнопки прогресса
        binding.progressButton.setOnClickListener {
            val completedCount = habitAdapter.habits.count { it.isCompleted() }
            val totalCount = habitAdapter.habits.size
            val message = if (totalCount > 0) {
                "Выполнено $completedCount из $totalCount привычек (${(completedCount * 100 / totalCount)}%)"
            } else {
                "Нет добавленных привычек"
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        
        // Загружаем сохраненные привычки или создаем тестовые, если их нет
        if (habitAdapter.habits.isEmpty()) {
            createSampleHabits()
        }
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
                Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show()
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
        
        // Загружаем сохраненные привычки
        loadHabits()
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
            }
        }
    }
    
    private fun loadHabits(): Boolean {
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        val habitsCount = sharedPreferences.getInt("habits_count", 0)
        
        if (habitsCount == 0) {
            return false
        }
        
        for (i in 0 until habitsCount) {
            val name = sharedPreferences.getString("habit_${i}_name", "") ?: ""
            val typeOrdinal = sharedPreferences.getInt("habit_${i}_type", 0)
            val target = sharedPreferences.getInt("habit_${i}_target", 0)
            val current = sharedPreferences.getInt("habit_${i}_current", 0)
            val date = sharedPreferences.getLong("habit_${i}_date", System.currentTimeMillis())
            
            val type = HabitType.entries[typeOrdinal]
            
            val habit = Habit(name = name, type = type, target = target, current = current, createdDate = Date(date))
            habitAdapter.addHabit(habit)
        }
        
        return true
    }
    
    fun addHabit(name: String, type: HabitType, target: Int) {
        val habit = Habit(name = name, type = type, target = target)
        habitAdapter.addHabit(habit)
        saveHabits()
        Toast.makeText(this, "Привычка добавлена: $name", Toast.LENGTH_SHORT).show()
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
            
            val message = if (isCompleted) "Привычка отмечена как выполненная" else "Привычка отмечена как невыполненная"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Создает три тестовые привычки разных типов с историей за неделю
     */
    private fun createSampleHabits() {
        // Создаем привычки разных типов
        val timeHabit = Habit(
            name = "Медитация", 
            type = HabitType.TIME, 
            target = 15, 
            current = 0, 
            createdDate = Date()
        ) // 15 минут
        
        val repeatHabit = Habit(
            name = "Отжимания", 
            type = HabitType.REPEAT, 
            target = 20, 
            current = 0, 
            createdDate = Date()
        ) // 20 повторений
        
        val simpleHabit = Habit(
            name = "Пить воду", 
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
     * Генерирует историю прогресса за последнюю неделю для всех привычек
     */
    private fun generateWeekHistory() {
        val random = java.util.Random()
        val calendar = java.util.Calendar.getInstance()
        
        // Для каждой привычки
        for (position in 0 until habitAdapter.habits.size) {
            val habit = habitAdapter.habits[position]
            
            // Генерируем данные за последние 7 дней
            for (day in 6 downTo 0) {
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
                if (progress > 0) {
                    progressHistory.addProgressRecord(position, progress, timestamp)
                }
            }
        }
    }
    
    // Реализация интерфейса HabitAdapter.HabitListener
    override fun onDeleteHabit(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удаление привычки")
            .setMessage("Вы уверены, что хотите удалить эту привычку?")
            .setPositiveButton("Да") { _, _ ->
                habitAdapter.removeHabit(position)
                saveHabits()
                Toast.makeText(this, "Привычка удалена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Нет", null)
            .show()
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
                Toast.makeText(this, "Прогресс обновлен: $count минут", Toast.LENGTH_SHORT).show()
            }
            HabitType.REPEAT -> {
                // Для повторений используем значение count как количество повторений
                val updatedHabit = habit.copy(current = count)
                habitAdapter.updateHabit(position, updatedHabit)
                saveHabits()
                // Добавляем запись в историю прогресса
                progressHistory.addProgressRecord(position, count)
                Toast.makeText(this, "Прогресс обновлен: $count повторений", Toast.LENGTH_SHORT).show()
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
        // Показываем контейнер фрагмента и скрываем RecyclerView
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.habitsRecyclerView.visibility = View.GONE
    }
    
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            // Если есть фрагменты в стеке, возвращаемся к списку привычек
            supportFragmentManager.popBackStack()
            binding.fragmentContainer.visibility = View.GONE
            binding.habitsRecyclerView.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }

}