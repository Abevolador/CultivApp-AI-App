package com.cucei.cherryapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
fun AdvancedChartComponent(
    data: List<AggregatedData>,
    parameter: String,
    aggregationType: AggregationType,
    modifier: Modifier = Modifier,
    onDataPointClick: ((AggregatedData) -> Unit)? = null
) {
    var selectedPoint by remember { mutableStateOf<AggregatedData?>(null) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    
    val density = LocalDensity.current
    
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No hay datos para mostrar",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (aggregationType == AggregationType.HOUR) {
                    Text(
                        "para el día seleccionado",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    Column(modifier = modifier) {
        // Gráfico principal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        touchPosition = offset
                        val clickedPoint = findNearestDataPoint(data, offset, density)
                        if (clickedPoint != null) {
                            selectedPoint = clickedPoint
                            onDataPointClick?.invoke(clickedPoint)
                        }
                    }
                }
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val padding = 80f
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

                // Dibujar fondo del gráfico
                drawRect(
                    color = Color.White,
                    size = size
                )

                // Dibujar líneas de cuadrícula
                drawGridLines(
                    width = width,
                    height = height,
                    padding = padding,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    minValue = actualMin,
                    maxValue = actualMax,
                    parameter = parameter,
                    data = data,
                    aggregationType = aggregationType
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

                // Dibujar área bajo la línea (gradiente)
                if (data.size > 1) {
                    drawAreaUnderLine(
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
                    valueRange = valueRange,
                    selectedPoint = selectedPoint
                )
            }
        }

        // Información del punto seleccionado
        selectedPoint?.let { point ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Información del Punto",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Hora:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                getTimeLabel(point, aggregationType),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                getParameterLabel(parameter),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                getFormattedValue(getParameterValue(point, parameter), parameter),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun findNearestDataPoint(
    data: List<AggregatedData>,
    touchOffset: Offset,
    density: androidx.compose.ui.unit.Density
): AggregatedData? {
    if (data.isEmpty()) return null
    
    val padding = 80f
    val chartWidth = (density.run { 300.dp.toPx() }) - 2 * padding
    val chartHeight = (density.run { 350.dp.toPx() }) - 2 * padding
    
    val touchX = touchOffset.x - padding
    if (touchX < 0 || touchX > chartWidth) return null
    
    val dataIndex = (touchX / chartWidth * (data.size - 1)).toInt().coerceIn(0, data.size - 1)
    return data[dataIndex]
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLines(
    width: Float,
    height: Float,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minValue: Double,
    maxValue: Double,
    parameter: String,
    data: List<AggregatedData>,
    aggregationType: AggregationType
) {
    val gridColor = Color.Gray.copy(alpha = 0.1f)
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
    val timeDivisions = when (aggregationType) {
        AggregationType.HOUR -> 12 // Una línea cada 2 horas
        AggregationType.DAY -> minOf(data.size, 7) // Una línea por día
    }
    
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
    val axisColor = Color.Gray.copy(alpha = 0.6f)
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
                textSize = 28f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.DEFAULT
                isAntiAlias = true
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
    val axisColor = Color.Gray.copy(alpha = 0.6f)
    val strokeWidth = 2f

    // Eje X
    drawLine(
        color = axisColor,
        start = Offset(padding, padding + chartHeight),
        end = Offset(padding + chartWidth, padding + chartHeight),
        strokeWidth = strokeWidth
    )

    // Etiquetas del eje X
    val timeDivisions = when (aggregationType) {
        AggregationType.HOUR -> 12 // Una etiqueta cada 2 horas (00:00, 02:00, 04:00, etc.)
        AggregationType.DAY -> minOf(data.size, 7) // Una etiqueta por día
    }
    
    for (i in 0..timeDivisions) {
        val x = padding + (i * chartWidth / timeDivisions)
        drawLine(
            color = axisColor,
            start = Offset(x, padding + chartHeight),
            end = Offset(x, padding + chartHeight + 5f),
            strokeWidth = 1f
        )
        
        // Dibujar etiqueta de tiempo
        if (aggregationType == AggregationType.HOUR) {
            // Para vista por hora, mostrar etiquetas cada 2 horas
            val hour = i * 2
            if (hour <= 24) {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT
                        isAntiAlias = true
                    }
                    val timeLabel = String.format("%02d:00", hour)
                    canvas.nativeCanvas.drawText(
                        timeLabel,
                        x,
                        padding + chartHeight + 25f,
                        paint
                    )
                }
            }
        } else if (i < data.size) {
            // Para vista por día, usar las etiquetas de los datos
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT
                    isAntiAlias = true
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAreaUnderLine(
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
    val path = Path()
    var isFirst = true

    // Crear el área bajo la línea
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

    // Completar el área
    path.lineTo(padding + chartWidth, padding + chartHeight)
    path.lineTo(padding, padding + chartHeight)
    path.close()

    // Dibujar el área con gradiente
    val gradient = Brush.verticalGradient(
        colors = listOf(
            getParameterColor(parameter).copy(alpha = 0.3f),
            getParameterColor(parameter).copy(alpha = 0.1f)
        ),
        startY = padding,
        endY = padding + chartHeight
    )

    drawPath(path = path, brush = gradient)
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
    val strokeWidth = 4f

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
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
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
    valueRange: Double,
    selectedPoint: AggregatedData?
) {
    val pointColor = getParameterColor(parameter)
    val selectedColor = Color(0xFF2196F3) // Azul para punto seleccionado

    data.forEachIndexed { index, item ->
        val value = getParameterValue(item, parameter)
        val x = padding + (index * chartWidth / (data.size - 1))
        val y = padding + chartHeight - ((value - minValue) / valueRange * chartHeight).toFloat()

        val isSelected = selectedPoint == item
        val currentColor = if (isSelected) selectedColor else pointColor
        val pointRadius = if (isSelected) 8f else 6f

        // Sombra para el punto seleccionado
        if (isSelected) {
            drawCircle(
                color = currentColor.copy(alpha = 0.3f),
                radius = pointRadius + 4f,
                center = Offset(x, y)
            )
        }

        drawCircle(
            color = currentColor,
            radius = pointRadius,
            center = Offset(x, y)
        )

        // Borde blanco para el punto seleccionado
        if (isSelected) {
            drawCircle(
                color = Color.White,
                radius = pointRadius - 2f,
                center = Offset(x, y),
                style = Stroke(width = 2f)
            )
        }
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
        "temperature" -> "Temperatura"
        "humidity" -> "Humedad"
        "lux" -> "Luminosidad"
        "moisture" -> "Humedad del Suelo"
        else -> "Temperatura"
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
                zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
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
