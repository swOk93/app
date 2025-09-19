package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Скрываем секцию выбора разделов на экране настроек
        (activity as? MainActivity)?.let { it.binding.sectionSpinnerLayout.visibility = View.GONE }

        // Кнопка Назад — закрывает настройки и возвращает главный экран
        view.findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            (activity as? MainActivity)?.let {
                it.showHabitList()
                it.binding.sectionSpinnerLayout.visibility = View.VISIBLE
            }
        }

        val languages = listOf(
            view.context.getString(R.string.language_ru),
            view.context.getString(R.string.language_en)
        )

        val auto = view.findViewById<AutoCompleteTextView>(R.id.languageAutoComplete)
        auto.setAdapter(ArrayAdapter(view.context, android.R.layout.simple_list_item_1, languages))

        // Проставляем текущее значение
        val prefs = view.context.getSharedPreferences("HabitsPrefs", Context.MODE_PRIVATE)
        val code = prefs.getString("app_language", "ru")
        auto.setText(if (code == "en") view.context.getString(R.string.language_en) else view.context.getString(R.string.language_ru), false)

        auto.setOnItemClickListener { _, _, position, _ ->
            val selectedCode = if (position == 1) "en" else "ru"
            (activity as? MainActivity)?.applyAppLanguage(selectedCode)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // На случай системной кнопки Назад — вернуть элементы главного экрана
        (activity as? MainActivity)?.let {
            it.showHabitList()
            it.binding.sectionSpinnerLayout.visibility = View.VISIBLE
        }
    }
}


