package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.YAxis
import java.text.SimpleDateFormat
import java.util.*

class HabitChartFragment : Fragment() {
    private var habitPosition: Int = -1
    private lateinit var progressHistory: HabitProgressHistory
    private lateinit var habit: Habit
    private lateinit var lineChart: LineChart
    
    companion object {
        private const val ARG_HABIT_POSITION = "habit_position"
        
        fun newInstance(habitPosition: Int): HabitChartFragment {
            val fragment = HabitChartFragment()
            val args = Bundle()
            args.putInt(ARG_HABIT_POSITION, habitPosition)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            habitPosition = it.getInt(ARG_HABIT_POSITION, -1)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit_chart, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Получаем данные о привычке и истории прогресса
        val mainActivity = activity as MainActivity
        progressHistory = mainActivity.progressHistory
        habit = mainActivity.habitAdapter.habits[habitPosition]
        
        // Настраиваем заголовок
        val chartTitleTextView = view.findViewById<TextView>(R.id.chartTitleTextView)
        chartTitleTextView.text = "График прогресса: ${habit.name}"
        
        // Инициализируем и настраиваем график
        lineChart = view.findViewById(R.id.lineChart)
        setupChart()
    }
    
    private fun setupChart() {
        // Получаем записи за последнюю неделю
        val weekRecords = getLastWeekRecords()
        
        if (weekRecords.isEmpty()) {
            lineChart.setNoDataText("Нет данных за последнюю неделю")
            return
        }
        
        // Создаем список точек для графика
        val entries = weekRecords.mapIndexed { index, record ->
            Entry(index.toFloat(), record.count.toFloat())
        }
        
        // Создаем и настраиваем набор данных
        val dataSet = LineDataSet(entries, habit.name).apply {
            lineWidth = 2f
            circleRadius = 4f
            setDrawFilled(true)
            fillAlpha = 60
            
            // Настраиваем цвета в зависимости от типа привычки
            val mainColor = when (habit.type) {
                HabitType.TIME -> Color.BLUE
                HabitType.REPEAT -> Color.GREEN
                HabitType.SIMPLE -> Color.RED
            }
            
            setColor(mainColor)
            setCircleColor(mainColor)
            setFillColor(mainColor)
        }
        
        // Создаем и устанавливаем данные для графика
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        
        // Настраиваем форматирование оси X (даты)
        val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
        val xLabels = weekRecords.map { record ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = record.timestamp
            dateFormat.format(calendar.time)
        }
        
        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(xLabels)
            granularity = 1f
        }
        
        // Настраиваем описание оси Y в зависимости от типа привычки
        val yAxisDescription = when (habit.type) {
            HabitType.TIME -> "Минуты"
            HabitType.REPEAT -> "Повторения"
            HabitType.SIMPLE -> "Выполнено"
        }
        
        lineChart.axisLeft.apply {
            axisMinimum = 0f
            textColor = Color.BLACK
            textSize = 12f
        }
        
        // Set the Y-axis label using proper method
        lineChart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()} $yAxisDescription"
            }
        }
        // Отключаем правую ось Y
        lineChart.axisRight.isEnabled = false
        
        // Добавляем горизонтальную линию для целевого значения
        if (habit.type != HabitType.SIMPLE) {
            val targetLine = LimitLine(habit.target.toFloat(), "Цель")
            targetLine.lineColor = Color.RED
            targetLine.lineWidth = 1f
            lineChart.axisLeft.addLimitLine(targetLine)
        }
        
        // Дополнительные настройки графика
        lineChart.apply {
            val desc = Description()
            desc.isEnabled = false
            description = desc
            legend.isEnabled = true
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            animateX(1000)
        }
        
        // Обновляем график
        lineChart.invalidate()
    }
    
    /**
     * Получает записи за последнюю неделю
     */
    private fun getLastWeekRecords(): List<HabitProgressHistory.ProgressRecord> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Неделя назад
        val weekAgo = calendar.timeInMillis
        
        val records = progressHistory.getRecordsForHabit(habitPosition)
        
        // Если нет реальных записей, создаем тестовые данные
        if (records.isEmpty() || records.all { it.timestamp < weekAgo }) {
            return generateTestData()
        }
        
        return records.filter { it.timestamp >= weekAgo }
            .sortedBy { it.timestamp }
    }
    
    /**
     * Генерирует тестовые данные за последнюю неделю
     */
    private fun generateTestData(): List<HabitProgressHistory.ProgressRecord> {
        val result = mutableListOf<HabitProgressHistory.ProgressRecord>()
        val calendar = Calendar.getInstance()
        val random = Random()
        
        // Генерируем данные за последние 7 дней
        for (i in 6 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val timestamp = calendar.timeInMillis
            
            // Генерируем случайное значение в зависимости от типа привычки
            val count = when (habit.type) {
                HabitType.TIME -> random.nextInt(habit.target * 2)
                HabitType.REPEAT -> random.nextInt(habit.target * 2)
                HabitType.SIMPLE -> if (random.nextBoolean()) 1 else 0
            }
            
            result.add(HabitProgressHistory.ProgressRecord(habitPosition, count, timestamp))
        }
        
        return result
    }
}