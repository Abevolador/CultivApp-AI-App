package com.cucei.cherryapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cucei.cherryapp.data.AggregatedData
import com.cucei.cherryapp.data.AggregationType
import com.cucei.cherryapp.viewmodel.PlantDataViewModel
import com.cucei.cherryapp.ui.components.AdvancedChartComponent
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    onBack: () -> Unit,
    viewModel: PlantDataViewModel
) {
    var selectedAggregation by remember { mutableStateOf(AggregationType.DAY) }
    var selectedParameter by remember { mutableStateOf("temperature") }
    var showChart by remember { mutableStateOf(false) }
    
    val aggregatedData by viewModel.aggregatedData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val availableDates by viewModel.availableDates.collectAsState()
    val currentData = aggregatedData[selectedAggregation] ?: emptyList()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Gráficos Avanzados",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header con información
            item {
                HeaderSection(
                    selectedDate = selectedDate,
                    availableDates = availableDates,
                    viewModel = viewModel
                )
            }
            
            // Selector de parámetro
            item {
                ParameterSelector(
                    selectedParameter = selectedParameter,
                    onParameterSelected = { 
                        selectedParameter = it
                        showChart = false
                    }
                )
            }
            
            // Selector de agregación temporal
            item {
                AggregationSelector(
                    selectedAggregation = selectedAggregation,
                    onAggregationSelected = { 
                        selectedAggregation = it
                        showChart = false
                    }
                )
            }
            
            // Navegación por días (solo para agregación por hora)
            item {
                if (selectedAggregation == AggregationType.HOUR && availableDates.isNotEmpty()) {
                    DayNavigationCard(
                        selectedDate = selectedDate,
                        availableDates = availableDates,
                        viewModel = viewModel
                    )
                }
            }
            
            // Botón de graficar
            item {
                ChartButton(
                    onClick = { showChart = true },
                    enabled = currentData.isNotEmpty()
                )
            }
            
            // Gráfico
            item {
                AnimatedVisibility(
                    visible = showChart,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ChartSection(
                        data = currentData,
                        parameter = selectedParameter,
                        aggregationType = selectedAggregation,
                        selectedDate = selectedDate
                    )
                }
            }
            
            // Estadísticas
            item {
                if (showChart && currentData.isNotEmpty()) {
                    StatisticsSection(
                        data = currentData,
                        parameter = selectedParameter
                    )
                }
            }
            
            // Espacio final
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun HeaderSection(
    selectedDate: java.time.LocalDate?,
    availableDates: List<java.time.LocalDate>,
    viewModel: PlantDataViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Analytics,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Visualización de Datos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Análisis detallado de sensores",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            if (selectedDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Datos disponibles: ${availableDates.size} días",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ParameterSelector(
    selectedParameter: String,
    onParameterSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Seleccionar Parámetro",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val parameters = listOf(
                "temperature" to (Icons.Outlined.Thermostat to "🌡️ Temperatura"),
                "humidity" to (Icons.Outlined.WaterDrop to "💧 Humedad"),
                "lux" to (Icons.Outlined.Lightbulb to "🔆 Luminosidad"),
                "moisture" to (Icons.Outlined.Grass to "🌱 Humedad Suelo")
            )
            
            parameters.forEach { (parameter, iconInfo) ->
                val (icon, label) = iconInfo
                ParameterChip(
                    parameter = parameter,
                    label = label,
                    icon = icon,
                    isSelected = selectedParameter == parameter,
                    onClick = { onParameterSelected(parameter) }
                )
            }
        }
    }
}

@Composable
fun ParameterChip(
    parameter: String,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AggregationSelector(
    selectedAggregation: AggregationType,
    onAggregationSelected: (AggregationType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Agregación Temporal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AggregationType.values().forEach { type ->
                    AggregationChip(
                        type = type,
                        isSelected = selectedAggregation == type,
                        onClick = { onAggregationSelected(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AggregationChip(
    type: AggregationType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                when (type) {
                    AggregationType.HOUR -> Icons.Outlined.Schedule
                    AggregationType.DAY -> Icons.Outlined.DateRange
                },
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onSecondaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                when (type) {
                    AggregationType.HOUR -> "Por Hora"
                    AggregationType.DAY -> "Por Día"
                },
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onSecondaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DayNavigationCard(
    selectedDate: java.time.LocalDate?,
    availableDates: List<java.time.LocalDate>,
    viewModel: PlantDataViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Navegación por Días",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón día anterior
                IconButton(
                    onClick = { viewModel.previousDay() },
                    enabled = viewModel.hasPreviousDay(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (viewModel.hasPreviousDay()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Día anterior",
                        tint = if (viewModel.hasPreviousDay()) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Fecha actual
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.getSelectedDateFormatted(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Datos por hora",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                // Botón día siguiente
                IconButton(
                    onClick = { viewModel.nextDay() },
                    enabled = viewModel.hasNextDay(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (viewModel.hasNextDay()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Día siguiente",
                        tint = if (viewModel.hasNextDay()) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChartButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Outlined.ShowChart,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Generar Gráfico",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ChartSection(
    data: List<AggregatedData>,
    parameter: String,
    aggregationType: AggregationType,
    selectedDate: java.time.LocalDate?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Título del gráfico
            val title = when (aggregationType) {
                AggregationType.HOUR -> {
                    val dateInfo = if (selectedDate != null) {
                        " - ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
                    } else ""
                    "Gráfico de ${getParameterLabel(parameter)} por Hora$dateInfo"
                }
                AggregationType.DAY -> "Gráfico de ${getParameterLabel(parameter)} por Día"
            }
            
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Gráfico avanzado
            AdvancedChartComponent(
                data = data,
                parameter = parameter,
                aggregationType = aggregationType,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StatisticsSection(
    data: List<AggregatedData>,
    parameter: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "📊 Estadísticas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            val values = data.map { getParameterValue(it, parameter) }
            val average = values.average()
            val max = values.maxOrNull() ?: 0.0
            val min = values.minOrNull() ?: 0.0
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    title = "Promedio",
                    value = getFormattedValue(average, parameter),
                    icon = Icons.Outlined.Analytics,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(
                    title = "Máximo",
                    value = getFormattedValue(max, parameter),
                    icon = Icons.Outlined.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(
                    title = "Mínimo",
                    value = getFormattedValue(min, parameter),
                    icon = Icons.Outlined.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Funciones utilitarias
fun getParameterLabel(parameter: String): String {
    return when (parameter) {
        "temperature" -> "🌡️ Temperatura"
        "humidity" -> "💧 Humedad"
        "lux" -> "🔆 Luminosidad"
        "moisture" -> "🌱 Humedad del Suelo"
        else -> "🌡️ Temperatura"
    }
}

fun getParameterValue(dataPoint: AggregatedData, parameter: String): Double {
    return when (parameter) {
        "temperature" -> dataPoint.temperature
        "humidity" -> dataPoint.humidity
        "lux" -> dataPoint.lux
        "moisture" -> dataPoint.moisture
        else -> dataPoint.temperature
    }
}

fun getFormattedValue(value: Double, parameter: String): String {
    return when (parameter) {
        "temperature" -> "${"%.1f".format(value)}°C"
        "humidity" -> "${"%.1f".format(value)}%"
        "lux" -> "${"%.0f".format(value)}"
        "moisture" -> "${"%.1f".format(value)}%"
        else -> "${"%.1f".format(value)}"
    }
}
