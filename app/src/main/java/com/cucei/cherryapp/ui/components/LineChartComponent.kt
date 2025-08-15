package com.cucei.cherryapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cucei.cherryapp.data.AggregatedData
import com.cucei.cherryapp.data.AggregationType
import android.graphics.Paint
import android.graphics.Typeface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LineChartComponent(
    data: List<AggregatedData>,
    parameter: String,
    aggregationType: AggregationType,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay datos para mostrar")
        }
        return
    }

    Column(modifier = modifier) {
        // Gráfico (más grande)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp) // Aumentado de 300dp a 400dp
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val padding = 60f // Aumentado para espacio de etiquetas
                val chartWidth = width - 2 * padding
                val chartHeight = height - 2 * padding

                // Obtener valores para el parámetro seleccionado
                val values = data.map { getParameterValue(it, parameter) }
                val minValue = values.minOrNull() ?: 0.0
                val maxValue = values.maxOrNull() ?: 1.0

                // Calcular rangos según el parámetro
                val (rangeMin, rangeMax) = getParameterRange(parameter)
                val actualMin = if (minValue < rangeMin) minValue else rangeMin
                val actualMax = if (maxValue > rangeMax) maxValue else rangeMax
                val valueRange = actualMax - actualMin

                // Dibujar líneas de cuadrícula
                drawGridLines(
                    width = width,
                    height = height,
                    padding = padding,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    minValue = actualMin,
                    maxValue = actualMax,
                    parameter = parameter
                )

                // Dibujar eje Y (valores) con etiquetas
                drawYAxis(
                    width = width,
                    height = height,
                    padding = padding,
                    chartHeight = chartHeight,
                    minValue = actualMin,
                    maxValue = actualMax,
                    parameter = parameter
                )

                // Dibujar eje X (tiempo) con etiquetas
                drawXAxis(
                    width = width,
                    height = height,
                    padding = padding,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    data = data,
                    aggregationType = aggregationType
                )

                // Dibujar línea del gráfico
                if (data.size > 1) {
                    drawLineChart(
                        data = data,
                        parameter = parameter,
                        width = width,
                        height = height,
                        padding = padding,
                        chartWidth = chartWidth,
                        chartHeight = chartHeight,
                        minValue = actualMin,
                        maxValue = actualMax,
                        valueRange = valueRange
                    )
                }

                // Dibujar puntos de datos
                drawDataPoints(
                    data = data,
                    parameter = parameter,
                    width = width,
                    height = height,
                    padding = padding,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    minValue = actualMin,
                    maxValue = actualMax,
                    valueRange = valueRange
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLines(
    width: Float,
    height: Float,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minValue: Double,
    maxValue: Double,
    parameter: String
) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val strokeWidth = 1f

    // Líneas horizontales (valores)
    val gridLines = getGridLines(parameter, minValue, maxValue)
    gridLines.forEach { value ->
        val y = padding + chartHeight - ((value - minValue) / (maxValue - minValue) * chartHeight).toFloat()
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + chartWidth, y),
            strokeWidth = strokeWidth
        )
    }

    // Líneas verticales (tiempo)
    val timeDivisions = 6
    for (i in 0..timeDivisions) {
        val x = padding + (i * chartWidth / timeDivisions)
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, padding + chartHeight),
            strokeWidth = strokeWidth
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawYAxis(
    width: Float,
    height: Float,
    padding: Float,
    chartHeight: Float,
    minValue: Double,
    maxValue: Double,
    parameter: String
) {
    val axisColor = Color.Gray
    val strokeWidth = 2f

    // Eje Y
    drawLine(
        color = axisColor,
        start = Offset(padding, padding),
        end = Offset(padding, padding + chartHeight),
        strokeWidth = strokeWidth
    )

    // Etiquetas del eje Y
    val gridLines = getGridLines(parameter, minValue, maxValue)
    gridLines.forEach { value ->
        val y = padding + chartHeight - ((value - minValue) / (maxValue - minValue) * chartHeight).toFloat()
        drawLine(
            color = axisColor,
            start = Offset(padding - 5f, y),
            end = Offset(padding, y),
            strokeWidth = 1f
        )
        
        // Dibujar etiqueta numérica
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 30f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.DEFAULT
            }
            canvas.nativeCanvas.drawText(
                getFormattedValue(value, parameter),
                padding - 10f,
                y + 10f,
                paint
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawXAxis(
    width: Float,
    height: Float,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    data: List<AggregatedData>,
    aggregationType: AggregationType
) {
    val axisColor = Color.Gray
    val strokeWidth = 2f

    // Eje X
    drawLine(
        color = axisColor,
        start = Offset(padding, padding + chartHeight),
        end = Offset(padding + chartWidth, padding + chartHeight),
        strokeWidth = strokeWidth
    )

    // Etiquetas del eje X
    val timeDivisions = minOf(data.size, 6)
    for (i in 0..timeDivisions) {
        val x = padding + (i * chartWidth / timeDivisions)
        drawLine(
            color = axisColor,
            start = Offset(x, padding + chartHeight),
            end = Offset(x, padding + chartHeight + 5f),
            strokeWidth = 1f
        )
        
        // Dibujar etiqueta de tiempo
        if (i < data.size) {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 25f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT
                }
                val label = getTimeLabel(data[i], aggregationType)
                canvas.nativeCanvas.drawText(
                    label,
                    x,
                    padding + chartHeight + 25f,
                    paint
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineChart(
    data: List<AggregatedData>,
    parameter: String,
    width: Float,
    height: Float,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minValue: Double,
    maxValue: Double,
    valueRange: Double
) {
    val lineColor = getParameterColor(parameter)
    val strokeWidth = 3f

    val path = Path()
    var isFirst = true

    data.forEachIndexed { index, item ->
        val value = getParameterValue(item, parameter)
        val x = padding + (index * chartWidth / (data.size - 1))
        val y = padding + chartHeight - ((value - minValue) / valueRange * chartHeight).toFloat()

        if (isFirst) {
            path.moveTo(x, y)
            isFirst = false
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDataPoints(
    data: List<AggregatedData>,
    parameter: String,
    width: Float,
    height: Float,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minValue: Double,
    maxValue: Double,
    valueRange: Double
) {
    val pointColor = getParameterColor(parameter)
    val pointRadius = 6f

    data.forEachIndexed { index, item ->
        val value = getParameterValue(item, parameter)
        val x = padding + (index * chartWidth / (data.size - 1))
        val y = padding + chartHeight - ((value - minValue) / valueRange * chartHeight).toFloat()

        drawCircle(
            color = pointColor,
            radius = pointRadius,
            center = Offset(x, y)
        )
    }
}

// Funciones utilitarias
private fun getParameterValue(dataPoint: AggregatedData, parameter: String): Double {
    return when (parameter) {
        "temperature" -> dataPoint.temperature
        "humidity" -> dataPoint.humidity
        "lux" -> dataPoint.lux
        "moisture" -> dataPoint.moisture
        else -> dataPoint.temperature
    }
}

private fun getParameterLabel(parameter: String): String {
    return when (parameter) {
        "temperature" -> "Temperatura (°C)"
        "humidity" -> "Humedad (%)"
        "lux" -> "Luminosidad (lux)"
        "moisture" -> "Humedad del Suelo (%)"
        else -> "Temperatura (°C)"
    }
}

private fun getParameterRange(parameter: String): Pair<Double, Double> {
    return when (parameter) {
        "temperature" -> Pair(0.0, 50.0) // 0-50°C
        "humidity" -> Pair(0.0, 100.0) // 0-100%
        "lux" -> Pair(0.0, 10000.0) // 0-10000 lux
        "moisture" -> Pair(0.0, 100.0) // 0-100%
        else -> Pair(0.0, 50.0)
    }
}

private fun getGridLines(parameter: String, minValue: Double, maxValue: Double): List<Double> {
    val range = maxValue - minValue
    val step = when (parameter) {
        "temperature" -> 10.0 // Cada 10°C
        "humidity" -> 20.0 // Cada 20%
        "lux" -> 2000.0 // Cada 2000 lux
        "moisture" -> 20.0 // Cada 20%
        else -> range / 5
    }

    val lines = mutableListOf<Double>()
    var current = minValue
    while (current <= maxValue) {
        lines.add(current)
        current += step
    }
    return lines
}

private fun getParameterColor(parameter: String): Color {
    return when (parameter) {
        "temperature" -> Color(0xFFE57373) // Rojo
        "humidity" -> Color(0xFF81C784) // Verde
        "lux" -> Color(0xFFFFB74D) // Naranja
        "moisture" -> Color(0xFF64B5F6) // Azul
        else -> Color(0xFFE57373)
    }
}

private fun getFormattedValue(value: Double, parameter: String): String {
    return when (parameter) {
        "temperature" -> "${value.toInt()}°C"
        "humidity" -> "${value.toInt()}%"
        "lux" -> "${value.toInt()}"
        "moisture" -> "${value.toInt()}%"
        else -> "${value.toInt()}"
    }
}

private fun getTimeLabel(dataPoint: AggregatedData, aggregationType: AggregationType): String {
    return when (aggregationType) {
        AggregationType.HOUR -> {
            // Usar la etiqueta precalculada o generar desde el timestamp
            if (dataPoint.label.isNotEmpty()) {
                dataPoint.label
            } else {
                // Generar desde timestamp en zona horaria de México
                val mexicoZone = ZoneId.of("America/Mexico_City")
                val instant = Instant.ofEpochSecond(dataPoint.timestamp)
                val zonedDateTime = instant.atZone(mexicoZone)
                zonedDateTime.format(DateTimeFormatter.ofPattern("HH:00"))
            }
        }
        AggregationType.DAY -> {
            // Usar la etiqueta precalculada o generar desde el timestamp
            if (dataPoint.label.isNotEmpty()) {
                dataPoint.label
            } else {
                // Generar desde timestamp en zona horaria de México
                val mexicoZone = ZoneId.of("America/Mexico_City")
                val instant = Instant.ofEpochSecond(dataPoint.timestamp)
                val zonedDateTime = instant.atZone(mexicoZone)
                zonedDateTime.format(DateTimeFormatter.ofPattern("dd/MM"))
            }
        }
    }
}
