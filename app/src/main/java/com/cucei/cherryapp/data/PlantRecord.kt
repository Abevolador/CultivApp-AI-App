package com.cucei.cherryapp.data

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class PlantRecord(
    val id: String,
    val timestamp: Long,
    val temperature: Double,
    val relativeHumidity: Double,
    val lux: Double,
    val moistureValue: Double,
    val moisturePercent: Double
) {
    // Zona horaria de México (Guadalajara, Jalisco)
    private val mexicoZone = ZoneId.of("America/Mexico_City")
    
    // Función para obtener ZonedDateTime en zona horaria de México
    fun getZonedDateTime(): ZonedDateTime {
        // Asumimos que el timestamp está en segundos (no milisegundos)
        return Instant.ofEpochSecond(timestamp).atZone(mexicoZone)
    }
    
    // Función para obtener la fecha formateada en hora local de México
    fun getFormattedDate(): String {
        val zonedDateTime = getZonedDateTime()
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        return zonedDateTime.format(formatter)
    }
    
    // Función para obtener la hora formateada en hora local de México
    fun getFormattedTime(): String {
        val zonedDateTime = getZonedDateTime()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return zonedDateTime.format(formatter)
    }
    
    // Función para obtener solo la fecha (sin hora) en hora local de México
    fun getDateOnly(): String {
        val zonedDateTime = getZonedDateTime()
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return zonedDateTime.format(formatter)
    }
    
    // Función para obtener solo la hora (sin minutos/segundos) en hora local de México
    fun getHourOnly(): String {
        val zonedDateTime = getZonedDateTime()
        val formatter = DateTimeFormatter.ofPattern("HH:00")
        return zonedDateTime.format(formatter)
    }
    
    // Función para obtener el día de la semana en hora local de México
    fun getDayOfWeek(): String {
        val zonedDateTime = getZonedDateTime()
        val formatter = DateTimeFormatter.ofPattern("EEEE", Locale("es", "ES"))
        return zonedDateTime.format(formatter)
    }
    
    // Función para obtener el mes en hora local de México
    fun getMonth(): String {
        val zonedDateTime = getZonedDateTime()
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
        return zonedDateTime.format(formatter)
    }
    
    // Función para obtener la semana del año en hora local de México
    fun getWeekOfYear(): Int {
        return getZonedDateTime().get(java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR)
    }
    
    // Función para obtener solo la hora como entero (0-23)
    fun getHourOfDay(): Int {
        return getZonedDateTime().hour
    }
    
    // Función para obtener solo la fecha como LocalDate
    fun getLocalDate(): java.time.LocalDate {
        return getZonedDateTime().toLocalDate()
    }
    
    // Función para obtener el estado de humedad del suelo
    fun getMoistureStatus(): String {
        return when {
            moisturePercent > 50 -> "Húmedo"
            moisturePercent > 20 -> "Moderado"
            else -> "Seco"
        }
    }
    
    // Función para obtener el color del estado de humedad
    fun getMoistureColor(): androidx.compose.ui.graphics.Color {
        return when {
            moisturePercent > 50 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Verde
            moisturePercent > 20 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Naranja
            else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Rojo
        }
    }
    
    // Función para obtener la fecha como objeto Date (para compatibilidad)
    fun getDate(): Date {
        return Date.from(getZonedDateTime().toInstant())
    }
    
    // Función para obtener el timestamp en milisegundos
    fun getTimestampMillis(): Long {
        return timestamp * 1000
    }
} 