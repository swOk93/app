package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentAddTaskBinding
import java.util.Calendar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    private var editingTask: Task? = null

    companion object {
        private const val ARG_TASK = "arg_task"
        fun newInstance(task: Task): AddTaskFragment {
            val fragment = AddTaskFragment()
            val args = Bundle()
            args.putSerializable(ARG_TASK, task)
            fragment.arguments = args
            return fragment
        }
    }

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

        editingTask = arguments?.getSerializable(ARG_TASK) as? Task
        setupTaskTypeRadioGroup()
        setupDatePicker()
        setupSaveButton()
        if (editingTask != null) fillFields(editingTask!!)
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
            val taskType = when (binding.taskTypeRadioGroup.checkedRadioButtonId) {
                R.id.simpleTaskRadioButton -> "simple"
                R.id.oneTimeTaskRadioButton -> "onetime"
                else -> "simple"
            }
            val deadline = if (taskType == "onetime") binding.deadlineDateEditText.text.toString() else null
            val importance = when (binding.importanceRadioGroup.checkedRadioButtonId) {
                R.id.lowPriorityRadioButton -> "low"
                R.id.mediumPriorityRadioButton -> "medium"
                R.id.highPriorityRadioButton -> "high"
                else -> "medium"
            }

            if (taskName.isBlank()) return@setOnClickListener

            val database = AppDatabase.getInstance(requireContext())
            val taskDao = database.taskDao()
            lifecycleScope.launch {
                if (editingTask == null) {
                    val task = Task(
                        name = taskName,
                        type = taskType,
                        deadline = deadline,
                        importance = importance
                    )
                    taskDao.insertTask(task)
                } else {
                    val updated = editingTask!!.copy(
                        name = taskName,
                        type = taskType,
                        deadline = deadline,
                        importance = importance
                    )
                    taskDao.updateTask(updated)
                }
                requireActivity().runOnUiThread {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fillFields(task: Task) {
        binding.taskNameEditText.setText(task.name)
        when (task.type) {
            "simple" -> binding.simpleTaskRadioButton.isChecked = true
            "onetime" -> binding.oneTimeTaskRadioButton.isChecked = true
        }
        if (task.type == "onetime") {
            binding.deadlineLayout.visibility = View.VISIBLE
            binding.deadlineDateEditText.setText(task.deadline ?: "")
        } else {
            binding.deadlineLayout.visibility = View.GONE
        }
        when (task.importance) {
            "low" -> binding.lowPriorityRadioButton.isChecked = true
            "medium" -> binding.mediumPriorityRadioButton.isChecked = true
            "high" -> binding.highPriorityRadioButton.isChecked = true
        }
    }
}