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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
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

// Data class para almacenar último registro con timestamp de caché
data class PlantLastRecord(
    val plantId: String,
    val lastData: ServerPlantData?,
    val cacheTimestamp: Long = System.currentTimeMillis()
)

// Data class para avisos de plantas
data class PlantAlert(
    val type: AlertType,
    val message: String,
    val severity: AlertSeverity
)

enum class AlertType {
    MOISTURE, TEMPERATURE, LIGHT
}

enum class AlertSeverity {
    LOW, HIGH, OK
}

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
    onPlantClick: (String) -> Unit,
    onEditPlant: (ServerPlant) -> Unit,
    plantLastRecords: Map<String, PlantLastRecord> = emptyMap()
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header con título y badge de contador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🌱 Plantas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (!isConnecting && serverPlants.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${serverPlants.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Contenido según estado
        if (isConnecting) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                CircularProgressIndicator()
                    Text(
                        "Cargando plantas...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else if (serverPlants.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "🌱",
                            fontSize = 48.sp
                        )
                        Text(
                            "Sin plantas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "No se encontraron plantas en el servidor o hubo un error de conexión",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(serverPlants.size) { idx ->
                    val p = serverPlants[idx]
                    
                    // Obtener último registro y avisos para esta planta
                    val lastRecord = plantLastRecords[p.plant_id]
                    val alerts = lastRecord?.let { getPlantAlerts(it) } ?: emptyList()
                    
                    // Formatear fecha de registro
                    val fechaRegistro = try {
                        val millis = if (p.plant_registered > 1000000000000L) 
                            p.plant_registered 
                        else 
                            p.plant_registered * 1000
                        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es","MX")).apply {
                            timeZone = java.util.TimeZone.getTimeZone("GMT-06:00")
                        }
                        dateFormat.format(java.util.Date(millis))
                    } catch (e: Exception) {
                        "Fecha no disponible"
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlantClick(p.plant_id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Nombre/ID destacado como título principal con botón de editar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    p.plant_name.ifBlank { p.plant_id },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        onEditPlant(p)
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar planta",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )
                            
                            // Tipo de planta como chip
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "🍅 ${p.plant_type}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            // Avisos de la planta (chips pequeños que se ajustan)
                            if (alerts.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Dividir avisos en filas para mejor organización
                                    alerts.chunked(2).forEach { alertRow ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            alertRow.forEach { alert ->
                                                val alertColor = when (alert.severity) {
                                                    AlertSeverity.LOW -> MaterialTheme.colorScheme.errorContainer
                                                    AlertSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                                                    AlertSeverity.OK -> MaterialTheme.colorScheme.primaryContainer
                                                }
                                                val alertTextColor = when (alert.severity) {
                                                    AlertSeverity.LOW -> MaterialTheme.colorScheme.onErrorContainer
                                                    AlertSeverity.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                                    AlertSeverity.OK -> MaterialTheme.colorScheme.onPrimaryContainer
                                                }
                                                
                                                Surface(
                                                    color = alertColor,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Text(
                                                        alert.message,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = alertTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            // Rellenar espacio si hay un número impar de avisos
                                            if (alertRow.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Info secundaria (sensor y fecha)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🔌 Sensor: ${p.soil_sens_num}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    "📅 $fechaRegistro",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Pantalla de edición de planta
@Composable
fun EditarPlantaScreen(
    serverInput: String,
    plant: ServerPlant,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Estados para los campos editables
    var plantName by remember { mutableStateOf(plant.plant_name) }
    var plantType by remember { mutableStateOf(plant.plant_type) }
    var plantDate by remember { mutableStateOf(plant.plant_date) }
    var plantUpdatePoll by remember { mutableStateOf(plant.plant_update_poll) }
    var updatePollActivated by remember { mutableStateOf(plant.update_poll_activated) }
    var soilSensNum by remember { mutableStateOf(plant.soil_sens_num.toString()) }
    
    // Estado para el guardado
    var guardando by remember { mutableStateOf(false) }
    
    // Opciones de intervalo en minutos (convertir a segundos)
    val intervalos = listOf(
        300 to "5 minutos",
        600 to "10 minutos",
        900 to "15 minutos",
        1800 to "30 minutos",
        3600 to "60 minutos"
    )
    
    // Formatear fecha
    val fechaFormateada = try {
        val millis = if (plantDate > 1000000000000L) plantDate else plantDate * 1000
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "MX")).apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT-06:00")
        }
        dateFormat.format(java.util.Date(millis))
    } catch (e: Exception) {
        "Fecha no disponible"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header con título y botón de regreso
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Editando la planta ${plant.plant_name.ifBlank { plant.plant_id }}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balancear el espacio del IconButton
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Contenido scrolleable
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campos bloqueados (solo lectura)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Campos de solo lectura",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        
                        // Plant ID (bloqueado)
                        OutlinedTextField(
                            value = plant.plant_id,
                            onValueChange = { },
                            label = { Text("ID de la planta") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true
                        )
                        
                        // Device MAC (bloqueado)
                        OutlinedTextField(
                            value = plant.device_mac,
                            onValueChange = { },
                            label = { Text("MAC del dispositivo") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true
                        )
                    }
                }
            }
            
            // Campos editables
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Campos editables",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Nombre de la planta
                        OutlinedTextField(
                            value = plantName,
                            onValueChange = { if (it.length <= 50) plantName = it },
                            label = { Text("Nombre de la planta") },
                            placeholder = { Text(plant.plant_id) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text(
                            "${plantName.length}/50",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (plantName.length > 50) Color.Red else Color.Gray
                        )
                        
                        // Tipo de planta
                        OutlinedTextField(
                            value = plantType,
                            onValueChange = { plantType = it },
                            label = { Text("Tipo de planta") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        // Fecha de plantación (mostrar como texto formateado)
                        OutlinedTextField(
                            value = fechaFormateada,
                            onValueChange = { },
                            label = { Text("Fecha de plantación") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                Text(
                                    "(No editable)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        )
                        
                        HorizontalDivider()
                        
                        // Intervalo de actualización
                        Text(
                            "⏱️ Intervalo de actualización de datos:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            intervalos.forEach { (segundos, etiqueta) ->
                                FilterChip(
                                    selected = plantUpdatePoll == segundos,
                                    onClick = {
                                        plantUpdatePoll = segundos
                                    },
                                    label = { Text(etiqueta) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Activar/desactivar polling
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Activar actualización automática:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = updatePollActivated,
                                onCheckedChange = { updatePollActivated = it }
                            )
                        }
                        
                        HorizontalDivider()
                        
                        // Número de sensor
                        OutlinedTextField(
                            value = soilSensNum,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                    soilSensNum = it
                                }
                            },
                            label = { Text("Número de sensor de suelo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
            
            // Botón de guardar
            item {
                Button(
                    onClick = {
                        guardando = true
                        coroutineScope.launch {
                            // Solo actualizar campos que cambiaron
                            val cambios = mutableMapOf<String, Any>()
                            
                            if (plantName != plant.plant_name) {
                                cambios["plantName"] = plantName
                            }
                            if (plantType != plant.plant_type) {
                                cambios["plantType"] = plantType
                            }
                            if (plantUpdatePoll != plant.plant_update_poll) {
                                cambios["plantUpdatePoll"] = plantUpdatePoll
                            }
                            if (updatePollActivated != plant.update_poll_activated) {
                                cambios["updatePollActivated"] = updatePollActivated
                            }
                            val soilSensNumInt = soilSensNum.toIntOrNull()
                            if (soilSensNumInt != null && soilSensNumInt != plant.soil_sens_num) {
                                cambios["soilSensNum"] = soilSensNumInt
                            }
                            
                            if (cambios.isNotEmpty()) {
                                val exito = updatePlant(
                                    serverInput = serverInput,
                                    plantId = plant.plant_id,
                                    plantName = cambios["plantName"] as? String,
                                    plantType = cambios["plantType"] as? String,
                                    plantDate = null, // No editable
                                    plantUpdatePoll = cambios["plantUpdatePoll"] as? Int,
                                    updatePollActivated = cambios["updatePollActivated"] as? Boolean,
                                    soilSensNum = cambios["soilSensNum"] as? Int
                                )
                                
                                guardando = false
                                
                                if (exito) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Planta actualizada exitosamente",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    onSave()
                                    onBack()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error al actualizar la planta",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                guardando = false
                                android.widget.Toast.makeText(
                                    context,
                                    "No hay cambios para guardar",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando...")
                    } else {
                        Text("Guardar cambios")
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
                                            "${r.moisture_value}",
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
    selectedPlantName: String?,
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
            val headerTitle = selectedPlantName?.takeIf { it.isNotBlank() } ?: selectedPlantId
            Text(
                "🌱 $headerTitle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            OutlinedButton(onClick = { onViewRecords(filterData()) }) {
                Text("Ver registros")
            }
        }

        Spacer(Modifier.height(24.dp))

        // Card de Filtros
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título de la sección
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "📅 Filtros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Selector de rango
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Filtro de rango",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Box {
                        OutlinedButton(
                            onClick = { rangeExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedRange)
                        }
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
                }

                // Calendario personalizado
                if (isCustom) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Modo:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = { customSingle = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (customSingle) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        Color.Transparent
                                )
                            ) {
                                Text("Fecha única")
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { customSingle = false },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!customSingle) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        Color.Transparent
                                )
                            ) {
                                Text("Rango")
                            }
                        }
                        
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
                                    startPicker.datePicker.minDate = 0L
                                    val cap = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, 10) }.timeInMillis
                                    startPicker.datePicker.maxDate = cap
                                    startPicker.show()
                                }) {
                                    Text("Elegir")
                                }
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
                                        val endMillis = parseYmd(customDateEnd)
                                        if (endMillis != null) {
                                            startPicker.datePicker.maxDate = endMillis
                                        }
                                        startPicker.datePicker.minDate = 0L
                                        startPicker.show()
                                    }) {
                                        Text("Elegir")
                                    }
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
                                        val startMillis = parseYmd(customDateStart)
                                        if (startMillis != null) {
                                            endPicker.datePicker.minDate = startMillis
                                        }
                                        val cap = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, 10) }.timeInMillis
                                        endPicker.datePicker.maxDate = cap
                                        endPicker.show()
                                    }) {
                                        Text("Elegir")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Calcular datos filtrados y muestreados primero
        val filtered = filterData()
        
        // CONSTANTE: Número máximo de puntos a mostrar en la gráfica
        // Puedes modificar este valor si quieres cambiar la cantidad de puntos
        val MAX_POINTS = 20

        fun downsample(items: List<ServerPlantData>): List<ServerPlantData> {
            if (items.isEmpty()) return items
            
            // Si hay menos puntos que el máximo, devolver todos
            if (items.size <= MAX_POINTS) {
                return items
            }
            
            // Calcular índices distribuidos uniformemente para obtener exactamente MAX_POINTS
            val result = mutableListOf<ServerPlantData>()
            val step = (items.size - 1).toFloat() / (MAX_POINTS - 1) // Paso entre índices
            
            // Siempre incluir el primer elemento
            result.add(items[0])
            
            // Calcular los índices intermedios distribuidos uniformemente
            for (i in 1 until MAX_POINTS - 1) {
                val index = (i * step).toInt() // Redondear hacia abajo (truncar)
                result.add(items[index])
            }
            
            // Siempre incluir el último elemento
            result.add(items[items.size - 1])
            
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

        // Card de Visualización
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título de la sección
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "📈 Visualización",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Selector de métrica
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Métrica",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Box {
                        OutlinedButton(
                            onClick = { metricExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${metricEmoji(selectedMetric)}  ${selectedMetric}")
                        }
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
                
                // Información de registros (badges)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "📊 ${filtered.size} registros",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "• ${muestra.size} puntos",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        // Estado para mostrar/ocultar lista de detalles
        var showPointsList by remember { mutableStateOf(false) }
        
        // Estado para el índice del punto seleccionado (para resaltar en gráfica)
        var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
        
        // Estado para items expandidos en la lista
        var expandedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
        
        // Card de Gráfica
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                // Header de la gráfica
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${metricEmoji(selectedMetric)} $selectedMetric",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))
                
                // Funciones auxiliares para calcular rangos Y personalizados
                fun getYRangeForMetric(metric: String, values: List<Float>): Pair<Float, Float> {
                    val vMin = values.minOrNull() ?: 0f
                    val vMax = values.maxOrNull() ?: 1f
                    
                    return when (metric) {
                        "Luz" -> {
                            // Lux: de 0, incrementos de 1000
                            val maxRounded = ((vMax / 1000f).toInt() + 1) * 1000f
                            Pair(0f, maxRounded)
                        }
                        "Temperatura" -> {
                            // Temperatura: de 0, incrementos de 5
                            val maxRounded = ((vMax / 5f).toInt() + 1) * 5f
                            Pair(0f, maxRounded)
                        }
                        "Suelo" -> {
                            // Humedad de suelo: de 0, incrementos de 100
                            val maxRounded = ((vMax / 100f).toInt() + 1) * 100f
                            Pair(0f, maxRounded)
                        }
                        "Humedad" -> {
                            // Humedad normal: de 0, incrementos de 5
                            val maxRounded = ((vMax / 5f).toInt() + 1) * 5f
                            Pair(0f, maxRounded)
                        }
                        else -> {
                            val safeRange = if (vMax - vMin < 1e-6f) 1f else (vMax - vMin)
                            Pair(vMin, vMax)
                        }
                    }
                }
                
                fun getYIncrementForMetric(metric: String): Float {
                    return when (metric) {
                        "Luz" -> 1000f
                        "Temperatura" -> 5f
                        "Suelo" -> 100f
                        "Humedad" -> 5f
                        else -> 1f
                    }
                }
                
                // Preparar datos
                val values = muestra.mapNotNull { it.replace(",", ".").toFloatOrNull() }
                val (yMin, yMax) = getYRangeForMetric(selectedMetric, values)
                val yRange = yMax - yMin
                val yIncrement = getYIncrementForMetric(selectedMetric)
                
                // Preparar timestamps para eje X
                val timestamps = sampled.map { item ->
                    val millis = if (item.timestamp > 1000000000000L) item.timestamp else item.timestamp * 1000
                    millis
                }
                val timeMin = timestamps.minOrNull() ?: 0L
                val timeMax = timestamps.maxOrNull() ?: 1L
                val timeRange = (timeMax - timeMin).toFloat()

                val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                val lineColor = MaterialTheme.colorScheme.primary
                val pointColor = MaterialTheme.colorScheme.secondary
                val textColorOutside = MaterialTheme.colorScheme.onSurface
                val highlightColor = MaterialTheme.colorScheme.error
                val highlightRadius = 8f

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    if (values.size < 2 || timestamps.size != values.size) return@Canvas

                    // Padding interno (más espacio para etiquetas, especialmente abajo para etiquetas rotadas)
                    val leftPad = 60f
                    val rightPad = 16f
                    val topPad = 24f
                    val bottomPad = 64f // Aumentado para acomodar etiquetas rotadas

                    val w = size.width - leftPad - rightPad
                    val h = size.height - topPad - bottomPad

                    // Paint para etiquetas
                    val axisLabelPaint = android.graphics.Paint().apply {
                        color = textColorOutside.toArgb()
                        textSize = 28f
                        isAntiAlias = true
                    }
                    
                    val valueLabelPaint = android.graphics.Paint().apply {
                        color = textColorOutside.toArgb()
                        textSize = 32f
                        isAntiAlias = true
                    }

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
                    
                    // Líneas de cuadrícula y etiquetas del eje Y
                    val ySteps = ((yMax - yMin) / yIncrement).toInt() + 1
                    for (i in 0 until ySteps) {
                        val yValue = yMin + (i * yIncrement)
                        val yPos = topPad + (h * (1f - (yValue - yMin) / yRange))
                        
                        // Línea de cuadrícula
                        if (i > 0 && i < ySteps) {
                            drawLine(
                                color = gridColor,
                                start = Offset(leftPad, yPos),
                                end = Offset(size.width - rightPad, yPos),
                                strokeWidth = 1f
                            )
                        }
                        
                        // Etiqueta del eje Y
                        val yLabel = when (selectedMetric) {
                            "Luz" -> "${yValue.toInt()}"
                            "Temperatura" -> "${yValue.toInt()}°"
                            "Suelo" -> "${yValue.toInt()}"
                            "Humedad" -> "${yValue.toInt()}%"
                            else -> String.format(java.util.Locale.US, "%.1f", yValue)
                        }
                        drawIntoCanvas { cnv ->
                            val textWidth = axisLabelPaint.measureText(yLabel)
                            cnv.nativeCanvas.drawText(
                                yLabel,
                                leftPad - textWidth - 8f,
                                yPos + (axisLabelPaint.textSize / 3f),
                                axisLabelPaint
                            )
                        }
                    }

                    // Calcular posiciones X basadas en tiempo real
                    val xPositions = timestamps.mapIndexed { i, timestamp ->
                        val timeOffset = (timestamp - timeMin).toFloat()
                        val xRatio = if (timeRange > 0) timeOffset / timeRange else (i.toFloat() / (values.size - 1).coerceAtLeast(1))
                        leftPad + (xRatio * w)
                    }

                    // Línea de la serie
                    var prev: Offset? = null
                    values.forEachIndexed { i, v ->
                        val x = xPositions[i]
                        val y = topPad + (h * (1f - (v - yMin) / yRange))
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

                    // Etiquetas del eje X (tiempo) - distribución uniforme para evitar solapamiento
                    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale("es", "MX")).apply {
                        timeZone = java.util.TimeZone.getTimeZone("GMT-06:00")
                    }
                    
                    // Calcular número de etiquetas (5-7) distribuidas uniformemente
                    val numLabels = when {
                        values.size <= 3 -> values.size
                        values.size <= 10 -> 5
                        else -> 7
                    }
                    
                    // Generar posiciones X uniformemente distribuidas
                    val uniformXPositions = (0 until numLabels).map { index ->
                        val ratio = if (numLabels > 1) index.toFloat() / (numLabels - 1) else 0.5f
                        leftPad + (ratio * w)
                    }
                    
                    // Para cada posición uniforme, encontrar el timestamp más cercano
                    uniformXPositions.forEach { targetX ->
                        // Encontrar el índice del punto más cercano a esta posición X
                        var closestIndex = 0
                        var minDistance = kotlin.math.abs(xPositions[0] - targetX)
                        
                        xPositions.forEachIndexed { i, x ->
                            val distance = kotlin.math.abs(x - targetX)
                            if (distance < minDistance) {
                                minDistance = distance
                                closestIndex = i
                            }
                        }
                        
                        // Obtener el timestamp y la posición X real del punto más cercano
                        val closestTimestamp = timestamps[closestIndex]
                        val actualX = xPositions[closestIndex]
                        val date = java.util.Date(closestTimestamp)
                        val timeLabel = timeFormat.format(date)
                        
                        drawIntoCanvas { cnv ->
                            // Rotar el canvas 90 grados para las etiquetas del eje X (vertical)
                            cnv.nativeCanvas.save()
                            val textWidth = axisLabelPaint.measureText(timeLabel)
                            val textHeight = axisLabelPaint.textSize
                            
                            // Para rotación de -90 grados, necesitamos ajustar la posición
                            // El texto se dibuja de arriba hacia abajo cuando está rotado -90
                            cnv.nativeCanvas.translate(actualX, size.height - bottomPad + 30f)
                            cnv.nativeCanvas.rotate(-90f) // Rotar -90 grados (vertical, de arriba hacia abajo)
                            
                            // Dibujar el texto rotado (centrado)
                            cnv.nativeCanvas.drawText(
                                timeLabel,
                                -textWidth / 2f, // Centrar horizontalmente (ahora es vertical después de rotar)
                                textHeight / 3f, // Ajustar posición vertical
                                axisLabelPaint
                            )
                            cnv.nativeCanvas.restore()
                        }
                    }
                    
                    // Puntos + etiquetas de valores
                    values.forEachIndexed { i, v ->
                        val x = xPositions[i]
                        val y = topPad + (h * (1f - (v - yMin) / yRange))
                        val point = Offset(x, y)
                        
                        // Si este punto está seleccionado, dibujar círculo de resaltado
                        if (selectedPointIndex == i) {
                            drawCircle(
                                color = highlightColor.copy(alpha = 0.3f),
                                radius = highlightRadius + 4f,
                                center = point
                            )
                            drawCircle(
                                color = highlightColor,
                                radius = highlightRadius,
                                center = point,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                            )
                        }
                        
                        // Dibujar el punto normal
                        drawCircle(color = pointColor, radius = 4f, center = point)
                        
                        // Dibujar etiqueta de valor
                        val label = when (selectedMetric) {
                            "Luz" -> "${v.toInt()}"
                            "Temperatura" -> "${v.toInt()}°"
                            "Suelo" -> "${v.toInt()}"
                            "Humedad" -> "${v.toInt()}%"
                            else -> String.format(java.util.Locale.US, "%.1f", v)
                        }
                        val tx = (x + 6f).coerceAtMost(size.width - 24f)
                        val ty = (y - 8f).coerceAtLeast(topPad + 8f)
                        drawIntoCanvas { cnv ->
                            cnv.nativeCanvas.drawText(label, tx, ty, valueLabelPaint)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Botón para mostrar/ocultar lista de detalles
        Button(
            onClick = { showPointsList = !showPointsList },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showPointsList) 
                    MaterialTheme.colorScheme.secondary 
                else 
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (showPointsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (showPointsList) "Ocultar detalles de puntos" else "Ver detalles de puntos")
            }
        }
        
        // Lista de puntos con detalles expandibles
        if (showPointsList) {
            Spacer(Modifier.height(16.dp))
            
            fun unitFor(metric: String): String = when (metric) {
                "Temperatura" -> "°C"
                "Humedad" -> "%"
                "Suelo" -> ""
                "Luz" -> " lux"
                else -> ""
            }
            
            val unit = unitFor(selectedMetric)
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(sampled.size) { idx ->
                    val item = sampled[idx]
                    val vStr = muestra.getOrNull(idx)?.replace(",", ".")?.toFloatOrNull()
                    val millis = if (item.timestamp > 1000000000000L) item.timestamp else item.timestamp * 1000
                    val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("es","MX")).apply {
                        timeZone = java.util.TimeZone.getTimeZone("GMT-06:00")
                    }.format(java.util.Date(millis))
                    val isExpanded = expandedIndices.contains(idx)
                    val isSelected = selectedPointIndex == idx
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPointIndex = idx
                                expandedIndices = if (isExpanded) {
                                    expandedIndices - idx
                                } else {
                                    expandedIndices + idx
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        dateStr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    if (vStr != null) {
                                        Text(
                                            "${metricEmoji(selectedMetric)} ${selectedMetric}: ${String.format(java.util.Locale.US, "%.1f", vStr)}${unit}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Contraer" else "Expandir",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Contenido expandido
                            if (isExpanded) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(12.dp))
                                
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "🌡️ Temperatura: ${item.temperature.replace(",", ".")}°C",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "💧 Humedad: ${item.relative_humidity.replace(",", ".")}%",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "☀️ Luz: ${item.lux.replace(",", ".")} lux",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "🌱 Suelo: ${item.moisture_value.replace(",", ".")}",
                                        fontSize = 14.sp
                                    )
                                    if (item.sensor_num.isNotEmpty()) {
                                        Text(
                                            "🔌 Sensor: ${item.sensor_num}",
                                            fontSize = 14.sp,
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
                            Text("Temp: ${r.temperature}°C | HR: ${r.relative_humidity}% | Lux: ${r.lux} | Suelo: ${r.moisture_value}")
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

// En ServerConnection.kt

// Función para obtener datos de una planta específica
// maxRecords: Límite máximo de registros a cargar (por defecto 10,000 para evitar problemas de memoria)
suspend fun getServerPlantData(serverInput: String, plantId: String, maxRecords: Int = 10000): List<ServerPlantData> {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("ServerConnection", "=== OBTENIENDO DATOS DE PLANTA ===")
            Log.d("ServerConnection", "IP y Puerto: $serverInput, Plant ID: $plantId")

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

            // --- CAMBIO CLAVE AQUÍ ---
            // La URL ahora apunta directamente al endpoint con el ID de la planta.
            val urlString = "http://${adjustedServerInput}/plant_data/$plantId"
            Log.d("ServerConnection", "URL: $urlString")
            Log.d("ServerConnection", "Límite máximo de registros: $maxRecords")

            val url = java.net.URL(urlString) // Usamos la nueva URL
            val conn = (url.openConnection() as java.net.HttpURLConnection)
            
            // Configuración de red (sin cambios)
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doInput = true
            conn.setRequestProperty("User-Agent", "CultivApp/1.0")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-cache")
            
            Log.d("ServerConnection", "Conectando...")
            
            val code = conn.responseCode
            if (code == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val json = org.json.JSONArray(body)
                val items = mutableListOf<ServerPlantData>()
                
                Log.d("ServerConnection", "JSON recibido con ${json.length()} elementos totales")

                // --- YA NO NECESITAMOS FILTRAR ---
                // El servidor ya nos dio solo los datos de la planta correcta.
                for (i in 0 until json.length()) {
                    val o = json.getJSONObject(i)
                    // Simplemente añadimos cada objeto del array.
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

                // Ordenar por timestamp y tomar solo los últimos N registros
                val sortedItems = items.sortedBy { it.timestamp }
                val limitedItems = if (sortedItems.size > maxRecords) {
                    Log.w("ServerConnection", "⚠️ Se encontraron ${sortedItems.size} registros, limitando a los últimos $maxRecords")
                    sortedItems.takeLast(maxRecords)
                } else {
                sortedItems
                }

                Log.d("ServerConnection", "Datos de planta procesados: ${limitedItems.size} registros")
                limitedItems
            } else {
                Log.e("ServerConnection", "Error HTTP: $code al obtener datos de planta desde $urlString")
                emptyList()
            }
        } catch (e: OutOfMemoryError) {
            Log.e("ServerConnection", "❌ Error de memoria al cargar datos: ${e.message}")
            emptyList()
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("ServerConnection", "⏱️ Timeout al cargar datos: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e("ServerConnection", "Error obteniendo datos de planta: ${e.message}")
            emptyList()
        }
    }
}


// Función para actualizar una planta (UPDATE /plant)
suspend fun updatePlant(
    serverInput: String,
    plantId: String,
    plantName: String? = null,
    plantType: String? = null,
    plantDate: Long? = null,
    plantUpdatePoll: Int? = null,
    updatePollActivated: Boolean? = null,
    soilSensNum: Int? = null
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("ServerConnection", "=== ACTUALIZANDO PLANTA ===")
            Log.d("ServerConnection", "IP y Puerto: $serverInput, Plant ID: $plantId")
            
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
            
            val url = java.net.URL("http://${adjustedServerInput}/plant")
            val conn = (url.openConnection() as java.net.HttpURLConnection)
            
            // Usar PUT en lugar de UPDATE/PATCH ya que HttpURLConnection no soporta métodos personalizados
            // El backend debe aceptar PUT además de UPDATE
            conn.requestMethod = "PUT"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "CultivApp/1.0")
            conn.setRequestProperty("Accept", "application/json")
            
            // Crear el JSON body solo con los campos que se quieren actualizar
            val jsonBody = org.json.JSONObject()
            jsonBody.put("plant_id", plantId)
            
            if (plantName != null) {
                jsonBody.put("plant_name", plantName)
                Log.d("ServerConnection", "Actualizando plant_name: $plantName")
            }
            if (plantType != null) {
                jsonBody.put("plant_type", plantType)
                Log.d("ServerConnection", "Actualizando plant_type: $plantType")
            }
            if (plantDate != null) {
                jsonBody.put("plant_date", plantDate)
                Log.d("ServerConnection", "Actualizando plant_date: $plantDate")
            }
            if (plantUpdatePoll != null) {
                jsonBody.put("plant_update_poll", plantUpdatePoll)
                Log.d("ServerConnection", "Actualizando plant_update_poll: $plantUpdatePoll")
            }
            if (updatePollActivated != null) {
                jsonBody.put("update_poll_activated", updatePollActivated)
                Log.d("ServerConnection", "Actualizando update_poll_activated: $updatePollActivated")
            }
            if (soilSensNum != null) {
                jsonBody.put("soil_sens_num", soilSensNum)
                Log.d("ServerConnection", "Actualizando soil_sens_num: $soilSensNum")
            }
            
            // Enviar el body
            conn.outputStream.use { output ->
                output.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }
            
            val code = conn.responseCode
            if (code in 200..299) {
                val responseBody = try {
                    conn.inputStream.bufferedReader().readText()
                } catch (e: Exception) {
                    ""
                }
                Log.d("ServerConnection", "Planta actualizada exitosamente: $responseBody")
                true
            } else {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: "Sin detalles"
                } catch (e: Exception) {
                    "Error al leer respuesta"
                }
                Log.e("ServerConnection", "Error HTTP $code al actualizar planta: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e("ServerConnection", "Error actualizando planta: ${e.message}")
            false
        }
    }
}

// Función para actualizar el intervalo de actualización del scheduler
suspend fun updateSchedulerInterval(
    serverInput: String,
    jobId: String,
    seconds: Int
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("ServerConnection", "=== ACTUALIZANDO INTERVALO DEL SCHEDULER ===")
            Log.d("ServerConnection", "IP y Puerto: $serverInput, Job ID: $jobId, Segundos: $seconds")
            
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
            
            val url = java.net.URL("http://${adjustedServerInput}/scheduler/jobs/$jobId")
            val conn = (url.openConnection() as java.net.HttpURLConnection)
            
            conn.requestMethod = "PATCH"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "CultivApp/1.0")
            conn.setRequestProperty("Accept", "application/json")
            
            // Crear el JSON body
            val jsonBody = org.json.JSONObject()
            jsonBody.put("trigger", "interval")
            jsonBody.put("seconds", seconds)
            
            // Enviar el body
            conn.outputStream.use { output ->
                output.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }
            
            val code = conn.responseCode
            if (code in 200..299) {
                Log.d("ServerConnection", "Intervalo actualizado exitosamente: $seconds segundos")
                true
            } else {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: "Sin detalles"
                } catch (e: Exception) {
                    "Error al leer respuesta"
                }
                Log.e("ServerConnection", "Error HTTP $code al actualizar intervalo: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e("ServerConnection", "Error actualizando intervalo: ${e.message}")
            false
        }
    }
}

// Función para obtener el último registro de una planta (con caché)
// maxRecords: Límite de registros a cargar (optimizado para lista de plantas - solo necesita el último)
suspend fun getLastPlantRecord(
    serverInput: String,
    plantId: String,
    cachedRecord: PlantLastRecord?,
    cacheTimeoutMs: Long = 60000, // 1 minuto por defecto
    maxRecords: Int = 100 // Solo cargar 100 registros (suficiente para obtener el último)
): PlantLastRecord {
    val now = System.currentTimeMillis()
    
    // Si hay caché válido (menos de 1 minuto), retornarlo
    if (cachedRecord != null && 
        cachedRecord.plantId == plantId &&
        (now - cachedRecord.cacheTimestamp) < cacheTimeoutMs) {
        Log.d("PlantAlerts", "Usando datos en caché para planta $plantId")
        return cachedRecord
    }
    
    // Obtener datos frescos (solo los últimos 100 registros - suficiente para encontrar el más reciente)
    val allData = getServerPlantData(serverInput, plantId, maxRecords = maxRecords)
    val lastData = allData.maxByOrNull { it.timestamp }
    
    Log.d("PlantAlerts", "Datos frescos obtenidos para planta $plantId: ${lastData != null} (de ${allData.size} registros cargados)")
    
    return PlantLastRecord(
        plantId = plantId,
        lastData = lastData,
        cacheTimestamp = now
    )
}

// Función para evaluar avisos de humedad de tierra
// Nota: Valores altos (>500) = muy seco (necesita agua)
//       Valores bajos (<400) = muy mojado (tiene mucha agua)
//       Valores entre 400-500 = OK
fun evaluateMoistureAlert(moistureValue: String?): PlantAlert? {
    if (moistureValue == null) return null
    
    val moisture = moistureValue.toDoubleOrNull() ?: return null
    
    return when {
        moisture > 500 -> PlantAlert(
            type = AlertType.MOISTURE,
            message = "Le hace falta agua",
            severity = AlertSeverity.LOW
        )
        moisture < 400 -> PlantAlert(
            type = AlertType.MOISTURE,
            message = "Tiene mucha agua",
            severity = AlertSeverity.HIGH
        )
        else -> PlantAlert(
            type = AlertType.MOISTURE,
            message = "Nivel de agua: OK",
            severity = AlertSeverity.OK
        ) // Entre 400-500, está OK
    }
}

// Función para evaluar avisos de temperatura
fun evaluateTemperatureAlert(temperature: String?): PlantAlert? {
    if (temperature == null) return null
    
    val temp = temperature.toDoubleOrNull() ?: return null
    
    return if (temp >= 36) {
        PlantAlert(
            type = AlertType.TEMPERATURE,
            message = "Temperatura alta: ${temp.toInt()}°C",
            severity = AlertSeverity.HIGH
        )
    } else {
        PlantAlert(
            type = AlertType.TEMPERATURE,
            message = "Temperatura: ${temp.toInt()}°C",
            severity = AlertSeverity.OK
        )
    }
}

// Función para evaluar avisos de luminosidad según la hora
fun evaluateLightAlert(lux: String?, timestamp: Long): PlantAlert? {
    if (lux == null) return null
    
    val luxValue = lux.toDoubleOrNull() ?: return null
    
    // Obtener hora del día (0-23) desde el timestamp
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = if (timestamp > 1000000000000L) timestamp else timestamp * 1000
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    
    // Solo evaluar entre 6am-6pm (6-18 horas)
    if (hour < 6 || hour >= 18) {
        return null // No mostrar aviso de luz fuera de horas de día
    }
    
    return when {
        hour >= 6 && hour < 9 -> {
            // 6:00 - 9:00: Muy Bajo < 5,000 lx, Máximo: 10,000 - 15,000 lx
            when {
                luxValue < 5000 -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz muy baja",
                    severity = AlertSeverity.LOW
                )
                luxValue > 15000 -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz muy alta",
                    severity = AlertSeverity.HIGH
                )
                else -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz: OK",
                    severity = AlertSeverity.OK
                )
            }
        }
        hour >= 9 && hour < 12 -> {
            // 9:00 - 12:00: Muy Bajo < 8,000 lx, Máximo: 20,000 - 25,000 lx
            when {
                luxValue < 8000 -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz muy baja",
                    severity = AlertSeverity.LOW
                )
                luxValue > 25000 -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz muy alta",
                    severity = AlertSeverity.HIGH
                )
                else -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz: OK",
                    severity = AlertSeverity.OK
                )
            }
        }
        hour >= 12 && hour < 15 -> {
            // 12:00 - 15:00: Muy Bajo < 10,000 lx, Máximo: 25,000 - 30,000 lx
            when {
                luxValue < 10000 -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz muy baja",
                    severity = AlertSeverity.LOW
                )
                luxValue > 30000 -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz muy alta",
                    severity = AlertSeverity.HIGH
                )
                else -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz: OK",
                    severity = AlertSeverity.OK
                )
            }
        }
        hour >= 15 && hour < 18 -> {
            // 15:00 - 18:00: Muy Bajo < 8,000 lx, Máximo: 15,000 - 25,000 lx
            when {
                luxValue < 8000 -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz muy baja",
                    severity = AlertSeverity.LOW
                )
                luxValue > 25000 -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz muy alta",
                    severity = AlertSeverity.HIGH
                )
                else -> PlantAlert(
                    type = AlertType.LIGHT,
                    message = "Luz: OK",
                    severity = AlertSeverity.OK
                )
            }
        }
        else -> null
    }
}

// Función para obtener todos los avisos de una planta
fun getPlantAlerts(lastRecord: PlantLastRecord): List<PlantAlert> {
    val alerts = mutableListOf<PlantAlert>()
    
    val data = lastRecord.lastData ?: return emptyList()
    
    // Evaluar humedad
    evaluateMoistureAlert(data.moisture_value)?.let { alerts.add(it) }
    
    // Evaluar temperatura
    evaluateTemperatureAlert(data.temperature)?.let { alerts.add(it) }
    
    // Evaluar luminosidad
    evaluateLightAlert(data.lux, data.timestamp)?.let { alerts.add(it) }
    
    return alerts
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
