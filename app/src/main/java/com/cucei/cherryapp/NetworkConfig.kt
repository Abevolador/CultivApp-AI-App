package com.cucei.cherryapp

import android.content.Context
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Configuración universal de red para CultivApp
 * Permite conexiones a cualquier red local sin restricciones
 */
object NetworkConfig {
    
    private const val TAG = "NetworkConfig"
    
    // Configuraciones de timeout
    private const val DEFAULT_CONNECT_TIMEOUT = 10000L  // 10 segundos
    private const val DEFAULT_READ_TIMEOUT = 15000L     // 15 segundos
    private const val FAST_TIMEOUT = 5000L              // 5 segundos para redes rápidas
    private const val SLOW_TIMEOUT = 20000L             // 20 segundos para redes lentas
    
    // Configuraciones de reintentos
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 1000L
    
    /**
     * Configuración de red según el tipo de entorno
     */
    enum class NetworkEnvironment {
        HOME,           // Casa - redes rápidas
        LABORATORY,     // Laboratorio - redes variables
        UNIVERSITY,     // Universidad - redes corporativas
        UNKNOWN         // Desconocido - configuración conservadora
    }
    
    /**
     * Detecta el entorno de red basándose en la IP local
     */
    fun detectNetworkEnvironment(localIP: String): NetworkEnvironment {
        return when {
            localIP.startsWith("192.168.") -> NetworkEnvironment.HOME
            localIP.startsWith("10.") -> NetworkEnvironment.UNIVERSITY
            localIP.startsWith("172.") -> NetworkEnvironment.LABORATORY
            else -> NetworkEnvironment.UNKNOWN
        }
    }
    
    /**
     * Obtiene timeouts según el entorno de red
     */
    fun getTimeouts(environment: NetworkEnvironment): Pair<Long, Long> {
        return when (environment) {
            NetworkEnvironment.HOME -> Pair(FAST_TIMEOUT, FAST_TIMEOUT)
            NetworkEnvironment.LABORATORY -> Pair(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT)
            NetworkEnvironment.UNIVERSITY -> Pair(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT)
            NetworkEnvironment.UNKNOWN -> Pair(SLOW_TIMEOUT, SLOW_TIMEOUT)
        }
    }
    
    /**
     * Configura una conexión HTTP con parámetros optimizados
     */
    fun configureConnection(
        connection: HttpURLConnection,
        environment: NetworkEnvironment,
        isTestConnection: Boolean = false
    ) {
        val (connectTimeout, readTimeout) = getTimeouts(environment)
        
        // Configuración básica
        connection.connectTimeout = connectTimeout.toInt()
        connection.readTimeout = readTimeout.toInt()
        connection.requestMethod = "GET"
        
        // Headers para mejor compatibilidad
        connection.setRequestProperty("User-Agent", "CultivApp/1.0")
        connection.setRequestProperty("Accept", "application/json, text/plain, */*")
        connection.setRequestProperty("Connection", "close")
        
        // Configuraciones específicas según el entorno
        when (environment) {
            NetworkEnvironment.HOME -> {
                // Redes domésticas - configuración agresiva
                connection.setRequestProperty("Cache-Control", "no-cache")
            }
            NetworkEnvironment.LABORATORY -> {
                // Laboratorios - configuración equilibrada
                connection.setRequestProperty("Cache-Control", "no-cache")
            }
            NetworkEnvironment.UNIVERSITY -> {
                // Universidades - configuración conservadora
                connection.setRequestProperty("Cache-Control", "no-cache")
                connection.setRequestProperty("Pragma", "no-cache")
            }
            NetworkEnvironment.UNKNOWN -> {
                // Desconocido - configuración más conservadora
                connection.setRequestProperty("Cache-Control", "no-cache")
                connection.setRequestProperty("Pragma", "no-cache")
            }
        }
        
        // Para conexiones de prueba, usar timeouts más cortos
        if (isTestConnection) {
            connection.connectTimeout = (connectTimeout / 2).toInt()
            connection.readTimeout = (readTimeout / 2).toInt()
        }
        
        Log.d(TAG, "Conexión configurada para entorno: $environment, timeouts: ${connectTimeout}ms/${readTimeout}ms")
    }
    
    /**
     * Verifica si una URL es accesible con reintentos
     */
    suspend fun testConnectionWithRetry(
        url: String,
        environment: NetworkEnvironment,
        maxRetries: Int = MAX_RETRIES
    ): ConnectionTestResult {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "Intento de conexión ${attempt + 1}/$maxRetries a: $url")
                
                val result = testSingleConnection(url, environment)
                if (result.isSuccessful) {
                    Log.d(TAG, "Conexión exitosa en intento ${attempt + 1}")
                    return result
                }
                
                lastException = result.exception
                
                // Esperar antes del siguiente intento (excepto en el último)
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Error en intento ${attempt + 1}: ${e.message}")
                lastException = e
                
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            }
        }
        
        Log.e(TAG, "Todos los intentos de conexión fallaron después de $maxRetries intentos")
        return ConnectionTestResult(
            isSuccessful = false,
            responseCode = -1,
            responseTime = -1,
            exception = lastException,
            attempts = maxRetries
        )
    }
    
    /**
     * Prueba una sola conexión
     */
    private suspend fun testSingleConnection(url: String, environment: NetworkEnvironment): ConnectionTestResult {
        val startTime = System.currentTimeMillis()
        var connection: HttpURLConnection? = null
        
        try {
            val urlObj = URL(url)
            connection = urlObj.openConnection() as HttpURLConnection
            
            // Configurar la conexión
            configureConnection(connection, environment, isTestConnection = true)
            
            // Realizar la conexión
            val responseCode = connection.responseCode
            val responseTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Respuesta del servidor: $responseCode en ${responseTime}ms")
            
            return ConnectionTestResult(
                isSuccessful = responseCode in 200..299,
                responseCode = responseCode,
                responseTime = responseTime,
                exception = null,
                attempts = 1
            )
            
        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "Error en conexión: ${e.message}")
            
            return ConnectionTestResult(
                isSuccessful = false,
                responseCode = -1,
                responseTime = responseTime,
                exception = e,
                attempts = 1
            )
        } finally {
            connection?.disconnect()
        }
    }
    
    /**
     * Obtiene información detallada de la red
     */
    fun getNetworkDiagnostics(context: Context): NetworkDiagnostics {
        val networkInfo = NetworkUtils.detectLocalNetwork(context)
        val connectivityInfo = NetworkUtils.getConnectivityInfo(context)
        val environment = detectNetworkEnvironment(networkInfo.localIP)
        val (connectTimeout, readTimeout) = getTimeouts(environment)
        
        return NetworkDiagnostics(
            localNetworkInfo = networkInfo,
            connectivityInfo = connectivityInfo,
            environment = environment,
            suggestedTimeouts = Pair(connectTimeout, readTimeout),
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Resultado de una prueba de conexión
 */
data class ConnectionTestResult(
    val isSuccessful: Boolean,
    val responseCode: Int,
    val responseTime: Long,
    val exception: Exception?,
    val attempts: Int
) {
    val isTimeout: Boolean
        get() = exception?.message?.contains("timeout", ignoreCase = true) == true
    
    val isConnectionRefused: Boolean
        get() = exception?.message?.contains("connection refused", ignoreCase = true) == true
    
    val isNetworkUnreachable: Boolean
        get() = exception?.message?.contains("network unreachable", ignoreCase = true) == true
    
    fun getErrorMessage(): String {
        return when {
            isSuccessful -> "Conexión exitosa (${responseTime}ms)"
            isTimeout -> "Timeout de conexión después de $attempts intentos"
            isConnectionRefused -> "Conexión rechazada por el servidor"
            isNetworkUnreachable -> "Red no alcanzable"
            responseCode > 0 -> "Error HTTP: $responseCode"
            exception != null -> "Error: ${exception.message}"
            else -> "Error desconocido de conexión"
        }
    }
}

/**
 * Diagnósticos completos de la red
 */
data class NetworkDiagnostics(
    val localNetworkInfo: LocalNetworkInfo,
    val connectivityInfo: ConnectivityInfo,
    val environment: NetworkConfig.NetworkEnvironment,
    val suggestedTimeouts: Pair<Long, Long>,
    val timestamp: Long
) {
    fun getSummary(): String {
        return buildString {
            appendLine("🌐 Diagnóstico de Red:")
            appendLine("📍 IP Local: ${localNetworkInfo.localIP}")
            appendLine("🏠 Entorno: ${environment.name}")
            appendLine("📡 Tipo: ${localNetworkInfo.networkClass.name}")
            appendLine("🔌 WiFi: ${if (connectivityInfo.isWifi) "✅" else "❌"}")
            appendLine("🌐 Internet: ${if (connectivityInfo.hasInternet) "✅" else "❌"}")
            appendLine("⏱️ Timeouts sugeridos: ${suggestedTimeouts.first}ms/${suggestedTimeouts.second}ms")
            appendLine("💡 IPs sugeridas: ${localNetworkInfo.suggestedIPs.take(3).joinToString(", ")}")
        }
    }
}
