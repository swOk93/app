package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.myapplication.databinding.FragmentAddSectionBinding

class AddSectionFragment : DialogFragment() {
    private var _binding: FragmentAddSectionBinding? = null
    private val binding get() = _binding!!
    
    // Интерфейс для обработки добавления нового раздела
    interface OnSectionAddedListener {
        fun onSectionAdded(sectionName: String)
    }
    
    // Слушатель добавления раздела
    var sectionAddedListener: OnSectionAddedListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Устанавливаем обработчики для кнопок
        binding.cancelButton.setOnClickListener {
            // Закрываем диалог
            dismiss()
        }
        
        binding.saveButton.setOnClickListener {
            val sectionName = binding.editTextSectionName.text.toString().trim()
            
            if (sectionName.isEmpty()) {
                // Показываем сообщение об ошибке, если название не введено
                binding.sectionNameLayout.error = getString(R.string.enter_section_name)
            } else {
                // Вызываем обработчик добавления раздела
                sectionAddedListener?.onSectionAdded(sectionName)
                // Показываем сообщение об успехе
                Toast.makeText(context, getString(R.string.section_added), Toast.LENGTH_SHORT).show()
                // Закрываем диалог
                dismiss()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(): AddSectionFragment {
            return AddSectionFragment()
        }
    }
} 