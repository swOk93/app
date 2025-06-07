package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.*

class HabitChartFragment : Fragment() {
    private lateinit var lineChart: LineChart
    private lateinit var chartTitleTextView: TextView
    private lateinit var backButton: ImageButton
    private lateinit var timeRangeToggleGroup: MaterialButtonToggleGroup
    private lateinit var displayModeToggleGroup: MaterialButtonToggleGroup
    private lateinit var weekButton: MaterialButton
    private lateinit var monthButton: MaterialButton
    private lateinit var allTimeButton: MaterialButton
    private lateinit var normalModeButton: MaterialButton
    private lateinit var cumulativeModeButton: MaterialButton
    
    private var habitPosition: Int = -1
    private lateinit var progressHistory: HabitProgressHistory
    private lateinit var habit: Habit
    
    // Текущий выбранный временной диапазон
    private var currentTimeRange: TimeRange = TimeRange.WEEK
    
    // Текущий режим отображения
    private var isCumulativeMode: Boolean = false
    
    // Кэш для хранения записей, чтобы избежать повторных вычислений
    private var cachedRecords: MutableMap<TimeRange, List<HabitProgressHistory.ProgressRecord>> = mutableMapOf()
    private var lastCacheTime: Long = 0
    private val CACHE_VALID_TIME = 60000L // 1 минута
    
    // Максимальное количество точек для графика "За всё время"
    private val MAX_ALL_TIME_POINTS = 100
    
    // Перечисление для временных диапазонов
    enum class TimeRange {
        WEEK,   // 7 дней
        MONTH,  // 30 дней
        ALL_TIME // Всё время
    }
    
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
        habit = mainActivity.habitAdapter.getHabitAt(habitPosition)
        
        lineChart = view.findViewById(R.id.lineChart)
        chartTitleTextView = view.findViewById(R.id.chartTitleTextView)
        backButton = view.findViewById(R.id.backButton)
        timeRangeToggleGroup = view.findViewById(R.id.timeRangeToggleGroup)
        displayModeToggleGroup = view.findViewById(R.id.displayModeToggleGroup)
        weekButton = view.findViewById(R.id.weekButton)
        monthButton = view.findViewById(R.id.monthButton)
        allTimeButton = view.findViewById(R.id.allTimeButton)
        normalModeButton = view.findViewById(R.id.normalModeButton)
        cumulativeModeButton = view.findViewById(R.id.cumulativeModeButton)
        
        // Настраиваем заголовок
        chartTitleTextView.text = getString(R.string.progress_chart_title, habit.name)
        
        // Настройка кнопки назад
        backButton.setOnClickListener {
            // Возвращаемся к списку привычек
            requireActivity().supportFragmentManager.popBackStack()
            // Показываем список привычек через публичный метод MainActivity
            (requireActivity() as MainActivity).showHabitList()
        }
        
        // Настройка переключателей временного диапазона
        weekButton.isChecked = true // По умолчанию выбран недельный диапазон
        normalModeButton.isChecked = true // По умолчанию выбран обычный режим
        
        timeRangeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentTimeRange = when (checkedId) {
                    R.id.weekButton -> TimeRange.WEEK
                    R.id.monthButton -> TimeRange.MONTH
                    R.id.allTimeButton -> TimeRange.ALL_TIME
                    else -> TimeRange.WEEK
                }
                setupChart()
            }
        }
        
        displayModeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isCumulativeMode = (checkedId == R.id.cumulativeModeButton)
                setupChart()
            }
        }
        
        // Инициализируем и настраиваем график
        setupChart()
    }
    
    private fun getRecordsForTimeRange(timeRange: TimeRange): List<HabitProgressHistory.ProgressRecord> {
        val currentTime = System.currentTimeMillis()
        
        // Если кэш действителен, возвращаем его
        if (cachedRecords.containsKey(timeRange) && (currentTime - lastCacheTime) < CACHE_VALID_TIME) {
            return cachedRecords[timeRange]!!
        }
        
        // Иначе загружаем новые данные и обновляем кэш
        val records = when (timeRange) {
            TimeRange.WEEK -> getLastWeekRecords()
            TimeRange.MONTH -> getLastMonthRecords()
            TimeRange.ALL_TIME -> getAllTimeRecords()
        }
        
        // Обновляем кэш
        cachedRecords[timeRange] = records
        lastCacheTime = currentTime
        
        return records
    }
    
    /**
     * Получает записи из кэша или загружает новые, если кэш устарел
     */
    private fun getCachedWeekRecords(): List<HabitProgressHistory.ProgressRecord> {
        return getRecordsForTimeRange(TimeRange.WEEK)
    }
    
    private fun setupChart() {
        // Включаем аппаратное ускорение для графика
        lineChart.setHardwareAccelerationEnabled(true)
        
        // Получаем записи прогресса для текущей привычки в зависимости от выбранного временного диапазона
        val records = getRecordsForTimeRange(currentTimeRange)
        
        if (records.isEmpty()) {
            lineChart.setNoDataText(getString(R.string.no_progress_data))
            lineChart.setNoDataTextColor(resources.getColor(R.color.mint_accent, null))
            lineChart.invalidate()
            return
        }
        
        // Создаем список точек для графика и метки для оси X
        val entries = ArrayList<Entry>()
        val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
        val calendar = Calendar.getInstance() // Переиспользуем один экземпляр Calendar
        val xLabels = ArrayList<String>()
        
        when (currentTimeRange) {
            TimeRange.WEEK -> {
                // Для недели - показываем данные за последние 7 дней
                val weekStartTime = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6) // 7 дней назад (включая сегодня)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                // Создаем точки для каждого дня недели
                for (i in 0..6) {
                    val dayTimestamp = weekStartTime + i * 24 * 60 * 60 * 1000L
                    val nextDayTimestamp = dayTimestamp + 24 * 60 * 60 * 1000L
                    
                    // Находим записи за этот день
                    val dayRecords = records.asSequence()
                        .filter { it.timestamp >= dayTimestamp && it.timestamp < nextDayTimestamp }
                    
                    val value = if (habit.type == HabitType.SIMPLE) {
                        // Для простой привычки берем последнее значение за день (0 или 1)
                        dayRecords.maxByOrNull { it.timestamp }?.count?.toFloat() ?: 0f
                    } else {
                        // Для других типов суммируем все значения за день
                        val dailySum = dayRecords.sumOf { it.count.toDouble() }.toFloat()
                        if (isCumulativeMode && i > 0) {
                            // В накопительном режиме добавляем значение предыдущего дня
                            dailySum + entries[i - 1].y
                        } else {
                            dailySum
                        }
                    }
                    
                    entries.add(Entry(i.toFloat(), value))
                    
                    // Добавляем метку для оси X
                    calendar.timeInMillis = dayTimestamp
                    xLabels.add(dateFormat.format(calendar.time))
                }
            }
            TimeRange.MONTH -> {
                // Для месяца - показываем данные за последние 30 дней
                val monthStartTime = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -29) // 30 дней назад (включая сегодня)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                // Создаем точки для каждого дня месяца
                for (i in 0..29) {
                    val dayTimestamp = monthStartTime + i * 24 * 60 * 60 * 1000L
                    val nextDayTimestamp = dayTimestamp + 24 * 60 * 60 * 1000L
                    
                    // Находим записи за этот день
                    val dayRecords = records.asSequence()
                        .filter { it.timestamp >= dayTimestamp && it.timestamp < nextDayTimestamp }
                    
                    val value = if (habit.type == HabitType.SIMPLE) {
                        // Для простой привычки берем последнее значение за день (0 или 1)
                        dayRecords.maxByOrNull { it.timestamp }?.count?.toFloat() ?: 0f
                    } else {
                        // Для других типов суммируем все значения за день
                        val dailySum = dayRecords.sumOf { it.count.toDouble() }.toFloat()
                        if (isCumulativeMode && i > 0) {
                            // В накопительном режиме добавляем значение предыдущего дня
                            dailySum + entries[i - 1].y
                        } else {
                            dailySum
                        }
                    }
                    
                    entries.add(Entry(i.toFloat(), value))
                    
                    // Добавляем метку для оси X (показываем каждый 5-й день для читаемости)
                    calendar.timeInMillis = dayTimestamp
                    xLabels.add(if (i % 5 == 0 || i == 29) dateFormat.format(calendar.time) else "")
                }
            }
            TimeRange.ALL_TIME -> {
                // Для всего времени - обрабатываем все записи
                val sortedRecords = records.sortedBy { it.timestamp }
                
                if (sortedRecords.isEmpty()) {
                    lineChart.setNoDataText(getString(R.string.no_data_to_display))
                    lineChart.invalidate()
                    return
                }
                
                // Находим первую ненулевую отметку
                val firstNonZeroIndex = sortedRecords.indexOfFirst { it.count > 0 }
                val firstTimestamp = if (firstNonZeroIndex != -1) {
                    sortedRecords[firstNonZeroIndex].timestamp
                } else {
                    sortedRecords.first().timestamp
                }
                // Используем текущую дату как последнюю дату для графика
                val lastTimestamp = Calendar.getInstance().timeInMillis
                val totalDays = ((lastTimestamp - firstTimestamp) / (24 * 60 * 60 * 1000L)).toInt() + 1
                
                if (totalDays <= MAX_ALL_TIME_POINTS) {
                    // Если дней меньше MAX_ALL_TIME_POINTS, показываем каждый день
                    val dayMap = mutableMapOf<Int, MutableList<HabitProgressHistory.ProgressRecord>>()
                    
                    // Группируем записи по дням
                    for (record in sortedRecords) {
                        val dayIndex = ((record.timestamp - firstTimestamp) / (24 * 60 * 60 * 1000L)).toInt()
                        if (!dayMap.containsKey(dayIndex)) {
                            dayMap[dayIndex] = mutableListOf()
                        }
                        dayMap[dayIndex]!!.add(record)
                    }
                    
                    // Создаем записи для каждого дня
                    for (i in 0 until totalDays) {
                        val dayRecords = dayMap[i] ?: emptyList()
                        
                        val value = if (habit.type == HabitType.SIMPLE) {
                            // Для простой привычки берем последнее значение за день (0 или 1)
                            dayRecords.maxByOrNull { it.timestamp }?.count?.toFloat() ?: 0f
                        } else {
                            // Для других типов суммируем все значения за день
                            val dailySum = dayRecords.sumOf { it.count.toDouble() }.toFloat()
                            if (isCumulativeMode && i > 0) {
                                // В накопительном режиме добавляем значение предыдущего дня
                                dailySum + entries[i - 1].y
                            } else {
                                dailySum
                            }
                        }
                        
                        entries.add(Entry(i.toFloat(), value))
                        
                        // Добавляем метку для оси X (показываем только некоторые даты для читаемости)
                        calendar.timeInMillis = firstTimestamp + i * 24 * 60 * 60 * 1000L
                        xLabels.add(if (totalDays <= 10 || i % (totalDays / 10 + 1) == 0 || i == totalDays - 1) {
                            dateFormat.format(calendar.time)
                        } else {
                            ""
                        })
                    }
                } else {
                    // Если дней больше MAX_ALL_TIME_POINTS, агрегируем данные по 2 дня
                    val daysPerPoint = 2
                    val numPoints = (totalDays + daysPerPoint - 1) / daysPerPoint // округление вверх
                    val aggregatedData = Array(numPoints) { 0f }
                    val pointCounts = Array(numPoints) { 0 }
                    
                    // Распределяем записи по точкам графика
                    for (record in sortedRecords) {
                        val dayIndex = ((record.timestamp - firstTimestamp) / (24 * 60 * 60 * 1000L)).toInt()
                        val pointIndex = dayIndex / daysPerPoint
                        
                        if (pointIndex < numPoints) {
                            if (habit.type == HabitType.SIMPLE) {
                                // Для простой привычки берем максимальное значение в группе дней
                                aggregatedData[pointIndex] = kotlin.math.max(aggregatedData[pointIndex], record.count.toFloat())
                                pointCounts[pointIndex] = 1 // Для простой привычки не нужно считать среднее
                            } else {
                                // Для других типов суммируем значения
                                aggregatedData[pointIndex] += record.count.toFloat()
                                pointCounts[pointIndex]++
                            }
                        }
                    }
                    
                    // Создаем записи для графика
                    for (i in 0 until numPoints) {
                        val value = if (habit.type == HabitType.SIMPLE) {
                            // Для простой привычки уже взяли максимальное значение
                            aggregatedData[i]
                        } else if (pointCounts[i] > 0) {
                            // Для других типов берем среднее значение за период
                            val periodValue = aggregatedData[i] / pointCounts[i]
                            if (isCumulativeMode && i > 0) {
                                // В накопительном режиме добавляем значение предыдущей точки
                                periodValue + entries[i - 1].y
                            } else {
                                periodValue
                            }
                        } else {
                            0f
                        }
                        
                        entries.add(Entry(i.toFloat(), value))
                        
                        // Добавляем метку для оси X
                        calendar.timeInMillis = firstTimestamp + (i * daysPerPoint) * 24 * 60 * 60 * 1000L
                        xLabels.add(if (i % (numPoints / 10 + 1) == 0 || i == numPoints - 1) {
                            dateFormat.format(calendar.time)
                        } else {
                            ""
                        })
                    }
                }
            }
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
                HabitType.TIME -> resources.getColor(R.color.mint_coral, null) // Коралловый
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
            valueTextColor = resources.getColor(R.color.mint_accent, null)
            valueTextSize = 12f
            
            // Настройка выделения точек
            highLightColor = resources.getColor(R.color.mint_accent, null)
            setDrawHighlightIndicators(true)
            enableDashedHighlightLine(10f, 5f, 0f)
        }
        
        // Создаем и устанавливаем данные для графика
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        
        // Настраиваем форматирование оси X (даты)
        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < xLabels.size) xLabels[index] else ""
                }
            }
            granularity = 1f
            textColor = resources.getColor(R.color.mint_accent, null)
            textSize = 12f
            setDrawGridLines(false)
        }
        
        // Настраиваем описание оси Y в зависимости от типа привычки
        val yAxisDescription = when (habit.type) {
            HabitType.TIME -> getString(R.string.minutes_label)
            HabitType.REPEAT -> getString(R.string.quantity)
            HabitType.SIMPLE -> getString(R.string.completed_label)
        }
        
        lineChart.axisLeft.apply {
            axisMinimum = 0f
            textColor = resources.getColor(R.color.mint_accent, null)
            textSize = 12f
            setDrawGridLines(true)
            gridColor = resources.getColor(R.color.mint_text_secondary, null)
            gridLineWidth = 0.5f
        }
        
        // Настраиваем метки оси Y
        lineChart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (habit.type == HabitType.SIMPLE) {
                    if (value < 0.5f) getString(R.string.no) else getString(R.string.yes_short)
                } else {
                    "${value.toInt()} $yAxisDescription"
                }
            }
        }
        
        // Отключаем правую ось Y
        lineChart.axisRight.isEnabled = false
        
        // Добавляем горизонтальную линию для целевого значения
        if (habit.type != HabitType.SIMPLE) {
            val targetLine = LimitLine(habit.target.toFloat(), getString(R.string.target))
            targetLine.lineColor = resources.getColor(R.color.mint_delete, null) // Лавандовый
            targetLine.lineWidth = 2f
            targetLine.textColor = resources.getColor(R.color.mint_accent, null)
            targetLine.textSize = 12f
            lineChart.axisLeft.addLimitLine(targetLine)
        }
        
        // Дополнительные настройки графика - оптимизированная версия
        with(lineChart) {
            val desc = Description()
            desc.isEnabled = false
            description = desc
            legend.isEnabled = true
            legend.textColor = resources.getColor(R.color.mint_accent, null)
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
    
    /**
     * Получает записи прогресса за последний месяц (30 дней)
     */
    private fun getLastMonthRecords(): List<HabitProgressHistory.ProgressRecord> {
        // Получаем все записи для текущей привычки
        val allRecords = progressHistory.getRecordsForHabit(habitPosition)
        
        // Если записей нет, возвращаем пустой список
        if (allRecords.isEmpty()) return emptyList()
        
        // Вычисляем timestamp для начала месяца (30 дней назад)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val monthStartTime = calendar.timeInMillis
        
        // Фильтруем записи за последний месяц и сортируем по времени
        return allRecords.asSequence()
            .filter { it.timestamp >= monthStartTime }
            .sortedBy { it.timestamp }
            .toList()
    }
    
    /**
     * Получает все записи прогресса для привычки
     */
    private fun getAllTimeRecords(): List<HabitProgressHistory.ProgressRecord> {
        // Получаем все записи для текущей привычки
        val allRecords = progressHistory.getRecordsForHabit(habitPosition)
        
        // Сортируем записи по времени
        return allRecords.sortedBy { it.timestamp }
    }
    
    // Удаляем метод generateTestData(), так как он больше не используется
}