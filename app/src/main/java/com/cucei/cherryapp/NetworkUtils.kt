package com.cucei.cherryapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

/**
 * Utilidades de red inteligente para CultivApp
 * Detecta automáticamente redes locales y sugiere IPs probables
 */
object NetworkUtils {
    
    private const val TAG = "NetworkUtils"
    
    /**
     * Detecta automáticamente la red local del dispositivo
     */
    fun detectLocalNetwork(context: Context): LocalNetworkInfo {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isEthernet = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
            
            if (isWifi || isEthernet) {
                val localIP = getLocalIPAddress()
                val networkInfo = analyzeNetworkFromIP(localIP)
                
                Log.d(TAG, "Red detectada: $networkInfo")
                networkInfo
            } else {
                Log.w(TAG, "No se detectó conexión WiFi o Ethernet")
                LocalNetworkInfo.unknown()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detectando red local", e)
            LocalNetworkInfo.unknown()
        }
    }
    
    /**
     * Obtiene la IP local del dispositivo
     */
    private fun getLocalIPAddress(): String {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses
                
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress.hostAddress.indexOf(':') < 0) {
                        val ip = inetAddress.hostAddress
                        if (isLocalIP(ip)) {
                            Log.d(TAG, "IP local detectada: $ip")
                            return ip
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            Log.e(TAG, "Error obteniendo IP local", e)
        }
        return "192.168.1.100" // Fallback
    }
    
    /**
     * Verifica si una IP es local
     */
    private fun isLocalIP(ip: String): Boolean {
        return ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               ip.startsWith("172.") ||
               ip.startsWith("169.254.") ||
               ip == "127.0.0.1"
    }
    
    /**
     * Analiza la red basándose en la IP local
     */
    private fun analyzeNetworkFromIP(localIP: String): LocalNetworkInfo {
        val parts = localIP.split(".")
        if (parts.size == 4) {
            val networkClass = when {
                localIP.startsWith("192.168.") -> NetworkClass.CLASS_C
                localIP.startsWith("10.") -> NetworkClass.CLASS_A
                localIP.startsWith("172.") -> NetworkClass.CLASS_B
                else -> NetworkClass.UNKNOWN
            }
            
            val baseNetwork = "${parts[0]}.${parts[1]}"
            val suggestedIPs = generateSuggestedIPs(parts[0], parts[1], parts[2])
            
            return LocalNetworkInfo(
                networkClass = networkClass,
                baseNetwork = baseNetwork,
                localIP = localIP,
                suggestedIPs = suggestedIPs,
                isDetected = true
            )
        }
        
        return LocalNetworkInfo.unknown()
    }
    
    /**
     * Genera IPs sugeridas basándose en la red detectada
     */
    fun generateSuggestedIPs(first: String, second: String, third: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        when {
            first == "192" && second == "168" -> {
                // Red típica de casa/oficina
                suggestions.addAll(listOf(
                    "192.168.1.1",    // Router común
                    "192.168.1.100",  // Servidor común
                    "192.168.1.200",  // Servidor alternativo
                    "192.168.0.1",    // Router alternativo
                    "192.168.0.100"   // Servidor alternativo
                ))
            }
            first == "10" -> {
                // Red corporativa/universidad
                suggestions.addAll(listOf(
                    "10.0.0.1",       // Router principal
                    "10.0.1.1",       // Router secundario
                    "10.1.1.1",       // Router de departamento
                    "10.0.0.100",     // Servidor principal
                    "10.1.1.100"      // Servidor de departamento
                ))
            }
            first == "172" -> {
                // Red corporativa/universidad
                suggestions.addAll(listOf(
                    "172.16.1.1",     // Router principal
                    "172.16.0.1",     // Router alternativo
                    "172.17.1.1",     // Router secundario
                    "172.16.1.100",   // Servidor principal
                    "172.17.1.100"    // Servidor secundario
                ))
            }
        }
        
        // Agregar IPs específicas de la red actual
        suggestions.add("$first.$second.$third.1")      // Router de la red actual
        suggestions.add("$first.$second.$third.100")    // Servidor de la red actual
        suggestions.add("$first.$second.$third.200")    // Servidor alternativo
        
        return suggestions.distinct()
    }
    
    /**
     * Genera IPs sugeridas con puertos para la aplicación
     */
    fun generateSuggestedIPsWithPorts(first: String, second: String, third: String): List<String> {
        val baseIPs = generateSuggestedIPs(first, second, third)
        val ports = listOf("2000", "3000", "5000", "8080", "8000") // Puertos comunes
        
        return baseIPs.flatMap { ip ->
            ports.map { port -> "$ip:$port" }
        }.take(10) // Limitar a 10 sugerencias
    }
    
    /**
     * Verifica si una IP está en la misma red que el dispositivo
     */
    fun isInSameNetwork(deviceIP: String, targetIP: String): Boolean {
        try {
            val deviceParts = deviceIP.split(".")
            val targetParts = targetIP.split(".")
            
            if (deviceParts.size == 4 && targetParts.size == 4) {
                // Para redes clase C (192.168.x.x)
                if (deviceIP.startsWith("192.168.") && targetIP.startsWith("192.168.")) {
                    return deviceParts[2] == targetParts[2]
                }
                // Para redes clase A (10.x.x.x)
                if (deviceIP.startsWith("10.") && targetIP.startsWith("10.")) {
                    return deviceParts[1] == targetParts[1]
                }
                // Para redes clase B (172.x.x.x)
                if (deviceIP.startsWith("172.") && targetIP.startsWith("172.")) {
                    return deviceParts[1] == targetParts[1]
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando si IPs están en la misma red", e)
        }
        return false
    }
    
    /**
     * Obtiene información de conectividad
     */
    fun getConnectivityInfo(context: Context): ConnectivityInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        return ConnectivityInfo(
            isConnected = capabilities != null,
            isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
            isEthernet = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true,
            hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            hasValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        )
    }
}

/**
 * Información de la red local detectada
 */
data class LocalNetworkInfo(
    val networkClass: NetworkClass,
    val baseNetwork: String,
    val localIP: String,
    val suggestedIPs: List<String>,
    val isDetected: Boolean
) {
    companion object {
        fun unknown() = LocalNetworkInfo(
            networkClass = NetworkClass.UNKNOWN,
            baseNetwork = "Desconocida",
            localIP = "0.0.0.0",
            suggestedIPs = emptyList(),
            isDetected = false
        )
    }
}

/**
 * Clases de red
 */
enum class NetworkClass {
    CLASS_A,    // 10.x.x.x
    CLASS_B,    // 172.16.x.x - 172.31.x.x
    CLASS_C,    // 192.168.x.x
    UNKNOWN
}

/**
 * Información de conectividad
 */
data class ConnectivityInfo(
    val isConnected: Boolean,
    val isWifi: Boolean,
    val isEthernet: Boolean,
    val hasInternet: Boolean,
    val hasValidated: Boolean
)
