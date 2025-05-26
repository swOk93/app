package com.example.myapplication

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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
    
    // Кэш для хранения записей, чтобы избежать повторных вычислений
    private var cachedRecords: List<HabitProgressHistory.ProgressRecord>? = null
    private var lastCacheTime: Long = 0
    private val CACHE_VALID_TIME = 60000L // 1 минута
    
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
    
    /**
     * Получает записи из кэша или загружает новые, если кэш устарел
     */
    private fun getCachedWeekRecords(): List<HabitProgressHistory.ProgressRecord> {
        val currentTime = System.currentTimeMillis()
        
        // Если кэш действителен и не пустой, возвращаем его
        if (cachedRecords != null && currentTime - lastCacheTime < CACHE_VALID_TIME) {
            return cachedRecords!!
        }
        
        // Иначе загружаем новые данные и обновляем кэш
        val newRecords = getLastWeekRecords()
        cachedRecords = newRecords
        lastCacheTime = currentTime
        
        return newRecords
    }
    
    private fun setupChart() {
        // Используем кэшированные записи за последнюю неделю
        val weekRecords = getCachedWeekRecords()
        
        if (weekRecords.isEmpty()) {
            lineChart.setNoDataText("Нет данных о прогрессе за последнюю неделю")
            lineChart.setNoDataTextColor(resources.getColor(R.color.mint_text_primary, null))
            lineChart.invalidate()
            return
        }
        
        // Предварительно вычисляем некоторые значения для оптимизации
        lineChart.setHardwareAccelerationEnabled(true)
        
        // Оптимизация: предварительно вычисляем первый timestamp
        val firstTimestamp = weekRecords.first().timestamp
        
        // Создаем список точек для графика - оптимизированная версия
        val entries = ArrayList<Entry>(weekRecords.size) // Предварительное выделение памяти
        for (i in weekRecords.indices) {
            val record = weekRecords[i]
            entries.add(Entry(i.toFloat(), record.count.toFloat()))
        }
        
        // Создаем и настраиваем набор данных с оптимизированными настройками
        val dataSet = LineDataSet(entries, habit.name).apply {
            lineWidth = 3f
            circleRadius = 5f
            setDrawFilled(true)
            fillAlpha = 80
            mode = LineDataSet.Mode.CUBIC_BEZIER // Сглаженные линии
            
            // Настраиваем цвета в зависимости от типа привычки
            val mainColor = when (habit.type) {
                HabitType.TIME -> resources.getColor(R.color.mint_primary, null) // Коралловый
                HabitType.REPEAT -> resources.getColor(R.color.mint_dark, null) // Мятный
                HabitType.SIMPLE -> resources.getColor(R.color.mint_progress, null) // Персиковый
            }
            
            // Настройка градиентной заливки - оптимизированная версия
            val drawable = context?.let { ContextCompat.getDrawable(it, R.drawable.fade_gradient) }
            if (drawable != null) {
                drawable.setTint(mainColor)
                fillDrawable = drawable
            }
            
            setColor(mainColor)
            setCircleColor(mainColor)
            valueTextColor = mainColor
            valueTextSize = 12f
            
            // Настройка выделения точек
            highLightColor = resources.getColor(R.color.mint_accent, null)
            setDrawHighlightIndicators(true)
            enableDashedHighlightLine(10f, 5f, 0f)
        }
        
        // Создаем и устанавливаем данные для графика
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        
        // Настраиваем форматирование оси X (даты) - оптимизированная версия
        val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
        val calendar = Calendar.getInstance() // Переиспользуем один экземпляр Calendar
        val xLabels = ArrayList<String>(weekRecords.size)
        
        for (record in weekRecords) {
            calendar.timeInMillis = record.timestamp
            xLabels.add(dateFormat.format(calendar.time))
        }
        
        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(xLabels)
            granularity = 1f
            textColor = resources.getColor(R.color.mint_text_primary, null)
            textSize = 12f
            setDrawGridLines(false)
        }
        
        // Настраиваем описание оси Y в зависимости от типа привычки
        val yAxisDescription = when (habit.type) {
            HabitType.TIME -> "Минуты"
            HabitType.REPEAT -> "Повторения"
            HabitType.SIMPLE -> "Выполнено"
        }
        
        lineChart.axisLeft.apply {
            axisMinimum = 0f
            textColor = resources.getColor(R.color.mint_text_primary, null)
            textSize = 12f
            setDrawGridLines(true)
            gridColor = resources.getColor(R.color.mint_text_secondary, null)
            gridLineWidth = 0.5f
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
            targetLine.lineColor = resources.getColor(R.color.mint_delete, null) // Лавандовый
            targetLine.lineWidth = 2f
            targetLine.textColor = resources.getColor(R.color.mint_text_primary, null)
            targetLine.textSize = 12f
            lineChart.axisLeft.addLimitLine(targetLine)
        }
        
        // Дополнительные настройки графика - оптимизированная версия
        with(lineChart) {
            val desc = Description()
            desc.isEnabled = false
            description = desc
            legend.isEnabled = true
            legend.textColor = resources.getColor(R.color.mint_text_primary, null)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            // Уменьшаем время анимации для ускорения отрисовки
            animateX(500)
            
            // Настройка фона графика
            setBackgroundColor(resources.getColor(R.color.mint_card, null))
            setDrawGridBackground(false)
            setDrawBorders(false)
            
            // Отключаем лишние вычисления при отрисовке
            isHighlightPerDragEnabled = false
            
            // Настройка маркера при нажатии на точку
            val markerView = context?.let { MarkerView(it, R.layout.custom_marker_view) }
            markerView?.chartView = this
            marker = markerView
        }
        
        // Обновляем график
        lineChart.invalidate()
    }
    
    /**
     * Получает записи за последнюю неделю
     * Оптимизированная версия для более быстрого получения данных
     */
    private fun getLastWeekRecords(): List<HabitProgressHistory.ProgressRecord> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Неделя назад
        val weekAgo = calendar.timeInMillis
        
        // Получаем только записи для данной привычки за последнюю неделю
        // Используем последовательность для ленивой обработки данных
        return progressHistory.getRecordsForHabit(habitPosition)
            .asSequence()
            .filter { it.timestamp >= weekAgo }
            .sortedBy { it.timestamp }
            .toList()
    }
    
    // Удаляем метод generateTestData(), так как он больше не используется
}