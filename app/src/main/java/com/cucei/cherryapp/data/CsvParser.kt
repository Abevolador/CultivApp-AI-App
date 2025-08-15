package com.cucei.cherryapp.data

import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {
    
    /**
     * Parsea un archivo CSV y retorna una lista de PlantRecord
     */
    fun parsePlantDataCsv(inputStream: InputStream): List<PlantRecord> {
        val records = mutableListOf<PlantRecord>()
        
        try {
            // Verificar que el stream no esté cerrado
            if (inputStream.available() == -1) {
                throw Exception("Stream cerrado o no disponible")
            }
            
            val reader = BufferedReader(InputStreamReader(inputStream))
            var lineNumber = 0
            var delimiter = ',' // Delimitador por defecto
            var firstLine: String? = null
            
            try {
                // Leer la primera línea para detectar delimitador
                firstLine = reader.readLine()
                if (firstLine != null) {
                    delimiter = detectDelimiter(firstLine)
                    Log.d("CsvParser", "Delimitador detectado: '$delimiter'")
                    Log.d("CsvParser", "Primera línea: $firstLine")
                }
                
                // Leer el resto de las líneas
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    lineNumber++
                    
                    try {
                        val record = parseCsvLine(line!!, delimiter)
                        if (record != null) {
                            records.add(record)
                        }
                        
                        // Log cada 100 registros para debugging
                        if (lineNumber % 100 == 0) {
                            Log.d("CsvParser", "Procesados $lineNumber registros...")
                        }
                    } catch (e: Exception) {
                        Log.w("CsvParser", "Error parsing line $lineNumber: ${e.message}")
                        Log.w("CsvParser", "Linea problemática: $line")
                        // Continuar con la siguiente línea
                    }
                }
            } finally {
                reader.close()
            }
            
            Log.d("CsvParser", "Total de registros procesados: ${records.size}")
            
        } catch (e: Exception) {
            Log.e("CsvParser", "Error reading CSV file: ${e.message}")
            Log.e("CsvParser", "Stack trace: ${e.stackTraceToString()}")
            throw e
        }
        
        return records
    }
    
    /**
     * Detecta el delimitador usado en el archivo
     */
    private fun detectDelimiter(firstLine: String): Char {
        val delimiters = listOf(',', ';', '\t')
        val delimiterCounts = delimiters.associateWith { delimiter ->
            firstLine.count { it == delimiter }
        }
        
        return delimiterCounts.maxByOrNull { it.value }?.key ?: ','
    }
    
    /**
     * Parsea una línea individual del CSV
     */
    private fun parseCsvLine(line: String, delimiter: Char): PlantRecord? {
        if (line.isBlank()) return null
        
        try {
            // Dividir la línea por el delimitador detectado
            val values = line.split(delimiter)
            
            if (values.size < 7) {
                Log.w("CsvParser", "Invalid line format (${values.size} columns): $line")
                return null
            }
            
            return PlantRecord(
                id = values[0].trim(),
                timestamp = values[1].trim().toLongOrNull() ?: 0L,
                temperature = values[2].trim().toDoubleOrNull() ?: 0.0,
                relativeHumidity = values[3].trim().toDoubleOrNull() ?: 0.0,
                lux = values[4].trim().toDoubleOrNull() ?: 0.0,
                moistureValue = values[5].trim().toDoubleOrNull() ?: 0.0,
                moisturePercent = values[6].trim().toDoubleOrNull() ?: 0.0
            )
        } catch (e: Exception) {
            Log.e("CsvParser", "Error parsing line: $line", e)
            return null
        }
    }
    
    /**
     * Valida que el archivo CSV tenga el formato correcto
     */
    fun validateCsvFormat(inputStream: InputStream): Boolean {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val firstLine = reader.readLine()
            reader.close()
            
            firstLine?.let { line ->
                // Detectar delimitador
                val delimiter = detectDelimiter(line)
                val headers = line.split(delimiter).map { it.trim() }
                val expectedHeaders = listOf(
                    "_id", "timestamp", "temperature", "relative_humidity", 
                    "lux", "moisture_value", "moisture_percent"
                )
                
                // Verificar que tenga al menos 7 columnas (número mínimo requerido)
                if (headers.size < 7) {
                    Log.w("CsvParser", "CSV tiene menos de 7 columnas: ${headers.size}")
                    return false
                }
                
                // Verificar que contenga al menos algunas de las columnas esperadas
                val hasRequiredHeaders = expectedHeaders.any { expected -> 
                    headers.any { it.equals(expected, ignoreCase = true) }
                }
                
                if (!hasRequiredHeaders) {
                    Log.w("CsvParser", "CSV no contiene columnas esperadas. Headers encontrados: $headers")
                }
                
                hasRequiredHeaders
            } ?: false
        } catch (e: Exception) {
            Log.e("CsvParser", "Error validating CSV format: ${e.message}")
            false
        }
    }
} 