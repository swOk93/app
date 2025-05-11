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

        // Настройка кнопки добавления привычки
        binding.addButton.setOnClickListener {
            val habitText: String = binding.habitEditText.text.toString()
            if (habitText.isNotEmpty()) {
                addHabit(habitText, currentHabitType, targetValue)
                binding.habitEditText.text.clear()
                targetValue = 0
                binding.minutesValueTextView.text = "0"
            } else {
                Toast.makeText(this, "Введите название привычки", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Настройка кнопок типов привычек
        binding.timeButton.setOnClickListener {
            currentHabitType = HabitType.TIME
            showTargetInputDialog("Введите целевое количество минут")
        }
        
        binding.repeatButton.setOnClickListener {
            currentHabitType = HabitType.REPEAT
            showTargetInputDialog("Введите целевое количество повторений")
        }
        
        binding.simpleButton.setOnClickListener {
            currentHabitType = HabitType.SIMPLE
            targetValue = 1
            binding.minutesValueTextView.text = "Простая привычка"
            Toast.makeText(this, "Выбран тип: Простая", Toast.LENGTH_SHORT).show()
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
        habitAdapter = HabitAdapter(mutableListOf())
        habitAdapter.listener = this
        binding.habitsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = habitAdapter
        }
    }
    
    private fun addHabit(name: String, type: HabitType, target: Int) {
        val habit = Habit(name = name, type = type, target = target)
        habitAdapter.addHabit(habit)
        Toast.makeText(this, "Привычка добавлена: $name", Toast.LENGTH_SHORT).show()
    }
    
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
                    binding.minutesValueTextView.text = targetValue.toString()
                }
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }
            .show()
    }
    
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
                Toast.makeText(this, "Привычка удалена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Нет", null)
            .show()
    }
    
    override fun onUpdateProgress(position: Int, hours: Int, minutes: Int) {
        val habit = habitAdapter.habits[position]
        
        when (habit.type) {
            HabitType.TIME -> {
                // Преобразуем часы и минуты в общее количество минут
                val totalMinutes = hours * 60 + minutes
                val updatedHabit = habit.copy(current = totalMinutes)
                habitAdapter.updateHabit(position, updatedHabit)
                Toast.makeText(this, "Прогресс обновлен: $totalMinutes минут", Toast.LENGTH_SHORT).show()
            }
            HabitType.REPEAT -> {
                // Для повторений используем только значение часов как количество повторений
                val updatedHabit = habit.copy(current = hours)
                habitAdapter.updateHabit(position, updatedHabit)
                Toast.makeText(this, "Прогресс обновлен: $hours повторений", Toast.LENGTH_SHORT).show()
            }
            HabitType.SIMPLE -> {
                if (hours > 0) {
                    // Отмечаем как выполненную
                    val updatedHabit = habit.copy(current = 1)
                    habitAdapter.updateHabit(position, updatedHabit)
                    Toast.makeText(this, "Привычка отмечена как выполненная", Toast.LENGTH_SHORT).show()
                } else {
                    // Отмечаем как невыполненную
                    val updatedHabit = habit.copy(current = 0)
                    habitAdapter.updateHabit(position, updatedHabit)
                    Toast.makeText(this, "Привычка отмечена как невыполненная", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}