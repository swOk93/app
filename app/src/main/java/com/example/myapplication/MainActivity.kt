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

class MainActivity : AppCompatActivity(), HabitAdapter.HabitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var habitAdapter: HabitAdapter
    private var currentHabitType: HabitType = HabitType.SIMPLE
    private var targetValue: Int = 0

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
        habitAdapter = HabitAdapter(loadHabits())
        habitAdapter.listener = this
        binding.habitsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = habitAdapter
        }
    }
    
    private fun saveHabits() {
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        
        // Сохраняем количество привычек
        editor.putInt("habits_count", habitAdapter.habits.size)
        
        // Сохраняем каждую привычку
        habitAdapter.habits.forEachIndexed { index, habit ->
            editor.putString("habit_${index}_name", habit.name)
            editor.putInt("habit_${index}_type", habit.type.ordinal)
            editor.putInt("habit_${index}_target", habit.target)
            editor.putInt("habit_${index}_current", habit.current)
            editor.putLong("habit_${index}_date", habit.createdDate.time)
        }
        
        editor.apply()
    }
    
    private fun loadHabits(): MutableList<Habit> {
        val habits = mutableListOf<Habit>()
        val sharedPreferences = getSharedPreferences("HabitsPrefs", MODE_PRIVATE)
        
        val habitsCount = sharedPreferences.getInt("habits_count", 0)
        
        for (i in 0 until habitsCount) {
            val name = sharedPreferences.getString("habit_${i}_name", "") ?: ""
            val typeOrdinal = sharedPreferences.getInt("habit_${i}_type", 0)
            val target = sharedPreferences.getInt("habit_${i}_target", 0)
            val current = sharedPreferences.getInt("habit_${i}_current", 0)
            val date = sharedPreferences.getLong("habit_${i}_date", System.currentTimeMillis())
            
            val type = HabitType.values()[typeOrdinal]
            
            habits.add(Habit(name = name, type = type, target = target, current = current, createdDate = Date(date)))
        }
        
        return habits
    }
    
    fun addHabit(name: String, type: HabitType, target: Int) {
        val habit = Habit(name = name, type = type, target = target)
        habitAdapter.addHabit(habit)
        saveHabits()
        Toast.makeText(this, "Привычка добавлена: $name", Toast.LENGTH_SHORT).show()
    }
    
    // Этот метод больше не используется в новом интерфейсе
    /*
    private fun showTargetInputDialog(title: String) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val inputText = input.text.toString()
                if (inputText.isNotEmpty()) {
                    targetValue = inputText.toInt()
                    // binding.minutesValueTextView.text = targetValue.toString()
                }
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }
            .show()
    }
    */
    
    private fun showProgressUpdateDialog(position: Int, habit: Habit) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        
        val title = when (habit.type) {
            HabitType.TIME -> "Введите количество минут"
            HabitType.REPEAT -> "Введите количество повторений"
            HabitType.SIMPLE -> return markSimpleHabitAsCompleted(position, habit)
        }
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val inputText = input.text.toString()
                if (inputText.isNotEmpty()) {
                    val newValue = inputText.toInt()
                    val updatedHabit = habit.copy(current = habit.current + newValue)
                    habitAdapter.updateHabit(position, updatedHabit)
                }
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }
            .show()
    }
    
    private fun markSimpleHabitAsCompleted(position: Int, habit: Habit) {
        val updatedHabit = habit.copy(current = 1)
        habitAdapter.updateHabit(position, updatedHabit)
        Toast.makeText(this, "Привычка отмечена как выполненная", Toast.LENGTH_SHORT).show()
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
        val progressHistory = HabitProgressHistory(this)
        
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
                if (count > 0) {
                    // Отмечаем как выполненную
                    val updatedHabit = habit.copy(current = 1)
                    habitAdapter.updateHabit(position, updatedHabit)
                    saveHabits()
                    // Добавляем запись в историю прогресса
                    progressHistory.addProgressRecord(position, 1)
                    Toast.makeText(this, "Привычка отмечена как выполненная", Toast.LENGTH_SHORT).show()
                } else {
                    // Отмечаем как невыполненную
                    val updatedHabit = habit.copy(current = 0)
                    habitAdapter.updateHabit(position, updatedHabit)
                    saveHabits()
                    // Добавляем запись в историю прогресса
                    progressHistory.addProgressRecord(position, 0)
                    Toast.makeText(this, "Привычка отмечена как невыполненная", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onShowChart(position: Int) {
        val habit = habitAdapter.habits[position]
        val chartFragment = HabitChartFragment.newInstance(position, habit.name)
        chartFragment.show(supportFragmentManager, "habitChart")
    }

}