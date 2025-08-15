package com.cucei.cherryapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cucei.cherryapp.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class PlantDataState {
    object Loading : PlantDataState()
    data class Success(
        val records: List<PlantRecord>,
        val groupedData: PlantDataGroups,
        val statistics: PlantStatistics
    ) : PlantDataState()
    data class Error(val message: String) : PlantDataState()
}

data class PlantStatistics(
    val totalRecords: Int,
    val averageTemperature: Double,
    val maxTemperature: Double,
    val minTemperature: Double,
    val averageHumidity: Double,
    val maxHumidity: Double,
    val minHumidity: Double,
    val averageLux: Double,
    val maxLux: Double,
    val minLux: Double,
    val averageMoisture: Double,
    val maxMoisture: Double,
    val minMoisture: Double
)

class PlantDataViewModel : ViewModel() {
    
    private val _state = MutableStateFlow<PlantDataState>(PlantDataState.Loading)
    val state: StateFlow<PlantDataState> = _state.asStateFlow()
    
    private val _records = MutableStateFlow<List<PlantRecord>>(emptyList())
    val records: StateFlow<List<PlantRecord>> = _records.asStateFlow()
    
    private val _groupedData = MutableStateFlow<PlantDataGroups?>(null)
    val groupedData: StateFlow<PlantDataGroups?> = _groupedData.asStateFlow()
    
    private val _aggregatedData = MutableStateFlow<Map<AggregationType, List<AggregatedData>>>(emptyMap())
    val aggregatedData: StateFlow<Map<AggregationType, List<AggregatedData>>> = _aggregatedData.asStateFlow()
    
    // Para navegación por días
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()
    
    private val _availableDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val availableDates: StateFlow<List<LocalDate>> = _availableDates.asStateFlow()
    
    fun loadCsvData(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                _state.value = PlantDataState.Loading
                
                // Leer el contenido completo del InputStream
                val content = inputStream.bufferedReader().use { it.readText() }
                val byteArrayInputStream = content.byteInputStream()
                
                val records = CsvParser.parsePlantDataCsv(byteArrayInputStream)
                
                if (records.isNotEmpty()) {
                    _records.value = records
                    
                    // Agrupar datos
                    val groupedData = PlantDataGrouper.groupRecords(records)
                    _groupedData.value = groupedData
                    
                    // Generar datos agregados
                    generateAggregatedData()
                    
                    // Configurar navegación por días
                    setupDateNavigation(groupedData)
                    
                    // Calcular estadísticas
                    val statistics = getStatistics(records)
                    
                    _state.value = PlantDataState.Success(records, groupedData, statistics)
                } else {
                    _state.value = PlantDataState.Error("No se encontraron registros válidos en el archivo CSV")
                }
            } catch (e: Exception) {
                Log.e("PlantDataViewModel", "Error loading CSV data", e)
                _state.value = PlantDataState.Error("Error al cargar el archivo CSV: ${e.localizedMessage}")
            }
        }
    }
    
    private fun setupDateNavigation(groupedData: PlantDataGroups) {
        val dates = groupedData.availableDates
        _availableDates.value = dates
        
        // Seleccionar la fecha más reciente por defecto
        if (dates.isNotEmpty()) {
            _selectedDate.value = dates.last()
        }
    }
    
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        generateAggregatedData()
    }
    
    fun nextDay() {
        val currentDate = _selectedDate.value
        val availableDates = _availableDates.value
        
        if (currentDate != null && availableDates.isNotEmpty()) {
            val currentIndex = availableDates.indexOf(currentDate)
            if (currentIndex < availableDates.size - 1) {
                selectDate(availableDates[currentIndex + 1])
            }
        }
    }
    
    fun previousDay() {
        val currentDate = _selectedDate.value
        val availableDates = _availableDates.value
        
        if (currentDate != null && availableDates.isNotEmpty()) {
            val currentIndex = availableDates.indexOf(currentDate)
            if (currentIndex > 0) {
                selectDate(availableDates[currentIndex - 1])
            }
        }
    }
    
    fun hasNextDay(): Boolean {
        val currentDate = _selectedDate.value
        val availableDates = _availableDates.value
        
        if (currentDate != null && availableDates.isNotEmpty()) {
            val currentIndex = availableDates.indexOf(currentDate)
            return currentIndex < availableDates.size - 1
        }
        return false
    }
    
    fun hasPreviousDay(): Boolean {
        val currentDate = _selectedDate.value
        val availableDates = _availableDates.value
        
        if (currentDate != null && availableDates.isNotEmpty()) {
            val currentIndex = availableDates.indexOf(currentDate)
            return currentIndex > 0
        }
        return false
    }
    
    fun getSelectedDateFormatted(): String {
        val date = _selectedDate.value
        return if (date != null) {
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } else {
            "Sin fecha seleccionada"
        }
    }
    
    fun toggleDayExpansion(date: String) {
        val currentGroupedData = _groupedData.value ?: return
        val updatedDayGroups = currentGroupedData.dayGroups.map { dayGroup ->
            if (dayGroup.date == date) {
                dayGroup.copy(isExpanded = !dayGroup.isExpanded)
            } else {
                dayGroup
            }
        }
        _groupedData.value = currentGroupedData.copy(dayGroups = updatedDayGroups)
    }
    
    fun toggleHourExpansion(date: String, hour: String) {
        val currentGroupedData = _groupedData.value ?: return
        val updatedDayGroups = currentGroupedData.dayGroups.map { dayGroup ->
            if (dayGroup.date == date) {
                val updatedHourGroups = dayGroup.hourGroups.map { hourGroup ->
                    if (hourGroup.hour == hour) {
                        hourGroup.copy(isExpanded = !hourGroup.isExpanded)
                    } else {
                        hourGroup
                    }
                }
                dayGroup.copy(hourGroups = updatedHourGroups)
            } else {
                dayGroup
            }
        }
        _groupedData.value = currentGroupedData.copy(dayGroups = updatedDayGroups)
    }
    
    private fun generateAggregatedData() {
        val records = _records.value
        if (records.isEmpty()) return
        
        val selectedDate = _selectedDate.value
        
        val aggregatedData = mutableMapOf<AggregationType, List<AggregatedData>>()
        
        // Para agregación por hora, usar datos del día seleccionado si está disponible
        if (selectedDate != null) {
            aggregatedData[AggregationType.HOUR] = PlantDataGrouper.aggregateDataForDay(records, selectedDate)
        } else {
            aggregatedData[AggregationType.HOUR] = PlantDataGrouper.aggregateData(records, AggregationType.HOUR)
        }
        
        aggregatedData[AggregationType.DAY] = PlantDataGrouper.aggregateData(records, AggregationType.DAY)
        
        _aggregatedData.value = aggregatedData
    }
    
    fun getAggregatedData(type: AggregationType): List<AggregatedData> {
        return _aggregatedData.value[type] ?: emptyList()
    }
    
    fun filterRecordsByDate(startDate: LocalDate, endDate: LocalDate): List<PlantRecord> {
        return _records.value.filter { record ->
            val recordDate = record.getLocalDate()
            recordDate >= startDate && recordDate <= endDate
        }
    }
    
    private fun getStatistics(records: List<PlantRecord>): PlantStatistics {
        if (records.isEmpty()) {
            return PlantStatistics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        val temperatures = records.map { it.temperature }
        val humidities = records.map { it.relativeHumidity }
        val luxValues = records.map { it.lux }
        val moistureValues = records.map { it.moisturePercent }
        
        return PlantStatistics(
            totalRecords = records.size,
            averageTemperature = temperatures.average(),
            maxTemperature = temperatures.maxOrNull() ?: 0.0,
            minTemperature = temperatures.minOrNull() ?: 0.0,
            averageHumidity = humidities.average(),
            maxHumidity = humidities.maxOrNull() ?: 0.0,
            minHumidity = humidities.minOrNull() ?: 0.0,
            averageLux = luxValues.average(),
            maxLux = luxValues.maxOrNull() ?: 0.0,
            minLux = luxValues.minOrNull() ?: 0.0,
            averageMoisture = moistureValues.average(),
            maxMoisture = moistureValues.maxOrNull() ?: 0.0,
            minMoisture = moistureValues.minOrNull() ?: 0.0
        )
    }
    
    fun clearData() {
        _records.value = emptyList()
        _groupedData.value = null
        _aggregatedData.value = emptyMap()
        _selectedDate.value = null
        _availableDates.value = emptyList()
        _state.value = PlantDataState.Loading
    }
} 