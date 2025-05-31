package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentTasksBinding
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskDao: TaskDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupDatabase()
        observeTasks()
        setupAddTaskButton()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter().apply {
            onTaskCheckedChanged = { task, isChecked ->
                lifecycleScope.launch {
                    taskDao.updateTaskCompletion(task.id, isChecked)
                }
            }
            onTaskDelete = { task ->
                lifecycleScope.launch {
                    taskDao.deleteTask(task)
                }
            }
            onTaskEdit = { task ->
                val editTaskFragment = AddTaskFragment.newInstance(task)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, editTaskFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.tasksRecyclerView.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupDatabase() {
        val database = AppDatabase.getInstance(requireContext())
        taskDao = database.taskDao()
    }

    private fun observeTasks() {
        taskDao.getAllTasks().observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
        }
    }

    private fun setupAddTaskButton() {
        binding.addTaskFab.setOnClickListener {
            val addTaskFragment = AddTaskFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, addTaskFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = TasksFragment()
    }
}