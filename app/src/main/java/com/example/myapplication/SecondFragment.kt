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
 * A simple [DialogFragment] subclass for adding new habits.
 */
class SecondFragment : DialogFragment() {

    private var _binding: FragmentSecondBinding? = null
    private var currentHabitType: HabitType = HabitType.SIMPLE

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

        // Устанавливаем значения по умолчанию
        binding.habitNameEditText.setText("привычка")
        binding.hoursEditText.setText("1")
        binding.minutesEditText.setText("30")
        binding.repeatCountEditText.setText("10")
        
        // По умолчанию выбираем простую привычку
        binding.simpleRadioButton.isChecked = true

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

        // Добавляем привычку через MainActivity
        (activity as? MainActivity)?.addHabit(habitName, currentHabitType, targetValue)

        // Закрываем диалог
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}