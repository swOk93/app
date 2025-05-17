package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import co.yml.charts.axis.AxisData
import co.yml.charts.common.extensions.formatToSinglePrecision
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.model.*
import co.yml.charts.ui.linechart.LineChart
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class HabitChartDialogFragment : DialogFragment() {
    private var habitPosition: Int = -1
    private lateinit var progressHistory: HabitProgressHistory
    private lateinit var habit: Habit
    private lateinit var composeView: androidx.compose.ui.platform.ComposeView
    private lateinit var chartTitleTextView: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var weekRadioButton: RadioButton
    private lateinit var monthRadioButton: RadioButton
    private lateinit var yearRadioButton: RadioButton
    
    private var currentTimeRange = TimeRange.WEEK
    
    enum class TimeRange {
        WEEK, MONTH, YEAR
    }
    
    companion object {
        private const val ARG_HABIT_POSITION = "habit_position"
        
        fun newInstance(habitPosition: Int): HabitChartDialogFragment {
            val fragment = HabitChartDialogFragment()
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
        // Устанавливаем стиль для диалога
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit_chart_dialog, container, false)
    }
    
    override fun onStart() {
        super.onStart()
        // Устанавливаем размер диалога на всю ширину экрана
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Добавляем анимацию для диалога
        dialog?.window?.attributes?.windowAnimations = android.R.style.Animation_Dialog
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Получаем данные о привычке и истории прогресса
        val mainActivity = activity as MainActivity
        progressHistory = mainActivity.progressHistory
        habit = mainActivity.habitAdapter.habits[habitPosition]
        
        // Инициализируем элементы интерфейса
        chartTitleTextView = view.findViewById(R.id.chartTitleTextView)
        chartTitleTextView.text = "График прогресса: ${habit.name}"
        
        // Инициализируем радиокнопки для выбора масштаба
        radioGroup = view.findViewById(R.id.timeRangeRadioGroup)
        weekRadioButton = view.findViewById(R.id.weekRadioButton)
        monthRadioButton = view.findViewById(R.id.monthRadioButton)
        yearRadioButton = view.findViewById(R.id.yearRadioButton)
        
        // Устанавливаем обработчик изменения выбора масштаба
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.weekRadioButton -> {
                    currentTimeRange = TimeRange.WEEK
                    setupChart()
                }
                R.id.monthRadioButton -> {
                    currentTimeRange = TimeRange.MONTH
                    setupChart()
                }
                R.id.yearRadioButton -> {
                    currentTimeRange = TimeRange.YEAR
                    setupChart()
                }
            }
        }
        
        // Инициализируем график через ComposeView
        composeView = view.findViewById(R.id.composeChart)
        setupChart()
    }
    
    private fun setupChart() {
        // Получаем записи за выбранный период
        val records = getRecordsForTimeRange(currentTimeRange)
        
        composeView.setContent {
            androidx.compose.material.Surface(color = Color.White) {
                if (records.isEmpty()) {
                    // Если нет данных, показываем сообщение
                    androidx.compose.material.Text(
                        text = "Нет данных за выбранный период",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxSize()
                            .wrapContentHeight()
                    )
                } else {
                    // Создаем точки для графика
                    val points = records.mapIndexed { index, record ->
                        Point(index.toFloat(), record.count.toFloat())
                    }
                    
                    // Получаем даты для оси X
                    val xAxisLabels = getFormattedDates(records, currentTimeRange)
                    
                    // Проверяем, что habit инициализирован
                    if (!::habit.isInitialized) {
                        return@Surface
                    }
                    
                    // Определяем максимальное значение для оси Y
                    val maxYValue = when (habit.type) {
                        HabitType.TIME, HabitType.REPEAT -> {
                            val maxRecord = records.maxByOrNull { it.count } ?: return@Surface
                            maxOf(maxRecord.count, habit.target) * 1.2f // Добавляем 20% для отступа сверху
                        }
                        HabitType.SIMPLE -> 1.2f // Для простых привычек максимум 1 (выполнено/не выполнено)
                    }
                    
                    // Настраиваем данные для оси X
                    val xAxisData = AxisData.Builder()
                        .axisStepSize(50.dp)
                        .backgroundColor(Color.Transparent)
                        .steps(points.size - 1)
                        .labelData { i -> xAxisLabels.getOrElse(i) { "" } }
                        .labelAndAxisLinePadding(15.dp)
                        .axisLineColor(Color.Gray)
                        .axisLabelColor(Color.DarkGray)
                        .build()
                    
                    // Определяем описание для оси Y в зависимости от типа привычки
                    val yAxisDescription = when (habit.type) {
                        HabitType.TIME -> "мин"
                        HabitType.REPEAT -> "раз"
                        HabitType.SIMPLE -> ""
                    }
                    
                    // Настраиваем данные для оси Y
                    val yAxisData = AxisData.Builder()
                        .steps(5)
                        .backgroundColor(Color.Transparent)
                        .labelAndAxisLinePadding(20.dp)
                        .axisLineColor(Color.Gray)
                        .axisLabelColor(Color.DarkGray)
                        .labelData { i ->
                            val value = (i * (maxYValue / 5)).formatToSinglePrecision()
                            "$value $yAxisDescription"
                        }
                        .build()
                    
                    // Определяем цвет линии в зависимости от типа привычки
                    val lineColor = when (habit.type) {
                        HabitType.TIME -> Color(0xFF2196F3) // Синий
                        HabitType.REPEAT -> Color(0xFF4CAF50) // Зеленый
                        HabitType.SIMPLE -> Color(0xFFF44336)// Красный
                    }
                    
                    // Настраиваем линию для целевого значения
                    val targetLine = if (habit.type != HabitType.SIMPLE) {
                        val targetPoints = points.map { Point(it.x, habit.target.toFloat()) }
                        Line(
                            dataPoints = targetPoints,
                            lineStyle = LineStyle(
                                color = Color(0xFFFF9800), // Оранжевый
                                width = 2F
                            ),
                            intersectionPoint = IntersectionPoint(
                                color = Color(0xFFFF9800),
                                radius = 3.dp
                            ),
                            shadowUnderLine = ShadowUnderLine()
                        )
                    } else null
                    
                    // Настраиваем данные для линии графика
                    val lineChartData = LineChartData(
                        linePlotData = LinePlotData(
                            lines = listOfNotNull(
                                Line(
                                    dataPoints = points,
                                    lineStyle = LineStyle(
                                        color = lineColor,
                                        width = 4F
                                    ),
                                    intersectionPoint = IntersectionPoint(
                                        color = lineColor,
                                        radius = 5.dp
                                    ),
                                    shadowUnderLine = ShadowUnderLine(
                                        alpha = 0.5f,
                                        color = lineColor
                                    )
                                ),
                                targetLine
                            ),
                        ),
                        xAxisData = xAxisData,
                        yAxisData = yAxisData,
                        backgroundColor = Color.White
                    )
                    
                    // Отображаем график
                    LineChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),      // или любое другое нужное вам размеры
                        lineChartData = lineChartData
                    )
                }
            }
        }
    }
    
    // Метод setupEmptyChart больше не нужен, так как мы обрабатываем пустые данные в setContent
    
    private fun getRecordsForTimeRange(timeRange: TimeRange): List<HabitProgressHistory.ProgressRecord> {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        
        // Определяем начало периода в зависимости от выбранного масштаба
        val startTime = when (timeRange) {
            TimeRange.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7) // Неделя назад
                calendar.timeInMillis
            }
            TimeRange.MONTH -> {
                calendar.add(Calendar.DAY_OF_YEAR, 7) // Возвращаемся на текущую дату
                calendar.add(Calendar.MONTH, -1) // Месяц назад
                calendar.timeInMillis
            }
            TimeRange.YEAR -> {
                calendar.add(Calendar.MONTH, 1) // Возвращаемся на текущую дату
                calendar.add(Calendar.YEAR, -1) // Год назад
                calendar.timeInMillis
            }
        }
        
        // Проверяем, что habitPosition и progressHistory инициализированы
        if (habitPosition < 0 || !::progressHistory.isInitialized) {
            return generateTestData(timeRange)
        }
        
        // Получаем записи для привычки
        val records = progressHistory.getRecordsForHabit(habitPosition)
        
        // Если нет реальных записей или они все старше выбранного периода, создаем тестовые данные
        if (records.isEmpty() || records.all { it.timestamp < startTime }) {
            return generateTestData(timeRange)
        }
        
        // Фильтруем записи по временному диапазону и сортируем по времени
        return records.filter { it.timestamp in startTime..currentTime }
            .sortedBy { it.timestamp }
    }
    
    private fun getFormattedDates(records: List<HabitProgressHistory.ProgressRecord>, timeRange: TimeRange): List<String> {
        // Форматируем даты в зависимости от выбранного масштаба
        val dateFormat = when (timeRange) {
            TimeRange.WEEK -> SimpleDateFormat("dd.MM", Locale.getDefault()) // День.Месяц
            TimeRange.MONTH -> SimpleDateFormat("dd.MM", Locale.getDefault()) // День.Месяц
            TimeRange.YEAR -> SimpleDateFormat("MM.yy", Locale.getDefault()) // Месяц.Год
        }
        
        return records.map { record ->
            dateFormat.format(Date(record.timestamp))
        }
    }
    
    private fun generateTestData(timeRange: TimeRange): List<HabitProgressHistory.ProgressRecord> {
        val result = mutableListOf<HabitProgressHistory.ProgressRecord>()
        val calendar = Calendar.getInstance()
        val random = Random(System.currentTimeMillis())
        
        // Определяем количество дней для генерации данных
        val daysCount = when (timeRange) {
            TimeRange.WEEK -> 7
            TimeRange.MONTH -> 30
            TimeRange.YEAR -> 365
        }
        
        // Генерируем данные за указанный период
        // Для года и месяца генерируем данные с шагом, чтобы не создавать слишком много точек
        val step = when (timeRange) {
            TimeRange.WEEK -> 1 // Каждый день
            TimeRange.MONTH -> 1 // Каждый день
            TimeRange.YEAR -> 7 // Каждую неделю
        }
        
        // Проверяем, что habit инициализирован
        if (!::habit.isInitialized) {
            // Создаем тестовую привычку, если habit не инициализирован
            for (i in daysCount downTo 0 step step) {
                calendar.add(Calendar.DAY_OF_YEAR, -step)
                val timestamp = calendar.timeInMillis
                val count = random.nextInt(0, 10)
                result.add(HabitProgressHistory.ProgressRecord(0, count, timestamp))
            }
            return result
        }
        
        for (i in daysCount downTo 0 step step) {
            calendar.add(Calendar.DAY_OF_YEAR, -step)
            val timestamp = calendar.timeInMillis
            
            // Генерируем случайное значение в зависимости от типа привычки
            val count = when (habit.type) {
                HabitType.TIME -> random.nextInt(0, habit.target * 2)
                HabitType.REPEAT -> random.nextInt(0, habit.target * 2)
                HabitType.SIMPLE -> if (random.nextBoolean()) 1 else 0
            }
            
            result.add(HabitProgressHistory.ProgressRecord(habitPosition, count, timestamp))
        }
        
        return result
    }
}