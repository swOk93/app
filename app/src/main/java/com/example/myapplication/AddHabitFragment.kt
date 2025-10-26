package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.myapplication.databinding.FragmentAddHabitBinding

/**
 * A [DialogFragment] subclass for adding or editing habits.
 */
class AddHabitFragment : DialogFragment() {

    private var _binding: FragmentAddHabitBinding? = null
    private var currentHabitType: HabitType = HabitType.SIMPLE
    private var currentHabitSection: HabitSectionBase = HabitSection.ALL
    
    // Параметры для редактирования существующей привычки
    private var isEditMode = false
    private var habitPosition = -1
    private var habitName = ""
    private var habitTarget = 0
    private var habitUnit = "" // Добавляем переменную для единицы измерения
    private var habitSectionName = "" // Имя раздела привычки
    
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddHabitBinding.inflate(inflater, container, false)
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
        
        // Настройка выпадающего списка разделов
        setupSectionSpinner()
        
        // Получаем аргументы, если они есть
        arguments?.let { args ->
            isEditMode = args.getBoolean(ARG_IS_EDIT_MODE, false)
            if (isEditMode) {
                habitPosition = args.getInt(ARG_HABIT_POSITION, -1)
                habitName = args.getString(ARG_HABIT_NAME, "")
                habitTarget = args.getInt(ARG_HABIT_TARGET, 0)
                habitUnit = args.getString(ARG_HABIT_UNIT, "") // Получаем единицу измерения
                currentHabitType = HabitType.entries[args.getInt(ARG_HABIT_TYPE, 0)]
                // Получаем имя раздела привычки
                habitSectionName = args.getString(ARG_HABIT_SECTION_NAME, HabitSection.ALL.displayName)
                currentHabitSection = HabitSection.getSectionByName(habitSectionName)
                
                // Изменяем заголовок диалога
                binding.addHabitTitleTextView.text = getString(R.string.edit_habit)
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
            binding.habitNameEditText.setText(getString(R.string.habit))
            binding.hoursEditText.setText(getString(R.string.one))
            binding.minutesEditText.setText(getString(R.string.thirty))
            binding.repeatCountEditText.setText(getString(R.string.ten))
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
    
    /**
     * Настройка выпадающего списка разделов
     */
    private fun setupSectionSpinner() {
        // Используем включенный макет для списка разделов
        val sectionsListLayout = binding.sectionsListLayout
        // Настройка списка разделов через MainActivity или другой подходящий метод
        (activity as? MainActivity)?.setupSectionsList(sectionsListLayout.root)
    }

    private fun saveHabit() {
        val habitName = binding.habitNameEditText.text.toString()
        if (habitName.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_habit_name), Toast.LENGTH_SHORT).show()
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
            (activity as? MainActivity)?.updateHabit(habitPosition, habitName, currentHabitType, targetValue, unitValue, currentHabitSection)
            Toast.makeText(requireContext(), getString(R.string.habit_updated), Toast.LENGTH_SHORT).show()
        } else {
            // Добавляем новую привычку
            (activity as? MainActivity)?.addHabit(habitName, currentHabitType, targetValue, unitValue, currentHabitSection)
        }

        // Закрываем диалог
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * Обновляет выбранный раздел
     */
    fun updateSelectedSection(section: HabitSectionBase) {
        currentHabitSection = section
    }
    
    /**
     * Возвращает имя текущего выбранного раздела
     */
    fun getCurrentSectionName(): String {
        return currentHabitSection.displayName
    }
    
    companion object {
        private const val ARG_IS_EDIT_MODE = "is_edit_mode"
        private const val ARG_HABIT_POSITION = "habit_position"
        private const val ARG_HABIT_NAME = "habit_name"
        private const val ARG_HABIT_TYPE = "habit_type"
        private const val ARG_HABIT_TARGET = "habit_target"
        private const val ARG_HABIT_UNIT = "habit_unit" // Константа для единицы измерения
        private const val ARG_HABIT_SECTION_NAME = "habit_section_name" // Константа для имени раздела
        
        /**
         * Создает новый экземпляр AddHabitFragment для редактирования существующей привычки
         */
        fun newInstance(position: Int, habit: Habit): AddHabitFragment {
            val fragment = AddHabitFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_EDIT_MODE, true)
            args.putInt(ARG_HABIT_POSITION, position)
            args.putString(ARG_HABIT_NAME, habit.name)
            args.putInt(ARG_HABIT_TYPE, habit.type.ordinal)
            args.putInt(ARG_HABIT_TARGET, habit.target)
            args.putString(ARG_HABIT_UNIT, habit.unit)
            args.putString(ARG_HABIT_SECTION_NAME, habit.section.displayName)
            fragment.arguments = args
            return fragment
        }
    }
} 