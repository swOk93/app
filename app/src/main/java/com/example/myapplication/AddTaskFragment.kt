package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.FragmentAddTaskBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskDao: TaskDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskDao = AppDatabase.getInstance(requireContext()).taskDao()
        setupTaskTypeRadioGroup()
        setupDatePicker()
        setupSaveButton()
    }

    private fun setupTaskTypeRadioGroup() {
        binding.taskTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.deadlineLayout.visibility = when (checkedId) {
                R.id.oneTimeTaskRadioButton -> View.VISIBLE
                else -> View.GONE
            }
        }
    }

    private fun setupDatePicker() {
        binding.deadlineDateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                binding.deadlineDateEditText.setText("$selectedDay.${selectedMonth + 1}.$selectedYear")
            }, year, month, day).show()
        }
    }

    private fun setupSaveButton() {
        binding.saveTaskButton.setOnClickListener {
            val taskName = binding.taskNameEditText.text.toString()
            if (taskName.isBlank()) {
                Toast.makeText(context, "Введите название задачи", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val taskType = when (binding.taskTypeRadioGroup.checkedRadioButtonId) {
                R.id.simpleTaskRadioButton -> "simple"
                R.id.oneTimeTaskRadioButton -> "onetime"
                else -> "simple"
            }

            val deadline = if (taskType == "onetime") {
                val deadlineText = binding.deadlineDateEditText.text.toString()
                if (deadlineText.isBlank()) {
                    Toast.makeText(context, "Выберите срок выполнения", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                deadlineText
            } else null

            val importance = when (binding.importanceRadioGroup.checkedRadioButtonId) {
                R.id.lowPriorityRadioButton -> "low"
                R.id.mediumPriorityRadioButton -> "medium"
                R.id.highPriorityRadioButton -> "high"
                else -> "medium"
            }

            val task = Task(
                name = taskName,
                type = taskType,
                deadline = deadline,
                importance = importance
            )

            lifecycleScope.launch {
                taskDao.insertTask(task)
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}