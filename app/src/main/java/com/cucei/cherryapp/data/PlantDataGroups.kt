package com.cucei.cherryapp.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// Clase para representar un grupo de registros por hora
data class HourGroup(
    val hour: String, // Formato: "09:00"
    val hourOfDay: Int, // 0-23
    val records: List<PlantRecord>,
    var isExpanded: Boolean = false
) {
    val recordCount: Int get() = records.size
    
    // Obtener valores promedio para esta hora
    val averageTemperature: Double get() = records.map { it.temperature }.average()
    val averageHumidity: Double get() = records.map { it.relativeHumidity }.average()
    val averageLux: Double get() = records.map { it.lux }.average()
    val averageMoisture: Double get() = records.map { it.moisturePercent }.average()
}

// Clase para representar un grupo de registros por día
data class DayGroup(
    val date: String, // Formato: "08/08/2023"
    val localDate: LocalDate, // Para navegación
    val dayOfWeek: String, // Formato: "Martes"
    val hourGroups: List<HourGroup>,
    var isExpanded: Boolean = false
) {
    val recordCount: Int get() = hourGroups.sumOf { it.records.size }
    val hourCount: Int get() = hourGroups.size
    
    // Obtener valores promedio para este día
    val averageTemperature: Double get() = hourGroups.flatMap { it.records }.map { it.temperature }.average()
    val averageHumidity: Double get() = hourGroups.flatMap { it.records }.map { it.relativeHumidity }.average()
    val averageLux: Double get() = hourGroups.flatMap { it.records }.map { it.lux }.average()
    val averageMoisture: Double get() = hourGroups.flatMap { it.records }.map { it.moisturePercent }.average()
}

// Clase para representar todos los datos agrupados
data class PlantDataGroups(
    val dayGroups: List<DayGroup>
) {
    val totalRecords: Int get() = dayGroups.sumOf { it.recordCount }
    val totalDays: Int get() = dayGroups.size
    
    // Obtener valores promedio generales
    val averageTemperature: Double get() = dayGroups.flatMap { it.hourGroups }.flatMap { it.records }.map { it.temperature }.average()
    val averageHumidity: Double get() = dayGroups.flatMap { it.hourGroups }.flatMap { it.records }.map { it.relativeHumidity }.average()
    val averageLux: Double get() = dayGroups.flatMap { it.hourGroups }.flatMap { it.records }.map { it.lux }.average()
    val averageMoisture: Double get() = dayGroups.flatMap { it.hourGroups }.flatMap { it.records }.map { it.moisturePercent }.average()
    
    // Obtener fechas disponibles para navegación
    val availableDates: List<LocalDate> get() = dayGroups.map { it.localDate }.sorted()
}

// Clase para datos agregados para gráficos
data class AggregatedData(
    val timestamp: Long,
    val temperature: Double,
    val humidity: Double,
    val lux: Double,
    val moisture: Double,
    val label: String, // Etiqueta para mostrar en el gráfico
    val hourOfDay: Int? = null, // Para navegación por hora
    val localDate: LocalDate? = null // Para navegación por día
)

// Enum para tipos de agregación (solo HOUR y DAY)
enum class AggregationType {
    HOUR, DAY
}

// Clase utilitaria para agrupar datos
object PlantDataGrouper {
    
    /**
     * Agrupa los registros por día y hora
     */
    fun groupRecords(records: List<PlantRecord>): PlantDataGroups {
        if (records.isEmpty()) return PlantDataGroups(emptyList())
        
        // Agrupar por día
        val dayGroups = records
            .groupBy { it.getLocalDate() }
            .map { (localDate, dayRecords) ->
                // Agrupar por hora dentro del día
                val hourGroups = dayRecords
                    .groupBy { it.getHourOfDay() }
                    .map { (hourOfDay, hourRecords) ->
                        HourGroup(
                            hour = String.format("%02d:00", hourOfDay),
                            hourOfDay = hourOfDay,
                            records = hourRecords.sortedBy { it.timestamp }
                        )
                    }
                    .sortedBy { it.hourOfDay }
                
                DayGroup(
                    date = localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    localDate = localDate,
                    dayOfWeek = dayRecords.first().getDayOfWeek(),
                    hourGroups = hourGroups
                )
            }
            .sortedByDescending { it.localDate } // Más reciente primero
        
        return PlantDataGroups(dayGroups)
    }
    
    /**
     * Agrega datos para gráficos según el tipo de agregación
     */
    fun aggregateData(records: List<PlantRecord>, type: AggregationType): List<AggregatedData> {
        if (records.isEmpty()) return emptyList()
        
        return when (type) {
            AggregationType.HOUR -> aggregateByHour(records)
            AggregationType.DAY -> aggregateByDay(records)
        }
    }
    
    /**
     * Agrega datos por hora para un día específico
     */
    fun aggregateDataForDay(records: List<PlantRecord>, targetDate: LocalDate): List<AggregatedData> {
        if (records.isEmpty()) return emptyList()
        
        val dayRecords = records.filter { it.getLocalDate() == targetDate }
        if (dayRecords.isEmpty()) return emptyList()
        
        // Crear datos para las 24 horas del día
        val hourlyData = mutableListOf<AggregatedData>()
        val mexicoZone = ZoneId.of("America/Mexico_City")
        
        for (hour in 0..23) {
            val hourRecords = dayRecords.filter { it.getHourOfDay() == hour }
            
            if (hourRecords.isNotEmpty()) {
                // Hay datos para esta hora
                val timestamp = hourRecords.first().getZonedDateTime()
                    .withHour(hour)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant()
                    .epochSecond
                
                hourlyData.add(
                    AggregatedData(
                        timestamp = timestamp,
                        temperature = hourRecords.map { it.temperature }.average(),
                        humidity = hourRecords.map { it.relativeHumidity }.average(),
                        lux = hourRecords.map { it.lux }.average(),
                        moisture = hourRecords.map { it.moisturePercent }.average(),
                        label = String.format("%02d:00", hour),
                        hourOfDay = hour,
                        localDate = targetDate
                    )
                )
            } else {
                // No hay datos para esta hora, crear punto vacío
                val timestamp = targetDate.atStartOfDay(mexicoZone)
                    .withHour(hour)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant()
                    .epochSecond
                
                hourlyData.add(
                    AggregatedData(
                        timestamp = timestamp,
                        temperature = 0.0,
                        humidity = 0.0,
                        lux = 0.0,
                        moisture = 0.0,
                        label = String.format("%02d:00", hour),
                        hourOfDay = hour,
                        localDate = targetDate
                    )
                )
            }
        }
        
        return hourlyData.sortedBy { it.hourOfDay }
    }
    
    private fun aggregateByHour(records: List<PlantRecord>): List<AggregatedData> {
        return records
            .groupBy { record ->
                val zonedDateTime = record.getZonedDateTime()
                zonedDateTime
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant()
                    .epochSecond
            }
            .map { (timestamp, hourRecords) ->
                val firstRecord = hourRecords.first()
                AggregatedData(
                    timestamp = timestamp,
                    temperature = hourRecords.map { it.temperature }.average(),
                    humidity = hourRecords.map { it.relativeHumidity }.average(),
                    lux = hourRecords.map { it.lux }.average(),
                    moisture = hourRecords.map { it.moisturePercent }.average(),
                    label = firstRecord.getHourOnly(),
                    hourOfDay = firstRecord.getHourOfDay(),
                    localDate = firstRecord.getLocalDate()
                )
            }
            .sortedBy { it.timestamp }
    }
    
    private fun aggregateByDay(records: List<PlantRecord>): List<AggregatedData> {
        return records
            .groupBy { record ->
                val zonedDateTime = record.getZonedDateTime()
                zonedDateTime
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant()
                    .epochSecond
            }
            .map { (timestamp, dayRecords) ->
                val firstRecord = dayRecords.first()
                AggregatedData(
                    timestamp = timestamp,
                    temperature = dayRecords.map { it.temperature }.average(),
                    humidity = dayRecords.map { it.relativeHumidity }.average(),
                    lux = dayRecords.map { it.lux }.average(),
                    moisture = dayRecords.map { it.moisturePercent }.average(),
                    label = firstRecord.getDateOnly(),
                    localDate = firstRecord.getLocalDate()
                )
            }
            .sortedBy { it.timestamp }
    }
}
