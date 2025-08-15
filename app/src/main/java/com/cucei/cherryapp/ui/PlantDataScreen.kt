package com.cucei.cherryapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.cucei.cherryapp.data.PlantRecord
import com.cucei.cherryapp.data.DayGroup
import com.cucei.cherryapp.data.HourGroup
import com.cucei.cherryapp.viewmodel.PlantDataState
import com.cucei.cherryapp.viewmodel.PlantDataViewModel
import com.cucei.cherryapp.ui.theme.BlackText
import com.cucei.cherryapp.ui.theme.WhiteButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDataScreen(
    onBack: () -> Unit,
    viewModel: PlantDataViewModel,
    onNavigateToCharts: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val records by viewModel.records.collectAsState()
    val groupedData by viewModel.groupedData.collectAsState()
    
    // Launcher para seleccionar archivo CSV (más flexible)
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                // Crear una copia del stream para evitar problemas de cierre
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    // Leer todo el contenido en memoria para evitar problemas de stream
                    val content = inputStream.bufferedReader().readText()
                    val byteArrayInputStream = content.byteInputStream()
                    viewModel.loadCsvData(byteArrayInputStream)
                } else {
                    // Mostrar error si no se puede abrir el archivo
                    Log.e("PlantDataScreen", "No se pudo abrir el archivo: $uri")
                }
            } catch (e: Exception) {
                Log.e("PlantDataScreen", "Error al abrir archivo: ${e.message}")
                // El error se maneja en el ViewModel
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos de Plantas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { csvLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Cargar CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            // Botón de gráficos (solo visible si hay datos)
            if (state is PlantDataState.Success && records.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onNavigateToCharts,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = "Ver Gráficos")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Botón para cargar CSV
            if (state is PlantDataState.Loading && records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "No hay datos cargados",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = { csvLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhiteButton,
                                contentColor = BlackText
                            )
                        ) {
                            Text("📊 Cargar CSV de Plantas")
                        }
                    }
                }
            } else {
                // Contenido principal
                val currentState = state
                when (currentState) {
                    is PlantDataState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Cargando datos...")
                            }
                        }
                    }
                    
                    is PlantDataState.Success -> {
                        // Estadísticas
                        StatisticsCard(statistics = currentState.statistics)
                        
                        // Lista agrupada de registros
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groupedData?.let { groups ->
                                itemsIndexed(groups.dayGroups) { dayIndex, dayGroup ->
                                    DayGroupCard(
                                        dayGroup = dayGroup,
                                        onDayToggle = { viewModel.toggleDayExpansion(dayGroup.date) },
                                        onHourToggle = { hour ->
                                            viewModel.toggleHourExpansion(dayGroup.date, hour)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    is PlantDataState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Error",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    currentState.message,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = { csvLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WhiteButton,
                                        contentColor = BlackText
                                    )
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                    
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun DayGroupCard(
    dayGroup: DayGroup,
    onDayToggle: () -> Unit,
    onHourToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header del día
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        dayGroup.date,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dayGroup.dayOfWeek,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${dayGroup.recordCount} registros",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onDayToggle) {
                        Icon(
                            if (dayGroup.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (dayGroup.isExpanded) "Contraer" else "Expandir"
                        )
                    }
                }
            }
            
            // Estadísticas del día
            if (dayGroup.isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("🌡️", "Temp", "${"%.1f".format(dayGroup.averageTemperature)}°C")
                    StatItem("💧", "Hum", "${"%.1f".format(dayGroup.averageHumidity)}%")
                    StatItem("🔆", "Lux", "${"%.0f".format(dayGroup.averageLux)}")
                    StatItem("💧", "Suelo", "${"%.1f".format(dayGroup.averageMoisture)}%")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Grupos de hora
                dayGroup.hourGroups.forEach { hourGroup ->
                    HourGroupCard(
                        hourGroup = hourGroup,
                        onHourToggle = { onHourToggle(hourGroup.hour) }
                    )
                }
            }
        }
    }
}

@Composable
fun HourGroupCard(
    hourGroup: HourGroup,
    onHourToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header de la hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    hourGroup.hour,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${hourGroup.recordCount} registros",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onHourToggle) {
                        Icon(
                            if (hourGroup.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (hourGroup.isExpanded) "Contraer" else "Expandir",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Registros individuales
            if (hourGroup.isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                hourGroup.records.forEach { record ->
                    PlantRecordCard(record = record)
                }
            }
        }
    }
}

@Composable
fun StatisticsCard(statistics: com.cucei.cherryapp.viewmodel.PlantStatistics) {
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
                "📈 Estadísticas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("🌡️", "Temp. Prom.", "${"%.1f".format(statistics.averageTemperature)}°C")
                StatItem("💧", "Hum. Prom.", "${"%.1f".format(statistics.averageHumidity)}%")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("🔆", "Lux Prom.", "${"%.0f".format(statistics.averageLux)}")
                StatItem("💧", "Hum. Suelo", "${"%.1f".format(statistics.averageMoisture)}%")
            }
        }
    }
}

@Composable
fun StatItem(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$icon $label",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlantRecordCard(record: PlantRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Fecha y hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    record.getFormattedTime(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Datos principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DataItem("🌡️", "Temp", "${"%.1f".format(record.temperature)}°C")
                DataItem("💧", "Hum", "${"%.1f".format(record.relativeHumidity)}%")
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DataItem("🔆", "Lux", "${"%.0f".format(record.lux)}")
                DataItem("💧", "Suelo", "${"%.1f".format(record.moisturePercent)}%")
            }
        }
    }
}

@Composable
fun DataItem(icon: String, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "$icon $label:",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
} 