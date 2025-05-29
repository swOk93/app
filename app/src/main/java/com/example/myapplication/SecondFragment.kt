package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.myapplication.databinding.FragmentSecondBinding
import com.example.myapplication.HabitType

/**
 * A [DialogFragment] subclass for adding or editing habits.
 */
class SecondFragment : DialogFragment() {

    private var _binding: FragmentSecondBinding? = null
    private var currentHabitType: HabitType = HabitType.SIMPLE
    
    // Параметры для редактирования существующей привычки
    private var isEditMode = false
    private var habitPosition = -1
    private var habitName = ""
    private var habitTarget = 0
    private var habitUnit = "" // Добавляем переменную для единицы измерения
    
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onStart() {
        super.onStart()
        // Устанавливаем размер диалога на всю ширину экрана
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Получаем аргументы, если они есть
        arguments?.let { args ->
            isEditMode = args.getBoolean(ARG_IS_EDIT_MODE, false)
            if (isEditMode) {
                habitPosition = args.getInt(ARG_HABIT_POSITION, -1)
                habitName = args.getString(ARG_HABIT_NAME, "")
                habitTarget = args.getInt(ARG_HABIT_TARGET, 0)
                habitUnit = args.getString(ARG_HABIT_UNIT, "") // Получаем единицу измерения
                currentHabitType = HabitType.entries[args.getInt(ARG_HABIT_TYPE, 0)]
                
                // Изменяем заголовок диалога
                binding.addHabitTitleTextView.text = "Редактирование привычки"
            }
        }

        // Настройка радиокнопок типа привычки
        binding.habitTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.timeRadioButton -> {
                    currentHabitType = HabitType.TIME
                    binding.timeSettingsLayout.visibility = View.VISIBLE
                    binding.repeatSettingsLayout.visibility = View.GONE
                }
                R.id.repeatRadioButton -> {
                    currentHabitType = HabitType.REPEAT
                    binding.timeSettingsLayout.visibility = View.GONE
                    binding.repeatSettingsLayout.visibility = View.VISIBLE
                }
                R.id.simpleRadioButton -> {
                    currentHabitType = HabitType.SIMPLE
                    binding.timeSettingsLayout.visibility = View.GONE
                    binding.repeatSettingsLayout.visibility = View.GONE
                }
            }
        }

        // Устанавливаем значения по умолчанию или из редактируемой привычки
        if (isEditMode) {
            binding.habitNameEditText.setText(habitName)
            
            // Устанавливаем тип привычки
            when (currentHabitType) {
                HabitType.TIME -> {
                    binding.timeRadioButton.isChecked = true
                    val hours = habitTarget / 60
                    val minutes = habitTarget % 60
                    binding.hoursEditText.setText(hours.toString())
                    binding.minutesEditText.setText(minutes.toString())
                }
                HabitType.REPEAT -> {
                    binding.repeatRadioButton.isChecked = true
                    binding.repeatCountEditText.setText(habitTarget.toString())
                    binding.repeatUnitEditText.setText(habitUnit) // Устанавливаем единицу измерения
                }
                HabitType.SIMPLE -> {
                    binding.simpleRadioButton.isChecked = true
                }
            }
        } else {
            // Значения по умолчанию для новой привычки
            binding.habitNameEditText.setText("привычка")
            binding.hoursEditText.setText("1")
            binding.minutesEditText.setText("30")
            binding.repeatCountEditText.setText("10")
            binding.repeatUnitEditText.setText("") // Пустая единица измерения по умолчанию
            
            // По умолчанию выбираем простую привычку
            binding.simpleRadioButton.isChecked = true
        }

        // Настройка кнопки отмены
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        // Настройка кнопки сохранения
        binding.saveButton.setOnClickListener {
            saveHabit()
        }
    }

    private fun saveHabit() {
        val habitName = binding.habitNameEditText.text.toString()
        if (habitName.isEmpty()) {
            Toast.makeText(requireContext(), "Введите название привычки", Toast.LENGTH_SHORT).show()
            return
        }

        val targetValue = when (currentHabitType) {
            HabitType.TIME -> {
                val hours = binding.hoursEditText.text.toString().toIntOrNull() ?: 0
                val minutes = binding.minutesEditText.text.toString().toIntOrNull() ?: 0
                hours * 60 + minutes
            }
            HabitType.REPEAT -> {
                binding.repeatCountEditText.text.toString().toIntOrNull() ?: 0
            }
            HabitType.SIMPLE -> 1
        }
        
        // Получаем единицу измерения для привычек типа REPEAT
        val unitValue = if (currentHabitType == HabitType.REPEAT) {
            binding.repeatUnitEditText.text.toString()
        } else {
            ""
        }

        if (isEditMode && habitPosition >= 0) {
            // Обновляем существующую привычку
            (activity as? MainActivity)?.updateHabit(habitPosition, habitName, currentHabitType, targetValue, unitValue)
            Toast.makeText(requireContext(), "Привычка обновлена", Toast.LENGTH_SHORT).show()
        } else {
            // Добавляем новую привычку
            (activity as? MainActivity)?.addHabit(habitName, currentHabitType, targetValue, unitValue)
        }

        // Закрываем диалог
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_IS_EDIT_MODE = "is_edit_mode"
        private const val ARG_HABIT_POSITION = "habit_position"
        private const val ARG_HABIT_NAME = "habit_name"
        private const val ARG_HABIT_TYPE = "habit_type"
        private const val ARG_HABIT_TARGET = "habit_target"
        private const val ARG_HABIT_UNIT = "habit_unit" // Добавляем константу для единицы измерения
        
        /**
         * Создает новый экземпляр SecondFragment для редактирования существующей привычки
         */
        fun newInstance(position: Int, habit: Habit): SecondFragment {
            val fragment = SecondFragment()
            val args = Bundle().apply {
                putBoolean(ARG_IS_EDIT_MODE, true)
                putInt(ARG_HABIT_POSITION, position)
                putString(ARG_HABIT_NAME, habit.name)
                putInt(ARG_HABIT_TYPE, habit.type.ordinal)
                putInt(ARG_HABIT_TARGET, habit.target)
                putString(ARG_HABIT_UNIT, habit.unit) // Сохраняем единицу измерения
            }
            fragment.arguments = args
            return fragment
        }
    }
}