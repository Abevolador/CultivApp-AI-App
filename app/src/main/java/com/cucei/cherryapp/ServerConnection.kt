package com.cucei.cherryapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.rememberCoroutineScope
import android.util.Log
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas


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

data class ConnectionResult(
    val success: Boolean,
    val plantsCount: Int,
    val message: String,
    val plants: List<ServerPlant> = emptyList(),
    val connectionError: Boolean = false
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
    canConnect: Boolean,
    huertosGuardados: List<HuertoGuardado> = emptyList(),
    onHuertoClick: (String) -> Unit = {},
    onEditarHuerto: (HuertoGuardado) -> Unit = {},
    onGuardarHuerto: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var showIpDialog by remember { mutableStateOf(false) }
    var showSaveHuertoDialog by remember { mutableStateOf(false) }
    var showSaveHuertoNoPlantsDialog by remember { mutableStateOf(false) }
    var showConnectionErrorDialog by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<ConnectionResult?>(null) }
    var huertoNombre by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Conectar!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Lista de huertos guardados
        if (huertosGuardados.isNotEmpty()) {
            Text(
                "🌱 Huertos Guardados", 
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                huertosGuardados.forEach { huerto ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌱", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onHuertoClick("${huerto.ip}:${huerto.puerto}") }
                            ) {
                                Text(
                                    huerto.nombre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${huerto.ip}:${huerto.puerto}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    "Última conexión: ${formatearFecha(huerto.ultimaConexion)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(
                                onClick = { onEditarHuerto(huerto) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Editar huerto",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Combobox de conexiones recientes
        if (recentServers.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Header del combobox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📋 Conexiones Recientes (${recentServers.size})",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Contraer" else "Expandir",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Lista expandible
                    if (expanded) {
                        Divider()
                        Column {
                            recentServers.forEach { addr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            onRecentServerClick(addr)
                                            expanded = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        addr,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { 
                                            onRecentServerClick("DELETE:$addr")
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (addr != recentServers.last()) {
                                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                            
                            // Botón limpiar todas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        onRecentServerClick("")
                                        expanded = false
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🗑️ Limpiar Todas",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }


        // Botón para abrir dialog de IP (al final de la pantalla)
        Button(
            onClick = { showIpDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                .height(52.dp)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Escribe IP y Puerto")
        }
    }

    // Dialog flotante para escribir IP y Puerto
    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isConnecting) {
                    showIpDialog = false
                }
            },
            title = {
                Text(
                    "🌐 Configurar Servidor",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Ingresa la dirección IP y puerto de tu servidor:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = serverInput,
                        onValueChange = { onServerInputChange(it.trim()) },
                        label = { Text("IP:PUERTO (ej. 192.168.100.26:2000)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isConnecting
                    )
                    if (isConnecting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                    Text(
                                "Verificando conexión...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverInput.isNotEmpty() && !isConnecting) {
                            // Verificar si el huerto ya existe
                            val ip = serverInput.split(":").getOrNull(0) ?: ""
                            val puerto = serverInput.split(":").getOrNull(1) ?: ""
                            val huertoExiste = huertosGuardados.any { 
                                it.ip == ip && it.puerto == puerto.toIntOrNull() 
                            }
                            
                            if (!huertoExiste) {
                                // Iniciar verificación de conexión
                                isConnecting = true
                                scope.launch {
                                    val result = handleConnectionAndCheckPlants(serverInput)
                                    connectionResult = result
                                    isConnecting = false
                                    
                                    if (result.success && result.plantsCount > 0) {
                                        // Caso 1: Huerto detectado con plantas
                                        showSaveHuertoDialog = true
                                        showIpDialog = false
                                    } else if (result.connectionError) {
                                        // Caso 2: Error de conexión
                                        showConnectionErrorDialog = true
                                        showIpDialog = false
                                    } else {
                                        // Caso 3: Conexión exitosa pero sin plantas
                                        showSaveHuertoNoPlantsDialog = true
                                        showIpDialog = false
                                    }
                                }
                            } else {
                                // Huerto ya existe, conectar directamente
                                onConnectClick()
                                showIpDialog = false
                            }
                        }
                    },
                    enabled = serverInput.isNotEmpty() && !isConnecting
                ) {
                    Text(if (isConnecting) "Conectando..." else "Conectar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        if (!isConnecting) {
                            showIpDialog = false
                        }
                    },
                    enabled = !isConnecting
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog para guardar huerto cuando se detectaron plantas
    if (showSaveHuertoDialog) {
        AlertDialog(
            onDismissRequest = { showSaveHuertoDialog = false },
            title = {
                    Text(
                    "🌱 ¡Huerto Detectado!",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        connectionResult?.message ?: "",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        "¿Quieres guardar este huerto para acceso rápido?",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = huertoNombre,
                        onValueChange = { huertoNombre = it },
                        label = { Text("Nombre del huerto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Mi Huerto Principal") }
                    )
                }
            },
            confirmButton = {
                    Button(
                    onClick = {
                        val ip = serverInput.split(":").getOrNull(0) ?: ""
                        val puerto = serverInput.split(":").getOrNull(1) ?: ""
                        val nombre = if (huertoNombre.isNotEmpty()) huertoNombre else "Mi Huerto"
                        onGuardarHuerto(nombre, ip, puerto)
                        onConnectClick()
                        showSaveHuertoDialog = false
                        huertoNombre = ""
                    },
                    enabled = huertoNombre.isNotEmpty()
                ) {
                    Text("Guardar y Conectar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onConnectClick()
                        showSaveHuertoDialog = false
                        huertoNombre = ""
                    }
                ) {
                    Text("Solo Conectar")
                }
            }
        )
    }

    // Dialog para guardar huerto cuando NO se detectaron plantas
    if (showSaveHuertoNoPlantsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveHuertoNoPlantsDialog = false },
            title = {
                Text(
                    "⚠️ Sin Plantas Detectadas",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        connectionResult?.message ?: "",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        "¿Quieres guardar este huerto de todas formas? Podrás agregar plantas más tarde.",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = huertoNombre,
                        onValueChange = { huertoNombre = it },
                        label = { Text("Nombre del huerto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Mi Huerto Principal") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ip = serverInput.split(":").getOrNull(0) ?: ""
                        val puerto = serverInput.split(":").getOrNull(1) ?: ""
                        val nombre = if (huertoNombre.isNotEmpty()) huertoNombre else "Mi Huerto"
                        onGuardarHuerto(nombre, ip, puerto)
                        onConnectClick()
                        showSaveHuertoNoPlantsDialog = false
                        huertoNombre = ""
                    },
                    enabled = huertoNombre.isNotEmpty()
                ) {
                    Text("Guardar y Conectar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onConnectClick()
                        showSaveHuertoNoPlantsDialog = false
                        huertoNombre = ""
                    }
                ) {
                    Text("Solo Conectar")
                }
            }
        )
    }

    // Dialog para errores de conexión
    if (showConnectionErrorDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionErrorDialog = false },
            title = {
                            Text(
                    "❌ Error de Conexión",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        connectionResult?.message ?: "",
                                fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        "No se pudo establecer conexión con el servidor. Verifica que:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "• La IP y puerto sean correctos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        "• El servidor esté funcionando",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        "• Tu dispositivo esté en la misma red",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showConnectionErrorDialog = false }
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}

// Función para manejar la conexión y verificar plantas
suspend fun handleConnectionAndCheckPlants(serverInput: String): ConnectionResult {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("ServerConnection", "=== VERIFICANDO CONEXIÓN Y PLANTAS ===")
            Log.d("ServerConnection", "IP y Puerto: $serverInput")
            
            val plants = getServerPlants(serverInput)
            
            if (plants.isNotEmpty()) {
                ConnectionResult(
                    success = true,
                    plantsCount = plants.size,
                    message = "✅ ¡Conexión exitosa! Se detectaron ${plants.size} plantas en el huerto.",
                    plants = plants,
                    connectionError = false
                )
            } else {
                ConnectionResult(
                    success = false,
                    plantsCount = 0,
                    message = "⚠️ Conexión exitosa pero no se detectaron plantas en el servidor.",
                    connectionError = false
                )
            }
        } catch (e: Exception) {
            Log.e("ServerConnection", "Error en conexión: ${e.message}")
            ConnectionResult(
                success = false,
                plantsCount = 0,
                message = "❌ Error de conexión: ${e.message ?: "Error desconocido"}",
                connectionError = true
            )
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

// Pantalla de filtros y gráfica (Propuesta A)
@Composable
fun PlantDataFilterScreen(
    serverInput: String,
    selectedPlantId: String,
    serverPlantData: List<ServerPlantData>,
    isConnecting: Boolean,
    onViewRecords: (List<ServerPlantData>) -> Unit,
) {
    val context = LocalContext.current
    var rangeExpanded by remember { mutableStateOf(false) }
    val rangeOptions = listOf("Hoy", "Semana", "Mes", "Personalizado")
    var selectedRange by remember { mutableStateOf(
        context.getSharedPreferences("CultivAppPrefs", Context.MODE_PRIVATE)
            .getString("plant_range", "Hoy") ?: "Hoy"
    ) }

    var metricExpanded by remember { mutableStateOf(false) }
    val metricOptions = listOf("Humedad", "Luz", "Temperatura", "Suelo")
    fun metricEmoji(name: String): String = when (name) {
        "Temperatura" -> "🌡️"
        "Humedad" -> "💧"
        "Luz" -> "☀️"
        "Suelo" -> "🌱"
        else -> "📈"
    }
    var selectedMetric by remember { mutableStateOf(
        context.getSharedPreferences("CultivAppPrefs", Context.MODE_PRIVATE)
            .getString("plant_metric", "Humedad") ?: "Humedad"
    ) }

    // Fechas personalizadas (texto simple para mantener compatibilidad sin dependencias de DatePicker)
    var isCustom by remember { mutableStateOf(selectedRange == "Personalizado") }
    var customSingle by remember { mutableStateOf(true) }
    var customDateStart by remember { mutableStateOf("") } // formato: yyyy-MM-dd
    var customDateEnd by remember { mutableStateOf("") }

    // DatePickerDialogs (nativos) para selección visual
    fun formatYmd(year: Int, monthZeroBased: Int, day: Int): String {
        val m = (monthZeroBased + 1).toString().padStart(2, '0')
        val d = day.toString().padStart(2, '0')
        return "$year-$m-$d"
    }

    val todayCal = java.util.Calendar.getInstance()
    val startPicker = remember {
        android.app.DatePickerDialog(
            context,
            { _, y, m, d -> customDateStart = formatYmd(y, m, d) },
            todayCal.get(java.util.Calendar.YEAR),
            todayCal.get(java.util.Calendar.MONTH),
            todayCal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
    val endPicker = remember {
        android.app.DatePickerDialog(
            context,
            { _, y, m, d -> customDateEnd = formatYmd(y, m, d) },
            todayCal.get(java.util.Calendar.YEAR),
            todayCal.get(java.util.Calendar.MONTH),
            todayCal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    // Filtrado por rango usando timestamps Unix (segundos o milisegundos)
    fun normalizeMillis(ts: Long): Long = if (ts > 1000000000000L) ts else ts * 1000

    fun periodBounds(range: String): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT-06:00"))
        val now = cal.timeInMillis
        when (range) {
            "Hoy" -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                return start to (start + 24L * 60 * 60 * 1000)
            }
            "Semana" -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = cal.timeInMillis
                return start to (start + 7L * 24 * 60 * 60 * 1000)
            }
            "Mes" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(java.util.Calendar.MONTH, 1)
                val end = cal.timeInMillis
                return start to end
            }
            else -> return 0L to Long.MAX_VALUE
        }
    }

    fun parseYmd(s: String): Long? {
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            fmt.timeZone = java.util.TimeZone.getTimeZone("GMT-06:00")
            fmt.parse(s)?.time
        } catch (_: Exception) { null }
    }

    fun filterData(): List<ServerPlantData> {
        var (start, end) = if (selectedRange == "Personalizado") {
            if (customSingle) {
                val d = parseYmd(customDateStart)
                if (d != null) d to (d + 24L * 60 * 60 * 1000) else 0L to Long.MAX_VALUE
            } else {
                val s = parseYmd(customDateStart) ?: 0L
                val e = parseYmd(customDateEnd) ?: Long.MAX_VALUE
                s to (e + 24L * 60 * 60 * 1000)
            }
        } else periodBounds(selectedRange)

        // Asegurar orden válido
        if (start > end) {
            val tmp = start
            start = end
            end = tmp
        }

        return serverPlantData.filter {
            val millis = normalizeMillis(it.timestamp)
            millis in start until end
        }
    }

    fun persistSelection() {
        val sp = context.getSharedPreferences("CultivAppPrefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("plant_range", selectedRange)
            .putString("plant_metric", selectedMetric)
            .apply()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header con botón Ver registros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Registros de plantas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { onViewRecords(filterData()) }) {
                Text("Ver registros")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Selector de rango
        Text("Filtro de rango", fontWeight = FontWeight.Medium)
        Box {
            OutlinedButton(onClick = { rangeExpanded = true }) { Text(selectedRange) }
            DropdownMenu(expanded = rangeExpanded, onDismissRequest = { rangeExpanded = false }) {
                rangeOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            selectedRange = opt
                            isCustom = opt == "Personalizado"
                            rangeExpanded = false
                            persistSelection()
                        }
                    )
                }
            }
        }

        if (isCustom) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Modo:")
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { customSingle = true }) { Text("Fecha única") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { customSingle = false }) { Text("Rango") }
            }
            Spacer(Modifier.height(8.dp))
            if (customSingle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customDateStart,
                        onValueChange = { customDateStart = it },
                        label = { Text("Fecha (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        enabled = false
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        // Sin restricciones adicionales en modo único
                        startPicker.datePicker.minDate = 0L
                        // Establecer un maxDate amplio (hoy + 10 años) para evitar problemas
                        val cap = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, 10) }.timeInMillis
                        startPicker.datePicker.maxDate = cap
                        startPicker.show()
                    }) { Text("Elegir fecha") }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customDateStart,
                            onValueChange = { customDateStart = it },
                            label = { Text("Desde (yyyy-MM-dd)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            enabled = false
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            // Si ya hay fin, el inicio no puede ser posterior al fin
                            val endMillis = parseYmd(customDateEnd)
                            if (endMillis != null) {
                                startPicker.datePicker.maxDate = endMillis
                            }
                            startPicker.datePicker.minDate = 0L
                            startPicker.show()
                        }) { Text("Elegir inicio") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customDateEnd,
                            onValueChange = { customDateEnd = it },
                            label = { Text("Hasta (yyyy-MM-dd)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            enabled = false
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            // Si ya hay inicio, el fin no puede ser anterior al inicio
                            val startMillis = parseYmd(customDateStart)
                            if (startMillis != null) {
                                endPicker.datePicker.minDate = startMillis
                            }
                            // MaxDate amplio (hoy + 10 años)
                            val cap = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, 10) }.timeInMillis
                            endPicker.datePicker.maxDate = cap
                            endPicker.show()
                        }) { Text("Elegir fin") }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Selector de métrica y botón Graficar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Métrica: ")
                Spacer(Modifier.width(8.dp))
                Box {
                    OutlinedButton(onClick = { metricExpanded = true }) { Text("${metricEmoji(selectedMetric)}  ${selectedMetric}") }
                    DropdownMenu(expanded = metricExpanded, onDismissRequest = { metricExpanded = false }) {
                        metricOptions.forEach { m ->
                            DropdownMenuItem(text = { Text("${metricEmoji(m)}  ${m}") }, onClick = {
                                selectedMetric = m
                                metricExpanded = false
                                persistSelection()
                            })
                        }
                    }
                }
            }
            Button(onClick = { /* La gráfica se actualiza reactivo con selectedMetric y filtro */ }) { Text("Graficar") }
        }

        Spacer(Modifier.height(12.dp))

        // Área de gráfica con downsampling
        val filtered = filterData()

        fun stepForSize(n: Int): Int {
            return when {
                n < 10 -> 1
                n in 10..100 -> 10
                n in 101..1000 -> 100
                n in 1001..10000 -> 1000
                else -> 10000
            }
        }

        fun downsample(items: List<ServerPlantData>): List<ServerPlantData> {
            if (items.isEmpty()) return items
            val step = stepForSize(items.size)
            val result = mutableListOf<ServerPlantData>()
            var i = step
            while (i < items.size) {
                result.add(items[i - 1])
                i += step
            }
            // Asegurar incluir el último
            if (result.isEmpty() || result.last() != items.last()) {
                result.add(items.last())
            }
            return result
        }

        val sampled = downsample(filtered)
        val muestra = when (selectedMetric) {
            "Humedad" -> sampled.map { it.relative_humidity }
            "Luz" -> sampled.map { it.lux }
            "Temperatura" -> sampled.map { it.temperature }
            "Suelo" -> sampled.map { it.moisture_value }
            else -> sampled.map { it.relative_humidity }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                val stepInfo = stepForSize(filtered.size)
                Text("${metricEmoji(selectedMetric)}  $selectedMetric • Registros: ${filtered.size} • Muestra: cada $stepInfo (puntos: ${muestra.size})", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                // Gráfica simple en Canvas
                val values = muestra.mapNotNull { it.replace(",", ".").toFloatOrNull() }
                val vMin = values.minOrNull() ?: 0f
                val vMax = values.maxOrNull() ?: 1f
                val safeRange = if (vMax - vMin < 1e-6f) 1f else (vMax - vMin)

                val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                val lineColor = MaterialTheme.colorScheme.primary
                val pointColor = MaterialTheme.colorScheme.secondary
                val textColorOutside = MaterialTheme.colorScheme.onSurface

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (values.size < 2) return@Canvas

                    // Padding interno
                    val leftPad = 48f
                    val rightPad = 16f
                    val topPad = 16f
                    val bottomPad = 32f

                    val w = size.width - leftPad - rightPad
                    val h = size.height - topPad - bottomPad

                    // Ejes
                    // X axis
                    drawLine(
                        color = axisColor,
                        start = Offset(leftPad, size.height - bottomPad),
                        end = Offset(size.width - rightPad, size.height - bottomPad),
                        strokeWidth = 2f
                    )
                    // Y axis
                    drawLine(
                        color = axisColor,
                        start = Offset(leftPad, topPad),
                        end = Offset(leftPad, size.height - bottomPad),
                        strokeWidth = 2f
                    )

                    val stepX = if (values.size == 1) w else w / (values.size - 1)

                    // Línea de la serie
                    var prev: Offset? = null
                    values.forEachIndexed { i, v ->
                        val x = leftPad + i * stepX
                        val y = topPad + (h * (1f - (v - vMin) / safeRange))
                        val p = Offset(x, y)
                        if (prev != null) {
                            drawLine(
                                color = lineColor,
                                start = prev!!,
                                end = p,
                                strokeWidth = 4f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                        prev = p
                    }

                    // Puntos + etiquetas con tamaño constante
                    val textColor = textColorOutside
                    val labelPaint = android.graphics.Paint().apply {
                        color = textColor.toArgb()
                        textSize = 36f
                        isAntiAlias = true
                    }
                    values.forEachIndexed { i, v ->
                        val x = leftPad + i * stepX
                        val y = topPad + (h * (1f - (v - vMin) / safeRange))
                        drawCircle(color = pointColor, radius = 4f, center = Offset(x, y))
                        val label = String.format(java.util.Locale.US, "%.1f", v)
                        val tx = (x + 6f).coerceAtMost(size.width - 24f)
                        val ty = (y - 8f).coerceAtLeast(24f)
                        drawIntoCanvas { cnv ->
                            cnv.nativeCanvas.drawText(label, tx, ty, labelPaint)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Lista de puntos muestreados
                fun unitFor(metric: String): String = when (metric) {
                    "Temperatura" -> "°C"
                    "Humedad" -> "%"
                    "Suelo" -> "%"
                    "Luz" -> " lux"
                    else -> ""
                }
                val unit = unitFor(selectedMetric)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sampled.forEachIndexed { idx, item ->
                        val vStr = muestra.getOrNull(idx)?.replace(",", ".")?.toFloatOrNull()
                        val millis = if (item.timestamp > 1000000000000L) item.timestamp else item.timestamp * 1000
                        val dateStr = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("es","MX")).apply {
                            timeZone = java.util.TimeZone.getTimeZone("GMT-06:00")
                        }.format(java.util.Date(millis))
                        if (vStr != null) {
                            Text("• ${dateStr} — ${String.format(java.util.Locale.US, "%.1f", vStr)}${unit}")
                        }
                    }
                }
            }
        }
    }
}

// Pantalla de lista filtrada
@Composable
fun ListaRegistrosFiltradosScreen(
    title: String,
    items: List<ServerPlantData>,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("←") }
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin registros en el periodo seleccionado")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                items(items.size) { idx ->
                    val r = items[idx]
                    val millis = if (r.timestamp > 1000000000000L) r.timestamp else r.timestamp * 1000
                    val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("es","MX")).apply {
                        timeZone = java.util.TimeZone.getTimeZone("GMT-06:00")
                    }.format(java.util.Date(millis))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(date, fontWeight = FontWeight.Medium)
                            Text("Temp: ${r.temperature}°C | HR: ${r.relative_humidity}% | Lux: ${r.lux} | Suelo: ${r.moisture_value}%")
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
            
            // Detectar si estamos en emulador y ajustar la IP
            val adjustedServerInput = if (isRunningOnEmulator()) {
                when {
                    serverInput.startsWith("10.214.49.194") -> serverInput.replace("10.214.49.194", "10.0.2.2")
                    serverInput.startsWith("192.168.") -> serverInput.replace("192.168.", "10.0.2.2")
                    else -> serverInput
                }
            } else {
                serverInput
            }
            
            Log.d("ServerConnection", "IP ajustada para emulador: $adjustedServerInput")
            Log.d("ServerConnection", "URL: http://${adjustedServerInput}/plant")
            
            val url = java.net.URL("http://${adjustedServerInput}/plant")
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
                // Error HTTP - lanzar excepción para que se detecte como error de conexión
                Log.e("ServerConnection", "Error HTTP: $code al obtener plantas")
                throw Exception("Error HTTP $code: No se pudo conectar al servidor")
            }
        } catch (e: Exception) {
            // Error de conexión - lanzar excepción hacia arriba
            Log.e("ServerConnection", "Error obteniendo plantas: ${e.message}")
            throw e
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
            
            // Configurar propiedades del sistema para permitir conexiones HTTP
            System.setProperty("http.agent", "CultivApp/1.0")
            System.setProperty("java.net.preferIPv4Stack", "true")
            
            // Detectar si estamos en emulador y ajustar la IP
            val adjustedServerInput = if (isRunningOnEmulator()) {
                when {
                    serverInput.startsWith("10.214.49.194") -> serverInput.replace("10.214.49.194", "10.0.2.2")
                    serverInput.startsWith("192.168.") -> serverInput.replace("192.168.", "10.0.2.2")
                    else -> serverInput
                }
            } else {
                serverInput
            }
            
            Log.d("ServerConnection", "IP ajustada para emulador: $adjustedServerInput")
            
            val testUrl = java.net.URL("http://$adjustedServerInput/plant")
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
                else -> when {
                    e.message?.contains("EPERM") == true -> "Error de permisos de red (EPERM). Verifica la IP y permisos."
                    e.message?.contains("ECONNREFUSED") == true -> "Conexión rechazada. Verifica que el servidor esté corriendo."
                    e.message?.contains("ENETUNREACH") == true -> "Red inalcanzable. Verifica la IP y conectividad."
                    e.message?.contains("ETIMEDOUT") == true -> "Tiempo de espera agotado. Verifica la IP y puerto."
                    else -> "Error: ${e.message}"
                }
            }
            Log.e("ServerConnection", "Error en prueba de conexión: $errorMessage")
            "❌ $errorMessage"
        }
    }
}

// Función para detectar si estamos en emulador
fun isRunningOnEmulator(): Boolean {
    return try {
        android.os.Build.FINGERPRINT.startsWith("generic") ||
        android.os.Build.FINGERPRINT.startsWith("unknown") ||
        android.os.Build.MODEL.contains("google_sdk") ||
        android.os.Build.MODEL.contains("Emulator") ||
        android.os.Build.MODEL.contains("Android SDK built for x86") ||
        android.os.Build.MANUFACTURER.contains("Genymotion") ||
        (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")) ||
        "google_sdk" == android.os.Build.PRODUCT
    } catch (e: Exception) {
        false
    }
}

// Función para formatear fechas
fun formatearFecha(timestamp: Long): String {
    val fecha = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("es", "MX"))
    return formatter.format(fecha)
}
