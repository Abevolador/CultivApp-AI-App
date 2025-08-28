package com.cucei.cherryapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

// Data classes para el servidor
data class ServerPlant(
    val plant_id: String,
    val plant_name: String,
    val plant_type: String,
    val plant_date: Long,
    val plant_registered: Long,
    val plant_update_poll: Int,
    val update_poll_activated: Boolean,
    val device_mac: String,
    val soil_sens_num: Int
)

data class ServerPlantData(
    val plant_id: String,
    val timestamp: Long,
    val temperature: String,
    val relative_humidity: String,
    val lux: String,
    val moisture_value: String,
    val sensor_num: String
)

// Pantalla: Conectar al servidor
@Composable
fun ConectarServidorScreen(
    serverInput: String,
    onServerInputChange: (String) -> Unit,
    recentServers: List<String>,
    onRecentServerClick: (String) -> Unit,
    onConnectClick: () -> Unit,
    onTestConnection: () -> Unit,
    canConnect: Boolean
) {
    val context = LocalContext.current
    var showNetworkInfo by remember { mutableStateOf(false) }
    var networkDiagnostics by remember { mutableStateOf<NetworkDiagnostics?>(null) }
    
    // Detectar información de red al cargar la pantalla
    LaunchedEffect(Unit) {
        try {
            Log.d("NetworkInfo", "Iniciando detección de red...")
            
            // Obtener diagnóstico completo
            networkDiagnostics = NetworkConfig.getNetworkDiagnostics(context)
            
        } catch (e: Exception) {
            Log.e("NetworkInfo", "Error obteniendo información de red", e)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Conectar!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Información de red inteligente
        networkDiagnostics?.let { diagnostics ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🌐 Red Detectada: ${diagnostics.localNetworkInfo.baseNetwork}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { showNetworkInfo = !showNetworkInfo }
                        ) {
                            Text(if (showNetworkInfo) "Ocultar" else "Ver más")
                        }
                    }
                    
                    if (showNetworkInfo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "📍 IP Local: ${diagnostics.localNetworkInfo.localIP}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            "🏠 Entorno: ${diagnostics.environment.name}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Input IP:PUERTO
        OutlinedTextField(
            value = serverInput,
            onValueChange = { onServerInputChange(it.trim()) },
            label = { Text("IP:PUERTO (ej. 192.168.100.26:2000)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Botón conectar
        Button(
            onClick = onConnectClick,
            enabled = canConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (canConnect) "Conectar al servidor" else "Escribe IP y puerto")
        }

        // Lista de recientes
        if (recentServers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Conexiones recientes", fontWeight = FontWeight.Medium)
                TextButton(
                    onClick = { 
                        // Limpiar todas las IPs recientes
                        onRecentServerClick("") // Enviar string vacío como señal para limpiar todas
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("🗑️ Limpiar Todas", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentServers.forEach { addr ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón principal de la IP (expandible)
                        OutlinedButton(
                            onClick = { onRecentServerClick(addr) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { 
                            Text(addr, fontSize = 14.sp) 
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Botón de bote de basura individual
                        IconButton(
                            onClick = { 
                                // Enviar señal para eliminar esta IP específica
                                onRecentServerClick("DELETE:$addr")
                            },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                "🗑️",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Botón de prueba de conexión elegante
        if (canConnect) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "🔍 Prueba de Conexión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        "Verifica que tu servidor esté funcionando correctamente",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Button(
                        onClick = onTestConnection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "🔍 Probar Conexión",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// Lista de plantas del servidor
@Composable
fun ListaPlantasServidorScreen(
    serverInput: String,
    serverPlants: List<ServerPlant>,
    isConnecting: Boolean,
    onPlantClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isConnecting) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (serverPlants.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin plantas o error de conexión")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(serverPlants.size) { idx ->
                    val p = serverPlants[idx]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlantClick(p.plant_id) },
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.plant_name, fontWeight = FontWeight.Bold)
                            Text("${p.plant_type} • ID: ${p.plant_id}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Datos de la planta elegida
@Composable
fun DatosPlantaServidorScreen(
    serverInput: String,
    selectedPlantId: String?,
    serverPlantData: List<ServerPlantData>,
    isConnecting: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header con información de la planta
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🌱 Datos de la Planta",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "ID: $selectedPlantId",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    "Total de registros: ${serverPlantData.size}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        if (isConnecting) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Cargando datos de la planta...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else if (serverPlantData.isEmpty()) {
            Box(
                Modifier.fillMaxSize(), 
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "📊",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Sin registros para esta planta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        "La planta aún no tiene datos registrados",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                                textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(serverPlantData.size) { idx ->
                    val r = serverPlantData[idx]
                    // Debug: mostrar el timestamp original
                    Log.d("Timestamp", "Timestamp original: ${r.timestamp}")
                    
                    // El timestamp puede estar en miliseegundos o segundos, probamos ambos
                    val timestampMillis = if (r.timestamp > 1000000000000L) {
                        // Si es mayor a 1000000000000, está en miliseegundos
                        Log.d("Timestamp", "Detectado como miliseegundos: ${r.timestamp}")
                        r.timestamp
                    } else {
                        // Si es menor, está en segundos, convertimos a miliseegundos
                        Log.d("Timestamp", "Detectado como segundos: ${r.timestamp}, convirtiendo a: ${r.timestamp * 1000}")
                        r.timestamp * 1000
                    }
                    
                    val timestamp = java.util.Date(timestampMillis)
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale("es", "MX"))
                    dateFormat.timeZone = java.util.TimeZone.getTimeZone("GMT-06:00")
                    
                    Log.d("Timestamp", "Fecha formateada: ${dateFormat.format(timestamp)}")
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // Header del registro con timestamp
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🕐",
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Registro #${idx + 1}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    dateFormat.format(timestamp),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Datos organizados en filas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Temperatura
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "🌡️",
                                            fontSize = 24.sp
                                        )
                                        Text(
                                            "Temperatura",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            "${r.temperature}°C",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Humedad relativa
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "💧",
                                            fontSize = 24.sp
                                        )
                                        Text(
                                            "Humedad",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            "${r.relative_humidity}%",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Luminosidad
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "☀️",
                                            fontSize = 24.sp
                                        )
                                        Text(
                                            "Luminosidad",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "${r.lux} lux",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Humedad del suelo
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "🌱",
                                            fontSize = 24.sp
                                        )
                                        Text(
                                            "Suelo",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "${r.moisture_value}%",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Información del sensor
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🔌",
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Sensor: ${r.sensor_num}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Función para obtener plantas del servidor
suspend fun getServerPlants(serverInput: String): List<ServerPlant> {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("ServerConnection", "=== OBTENIENDO PLANTAS DEL SERVIDOR ===")
            Log.d("ServerConnection", "IP y Puerto: $serverInput")
            Log.d("ServerConnection", "URL: http://${serverInput}/plant")
            
            val url = java.net.URL("http://${serverInput}/plant")
            val conn = (url.openConnection() as java.net.HttpURLConnection)
            
            // Configuración estándar de red
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000  // 10 segundos
            conn.readTimeout = 10000     // 10 segundos
            conn.doInput = true
            conn.setRequestProperty("User-Agent", "CultivApp/1.0")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-cache")
            
            Log.d("ServerConnection", "Conectando con configuración estándar")
            
            val code = conn.responseCode
            if (code == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val json = org.json.JSONArray(body)
                val items = mutableListOf<ServerPlant>()
                
                for (i in 0 until json.length()) {
                    val o = json.getJSONObject(i)
                    items.add(
                        ServerPlant(
                            plant_id = o.optString("plant_id"),
                            plant_name = o.optString("plant_name"),
                            plant_type = o.optString("plant_type"),
                            plant_date = o.optLong("plant_date"),
                            plant_registered = o.optLong("plant_registered"),
                            plant_update_poll = o.optInt("plant_update_poll"),
                            update_poll_activated = o.optBoolean("update_poll_activated"),
                            device_mac = o.optString("device_mac"),
                            soil_sens_num = o.optInt("soil_sens_num")
                        )
                    )
                }
                Log.d("ServerConnection", "Plantas obtenidas exitosamente: ${items.size}")
                items
            } else {
                Log.e("ServerConnection", "Error HTTP: $code al obtener plantas")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ServerConnection", "Error obteniendo plantas: ${e.message}")
            emptyList()
        }
    }
}

// Función para obtener datos de una planta específica
suspend fun getServerPlantData(serverInput: String, plantId: String): List<ServerPlantData> {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("ServerConnection", "=== OBTENIENDO DATOS DE PLANTA ===")
            Log.d("ServerConnection", "IP y Puerto: $serverInput, Plant ID: $plantId")
            Log.d("ServerConnection", "URL: http://${serverInput}/plant_data")
            
            val url = java.net.URL("http://${serverInput}/plant_data")
            val conn = (url.openConnection() as java.net.HttpURLConnection)
            
            // Configuración estándar de red
            conn.requestMethod = "GET"
            conn.connectTimeout = 7000   // 7 segundos
            conn.readTimeout = 7000      // 7 segundos
            conn.doInput = true
            conn.setRequestProperty("User-Agent", "CultivApp/1.0")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-cache")
            
            Log.d("ServerConnection", "Conectando con configuración estándar")
            
            val code = conn.responseCode
            if (code == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val json = org.json.JSONArray(body)
                val items = mutableListOf<ServerPlantData>()
                
                for (i in 0 until json.length()) {
                    val o = json.getJSONObject(i)
                    if (o.optString("plant_id") == plantId) {
                        items.add(
                            ServerPlantData(
                                plant_id = o.optString("plant_id"),
                                timestamp = o.optLong("timestamp"),
                                temperature = o.optString("temperature"),
                                relative_humidity = o.optString("relative_humidity"),
                                lux = o.optString("lux"),
                                moisture_value = o.optString("moisture_value"),
                                sensor_num = o.optString("sensor_num")
                            )
                        )
                    }
                }
                val sortedItems = items.sortedBy { it.timestamp }
                Log.d("ServerConnection", "Datos de planta obtenidos exitosamente: ${sortedItems.size} registros")
                sortedItems
            } else {
                Log.e("ServerConnection", "Error HTTP: $code al obtener datos de planta")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ServerConnection", "Error obteniendo datos de planta: ${e.message}")
            emptyList()
        }
    }
}

// Función para probar conexión con configuración inteligente
suspend fun testServerConnection(serverInput: String): String {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("ServerConnection", "=== PROBANDO CONEXIÓN ===")
            Log.d("ServerConnection", "IP y Puerto: $serverInput")
            
            val testUrl = java.net.URL("http://$serverInput/plant")
            val conn = (testUrl.openConnection() as java.net.HttpURLConnection)
            
            // Configuración estándar de red para pruebas
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000   // 5 segundos para pruebas
            conn.readTimeout = 5000      // 5 segundos para pruebas
            conn.doInput = true
            conn.setRequestProperty("User-Agent", "CultivApp/1.0")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-cache")
            
            Log.d("ServerConnection", "Probando conexión con configuración estándar")
            
            val startTime = System.currentTimeMillis()
            val code = conn.responseCode
            val responseTime = System.currentTimeMillis() - startTime
            
            if (code == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                try {
                    val json = org.json.JSONArray(body)
                    Log.d("ServerConnection", "Conexión exitosa en ${responseTime}ms, ${json.length()} plantas encontradas")
                    "✅ Conexión exitosa en ${responseTime}ms! ${json.length()} plantas encontradas"
                } catch (e: Exception) {
                    Log.w("ServerConnection", "Conexión exitosa pero respuesta no es JSON válido")
                    "⚠️ Conexión exitosa pero respuesta no es JSON válido"
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText()
                Log.w("ServerConnection", "Error HTTP $code en ${responseTime}ms: ${err?.take(50)}")
                "⚠️ Error HTTP $code en ${responseTime}ms: ${err?.take(50) ?: "Sin detalles"}..."
            }
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is java.net.ConnectException -> "No se pudo conectar al servidor"
                is java.net.SocketTimeoutException -> "Timeout de conexión"
                is java.net.UnknownHostException -> "Host desconocido"
                is java.net.NoRouteToHostException -> "No hay ruta al host"
                else -> "Error: ${e.message}"
            }
            Log.e("ServerConnection", "Error en prueba de conexión: $errorMessage")
            "❌ $errorMessage"
        }
    }
}
