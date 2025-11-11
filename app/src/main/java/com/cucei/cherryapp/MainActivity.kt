package com.cucei.cherryapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Download
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import android.graphics.Matrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.cucei.cherryapp.ui.theme.CherryAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.SharedPreferences
import java.util.UUID
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.asAndroidBitmap
import com.cucei.cherryapp.ui.CameraScreen
import com.cucei.cherryapp.ui.theme.WhiteButton
import com.cucei.cherryapp.ui.theme.BlackText
import com.cucei.cherryapp.ui.theme.DarkButton
import com.cucei.cherryapp.ui.theme.WhiteText

// Importar funciones del ServerConnection.kt
import com.cucei.cherryapp.getServerPlants
import com.cucei.cherryapp.getServerPlantData
import com.cucei.cherryapp.testServerConnection
import com.cucei.cherryapp.getLastPlantRecord
import com.cucei.cherryapp.getPlantAlerts
import com.cucei.cherryapp.PlantLastRecord
import com.cucei.cherryapp.PlantAlert
import com.cucei.cherryapp.ConectarServidorScreen
import com.cucei.cherryapp.ListaPlantasServidorScreen
import com.cucei.cherryapp.DatosPlantaServidorScreen
import com.cucei.cherryapp.EditarPlantaScreen
import com.cucei.cherryapp.updatePlant

// Sealed class Pantalla actualizada
sealed class Pantalla {
    object SplashScreen : Pantalla()
    object Inicio : Pantalla()
    object ConectarServidor : Pantalla()
    object ListaPlantasServidor : Pantalla()
    object DatosPlantaServidor : Pantalla()
    object DatosPlantaFiltro : Pantalla()
    object ListaRegistrosFiltrados : Pantalla()
    object Galeria : Pantalla()
    object AnalisisPlanta : Pantalla()
    object EditarPlanta : Pantalla()
    
}

// Data class para items del Navigation Drawer
data class DrawerItem(
    val title: String,
    val icon: @Composable () -> Unit,
    val screen: Pantalla,
    val description: String
)

data class Registro(val temperatura: String, val humedad: String, val luminosidad: String)

// Data class para el análisis de imagen
data class ImageAnalysis(
    val isGood: Boolean,
    val issues: List<String>,
    val suggestions: List<String>
)

data class DiseasePrediction(
    val prediction: String,
    val confidence: Float,
    val preventionMeasures: List<String>
)

// Las data classes ServerPlant y ServerPlantData están definidas en ServerConnection.kt

// Data class para huertos guardados
data class HuertoGuardado(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val ip: String,
    val puerto: Int,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val ultimaConexion: Long = System.currentTimeMillis()
)

// MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CherryAppTheme {
                CherryApp()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val nightMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightMode) {
            Configuration.UI_MODE_NIGHT_YES -> Log.d("Config", "Cambio a modo oscuro - sin recrear actividad")
            Configuration.UI_MODE_NIGHT_NO -> Log.d("Config", "Cambio a modo claro - sin recrear actividad")
            else -> Log.d("Config", "Cambio de uiMode detectado")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CherryApp() {
    val context = LocalContext.current
    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.SplashScreen) }
    
    var error by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var fotos by remember { mutableStateOf<List<File>>(emptyList()) }
    var showAnalisisEnGaleria by remember { mutableStateOf(false) } // false = fotos, true = análisis
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    val FOTOS_DIR = remember { File(context.filesDir, "CherryFotos").apply { mkdirs() } }

    var imagenSeleccionada by remember { mutableStateOf<File?>(null) }
    var showConfirmDelete by remember { mutableStateOf<File?>(null) }
    var showCamaraPersonalizada by remember { mutableStateOf(false) }
    var showVistaPrevia by remember { mutableStateOf<File?>(null) }
    var showResultadosAnalisis by remember { mutableStateOf<String?>(null) }
    var analizandoPlanta by remember { mutableStateOf(false) }
    var currentPhotoIndex by remember { mutableStateOf(0) }
    var rotationAngle by remember { mutableStateOf(0f) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var showAnalysisDetails by remember { mutableStateOf(false) }
    
    // Variables para análisis real con TensorFlow Lite
    var showRealAnalysisScreen by remember { mutableStateOf(false) }
    var realAnalysisProgress by remember { mutableStateOf(0f) }
    var realAnalysisMessage by remember { mutableStateOf("Iniciando análisis...") }
    var imagenRealAnalysis by remember { mutableStateOf<File?>(null) }
    var realAnalysisResult by remember { mutableStateOf<DiseasePrediction?>(null) }
    var analisisFromGaleria by remember { mutableStateOf(false) } // Para rastrear si viene de galería
    
    // Variables para captura de análisis
    var showCapturaDialog by remember { mutableStateOf(false) }
    var capturaFile by remember { mutableStateOf<File?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    
    // Variable para AlertDialog de restricción de análisis
    var showAnalysisRestrictionDialog by remember { mutableStateOf(false) }
    
    // --- Estado para Huertos ---
    var serverInput by remember { mutableStateOf("") } // Ej: 192.168.100.26:2000
    var recentServers by remember { mutableStateOf<List<String>>(emptyList()) }
    var serverPlants by remember { mutableStateOf<List<ServerPlant>>(emptyList()) }
    var selectedPlantId by remember { mutableStateOf<String?>(null) }
    var serverPlantData by remember { mutableStateOf<List<ServerPlantData>>(emptyList()) }
    var registrosFiltrados by remember { mutableStateOf<List<ServerPlantData>>(emptyList()) }
    var isConnecting by remember { mutableStateOf(false) }
    
    // --- Estado para Huertos Guardados ---
    var huertosGuardados by remember { mutableStateOf<List<HuertoGuardado>>(emptyList()) }
    
    // --- Estado para Edición de Huertos ---
    var showEditarHuertoDialog by remember { mutableStateOf(false) }
    var huertoEditando by remember { mutableStateOf<HuertoGuardado?>(null) }
    var nuevoNombreHuerto by remember { mutableStateOf("") }
    var showEliminarHuertoDialog by remember { mutableStateOf(false) }
    
    // --- Estado para últimos registros de plantas (caché) ---
    var plantLastRecords by remember { mutableStateOf<Map<String, PlantLastRecord>>(emptyMap()) }
    
    // --- Estado derivado: nombre de la planta seleccionada ---
    val selectedPlantName = selectedPlantId?.let { id ->
        serverPlants.find { it.plant_id == id }?.plant_name
    }
    
    // --- Estado para edición de planta ---
    var plantaEditando by remember { mutableStateOf<ServerPlant?>(null) }

    // Navigation Drawer state
    var drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var scope = rememberCoroutineScope()
    
    
    
    // TensorFlow Lite interpreter
    val tensorFlowInterpreter = remember { loadTensorFlowModel(context) }
    
    // Funciones para persistir IPs recientes
    fun saveRecentServers(servers: List<String>) {
        val sharedPrefs = context.getSharedPreferences("CultivAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putStringSet("recent_servers", servers.toSet())
        editor.apply()
    }
    
    fun loadRecentServers(): List<String> {
        val sharedPrefs = context.getSharedPreferences("CultivAppPrefs", Context.MODE_PRIVATE)
        val serversSet = sharedPrefs.getStringSet("recent_servers", emptySet()) ?: emptySet()
        return serversSet.toList()
    }
    
    // Funciones para manejar huertos guardados
    fun saveHuertosGuardados(huertos: List<HuertoGuardado>) {
        val sharedPrefs = context.getSharedPreferences("CultivAppPrefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(huertos)
        sharedPrefs.edit().putString("huertos_guardados", json).apply()
    }
    
    fun loadHuertosGuardados(): List<HuertoGuardado> {
        val sharedPrefs = context.getSharedPreferences("CultivAppPrefs", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("huertos_guardados", "[]") ?: "[]"
        return try {
            Gson().fromJson(json, object : TypeToken<List<HuertoGuardado>>(){}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun guardarHuerto(nombre: String, ip: String) {
        val partes = ip.split(":")
        if (partes.size == 2) {
            val nuevaIp = partes[0]
            val puerto = partes[1].toIntOrNull() ?: 2000
            val nuevoHuerto = HuertoGuardado(
                nombre = nombre,
                ip = nuevaIp,
                puerto = puerto
            )
            val huertosActualizados = huertosGuardados + nuevoHuerto
            huertosGuardados = huertosActualizados
            saveHuertosGuardados(huertosActualizados)
        }
    }
    
    fun esIpNueva(ip: String): Boolean {
        return huertosGuardados.none { "${it.ip}:${it.puerto}" == ip }
    }
    
    fun actualizarUltimaConexion(ip: String) {
        val huertosActualizados = huertosGuardados.map { huerto ->
            if ("${huerto.ip}:${huerto.puerto}" == ip) {
                huerto.copy(ultimaConexion = System.currentTimeMillis())
            } else {
                huerto
            }
        }
        huertosGuardados = huertosActualizados
        saveHuertosGuardados(huertosActualizados)
    }
    
    fun editarHuerto(huerto: HuertoGuardado, nuevoNombre: String) {
        val huertosActualizados = huertosGuardados.map { h ->
            if (h.id == huerto.id) {
                h.copy(nombre = nuevoNombre)
            } else {
                h
            }
        }
        huertosGuardados = huertosActualizados
        saveHuertosGuardados(huertosActualizados)
    }
    
    fun eliminarHuerto(huerto: HuertoGuardado) {
        val huertosActualizados = huertosGuardados.filter { it.id != huerto.id }
        huertosGuardados = huertosActualizados
        saveHuertosGuardados(huertosActualizados)
    }
    
    fun obtenerNombreHuerto(serverInput: String): String? {
        return huertosGuardados.find { "${it.ip}:${it.puerto}" == serverInput }?.nombre
    }
    

    // Para el doble toque para salir y Snackbar
    var backPressedTime by remember { mutableStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val activity = (LocalContext.current as? Activity)
    
    // Estado para la pantalla de carga
    var splashProgress by remember { mutableStateOf(0f) }
    var splashMessage by remember { mutableStateOf("Inicializando...") }
    var splashVisible by remember { mutableStateOf(true) }
    var showProgressBar by remember { mutableStateOf(false) }
    
    // Efecto para manejar la pantalla de carga
    LaunchedEffect(Unit) {
        if (pantalla == Pantalla.SplashScreen) {
            // Esperar 1 segundo para que aparezcan los elementos
            delay(1000)
            
            // Mostrar la barra de progreso
            showProgressBar = true
            
            // Simular carga con diferentes mensajes
            val messages = listOf(
                "Inicializando...",
                "Cargando módulos...",
                "Preparando cámara...",
                "Listo"
            )
            
            messages.forEachIndexed { index, message ->
                splashMessage = message
                delay(1500) // 1500ms por mensaje
                splashProgress = (index + 1) / messages.size.toFloat()
            }
            
            delay(1000) // Pausa final
            splashVisible = false // Iniciar fade-out
            delay(500) // Tiempo para la animación de fade-out
            pantalla = Pantalla.Inicio
        }
    }
    
    // Efecto para cargar IPs recientes al iniciar
    LaunchedEffect(Unit) {
        recentServers = loadRecentServers()
        huertosGuardados = loadHuertosGuardados()
    }
    


    // Definir items del Navigation Drawer
    val drawerItems = remember {
        listOf(
            DrawerItem(
                title = "Inicio",
                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                screen = Pantalla.Inicio,
                description = "Pantalla principal de la aplicación"
            ),
            DrawerItem(
                title = "Huertos",
                icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                screen = Pantalla.ConectarServidor,
                description = "Configura la conexión con el servidor"
            ),
            DrawerItem(
                title = "Tomar Foto",
                icon = { Icon(Icons.Filled.Camera, contentDescription = null) },
                screen = Pantalla.AnalisisPlanta,
                description = "Captura imágenes con la cámara"
            ),
            DrawerItem(
                title = "Ver Galería",
                icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                screen = Pantalla.Galeria,
                description = "Visualiza las fotos que has tomado"
            ),
            
        )
    }

    // --- MANEJO DEL BOTÓN ATRÁS ---
    BackHandler(enabled = true) {
        Log.d("BackHandler", "Principal: Pantalla actual: $pantalla")
        when (pantalla) {
            Pantalla.Inicio -> {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    Log.d("BackHandler", "Principal: Saliendo de la app")
                    activity?.finish()
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Presiona atrás nuevamente\npara salir de la aplicación",
                            duration = SnackbarDuration.Short
                        )
                    }
                    backPressedTime = System.currentTimeMillis()
                }
            }
            Pantalla.Galeria, Pantalla.ConectarServidor -> {
                Log.d("BackHandler", "Principal: Volviendo a Inicio desde $pantalla")
                pantalla = Pantalla.Inicio
            }
            Pantalla.ListaPlantasServidor -> {
                Log.d("BackHandler", "Principal: Volviendo a ConectarServidor desde ListaPlantasServidor")
                pantalla = Pantalla.ConectarServidor
            }
            Pantalla.AnalisisPlanta -> {
                Log.d("BackHandler", "Principal: Volviendo a Inicio desde Análisis de Plantas")
                pantalla = Pantalla.Inicio
            }
            Pantalla.DatosPlantaServidor -> {
                Log.d("BackHandler", "Volviendo a lista de plantas del servidor")
                pantalla = Pantalla.ListaPlantasServidor
            }
            Pantalla.DatosPlantaFiltro -> {
                Log.d("BackHandler", "Volviendo a lista de plantas filtradas")
                pantalla = Pantalla.ListaRegistrosFiltrados
            }
            Pantalla.EditarPlanta -> {
                Log.d("BackHandler", "Volviendo a lista de plantas desde edición")
                pantalla = Pantalla.ListaPlantasServidor
                plantaEditando = null
            }
            else -> {
                Log.d("BackHandler", "Principal: Estado no manejado - $pantalla")
            }
        }
    }

    // BackHandler para cerrar la imagen en pantalla completa
    if (imagenSeleccionada != null) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Cerrando imagen a pantalla completa")
            imagenSeleccionada = null
        }
    }

    // BackHandler para la cámara personalizada
    if (showCamaraPersonalizada) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Cerrando cámara personalizada")
            showCamaraPersonalizada = false
        }
    }

    // BackHandler para la vista previa de foto
    if (showVistaPrevia != null) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Cerrando vista previa de foto")
            showVistaPrevia = null
        }
    }

    // BackHandler para los resultados de análisis
    if (showResultadosAnalisis != null) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Regresando a vista previa desde resultados de análisis")
            showResultadosAnalisis = null
            // Regresar a la vista previa de la foto si existe imagenRealAnalysis
            if (imagenRealAnalysis != null) {
                showVistaPrevia = imagenRealAnalysis
                imagenRealAnalysis = null
            }
        }
    }

    // BackHandler para la pantalla de análisis real
    if (showRealAnalysisScreen) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Cerrando pantalla de análisis real")
            showRealAnalysisScreen = false
            realAnalysisProgress = 0f
            realAnalysisMessage = "Iniciando análisis..."
            imagenRealAnalysis = null
            realAnalysisResult = null
            // No cambiar analisisFromGaleria aquí, se maneja en los resultados
        }
    }

    // BackHandler para navegación del flujo de registros (Propuesta A)
    if (pantalla == Pantalla.ListaRegistrosFiltrados) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Regresando de ListaRegistrosFiltrados a DatosPlantaFiltro")
            pantalla = Pantalla.DatosPlantaFiltro
        }
    }

    if (pantalla == Pantalla.DatosPlantaFiltro) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Regresando de DatosPlantaFiltro a ListaPlantasServidor")
            pantalla = Pantalla.ListaPlantasServidor
        }
    }
    
    // BackHandler para diálogo de edición de huerto
    if (showEditarHuertoDialog) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Cerrando diálogo de edición de huerto")
            showEditarHuertoDialog = false
            huertoEditando = null
            nuevoNombreHuerto = ""
        }
    }
    
    // BackHandler para diálogo de eliminación de huerto
    if (showEliminarHuertoDialog) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Cerrando diálogo de eliminación de huerto")
            showEliminarHuertoDialog = false
            showEditarHuertoDialog = true
        }
    }





    // Cámara (ActivityResultLauncher) - Simplificado para resolución fija 512x512
    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && fotoUri != null) {
            val file = File(fotoUri!!.path!!)
            try {
                val original = BitmapFactory.decodeFile(file.absolutePath)
                if (original != null) {
                    val scaled = Bitmap.createScaledBitmap(original, 512, 512, true)
                    file.outputStream().use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    original.recycle()
                    scaled.recycle()
                }
            } catch (e: Exception) {
                coroutineScope.launch { snackbarHostState.showSnackbar("No se pudo ajustar la resolución: ${e.localizedMessage}") }
                Log.e("CherryApp", "Error al ajustar resolución", e)
            }
            // Foto tomada exitosamente
        } else if (!success) {
            coroutineScope.launch { snackbarHostState.showSnackbar("No se pudo tomar la foto") }
        }
        fotoUri = null
    }

    fun abrirCamaraNativa() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val nombreFoto = File(FOTOS_DIR, "IMG_${timestamp}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                nombreFoto
            )
            fotoUri = uri
            takePhotoLauncher.launch(uri)
        } catch (e: Exception) {
            coroutineScope.launch { snackbarHostState.showSnackbar("No se pudo abrir la cámara: ${e.localizedMessage}") }
            Log.e("CherryApp", "Error al abrir cámara", e)
        }
    }

    // Permisos
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Permiso de cámara concedido.") }
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Permiso de cámara denegado.") }
        }
    }

    // File picker para JSON
    

    // Navigation Drawer (deshabilitado durante splash screen)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "🌱 CultivApp",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { item.icon() },
                        label = { Text(item.title) },
                        selected = pantalla == item.screen,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                            }
                            when (item.screen) {
                                Pantalla.Galeria -> {
                                    fotos = getFotosNormales(FOTOS_DIR)
                                    showAnalisisEnGaleria = false
                                    pantalla = item.screen
                                }
                                
                                else -> {
                                    pantalla = item.screen
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                // Espaciador para empujar el footer hacia abajo
                Spacer(modifier = Modifier.weight(1f))
                
                // Footer del Navigation Drawer - Tarjeta invisible con logo CUCEI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                                            // Logo CUCEI limpio con inversión de colores según el tema
                    Image(
                        painter = painterResource(id = R.drawable.logo_cucei),
                        contentDescription = "Logo CUCEI",
                        modifier = Modifier
                            .size(270.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = if (isSystemInDarkTheme()) {
                            androidx.compose.ui.graphics.ColorFilter.tint(
                                color = androidx.compose.ui.graphics.Color.White,
                                blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn
                            )
                        } else {
                            null
                        }
                    )
                    }
                }
            }
        }
        ) {
    Scaffold(
        // Deshabilitar gestos durante splash screen, cámara y vista previa
        modifier = if (pantalla == Pantalla.SplashScreen || showCamaraPersonalizada || showVistaPrevia != null) {
            Modifier.pointerInput(Unit) {
                detectDragGestures { _, _ -> 
                    // No hacer nada - bloquear todos los gestos
                }
            }
        } else {
            Modifier
        },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (pantalla) {
                                Pantalla.SplashScreen -> "CultivApp"
                                Pantalla.Inicio -> "Inicio"
                                Pantalla.ConectarServidor -> "Huertos"
                                Pantalla.ListaPlantasServidor -> {
                                    val nombreHuerto = obtenerNombreHuerto(serverInput)
                                    if (nombreHuerto != null) "Plantas de $nombreHuerto" else "Plantas del Huerto"
                                }
                                Pantalla.DatosPlantaServidor -> "Datos de la planta"
                                Pantalla.DatosPlantaFiltro -> "Registro de plantas"
                                Pantalla.AnalisisPlanta -> "Análisis de Plantas"
                                Pantalla.Galeria -> "Galería"
                                Pantalla.EditarPlanta -> "Editar Planta"
                                
                                else -> "CultivApp"
                            }
                        )
                    },
                    navigationIcon = {
                        if (pantalla != Pantalla.SplashScreen && !showCamaraPersonalizada && showVistaPrevia == null) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menú")
                            }
                        }
                    },
                    actions = {
                        // Botones de rotación en el header (solo en vista previa)
                        if (showVistaPrevia != null) {
                            IconButton(
                                onClick = { rotationAngle -= 90f },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.RotateLeft,
                                    contentDescription = "Rotar izquierda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { rotationAngle += 90f },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.RotateRight,
                                    contentDescription = "Rotar derecha",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { contentPadding ->
            Box(modifier = Modifier.padding(contentPadding)) {

                // Pantalla de Carga (Splash Screen)
                if (pantalla == Pantalla.SplashScreen) {
                    SplashScreen(
                        progress = splashProgress,
                        message = splashMessage,
                        visible = splashVisible,
                        showProgress = showProgressBar
                    )
                }


                // Pantalla principal (Inicio)
                if (pantalla == Pantalla.Inicio && !showCamaraPersonalizada && imagenSeleccionada == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
                            Text("🌱", fontSize = 48.sp)
                            Spacer(Modifier.width(16.dp))
                            Text("CultivApp", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "¡Bienvenido/a a CultivApp!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            "Para acceder a todas las funcionalidades, presiona el icono de menú (☰) en la esquina superior izquierda.",
                            fontSize = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Funcionalidades disponibles:",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text("🌐 Conectar al Servidor: Conecta con tu base de datos local")
                                Text("📸 Tomar Foto: Captura imágenes con la cámara")
                                Text("🖼️ Ver Galería: Visualiza las fotos que has tomado")
                            }
                        }
                    }
                }

                // Lista de plantas del servidor (usando ServerConnection.kt)
                if (pantalla == Pantalla.ListaPlantasServidor && serverInput.isNotBlank()) {
                    LaunchedEffect(serverInput) {
                        isConnecting = true
                        try {
                            serverPlants = getServerPlants(serverInput)
                        } catch (e: Exception) {
                            Log.e("Server", "Error obteniendo plantas", e)
                            serverPlants = emptyList()
                        } finally {
                            isConnecting = false
                        }
                    }
                    
                    // Cargar últimos registros de sensores en segundo plano
                    LaunchedEffect(serverPlants.map { it.plant_id }.joinToString()) {
                        if (serverPlants.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val newRecords = mutableMapOf<String, PlantLastRecord>()
                                
                                serverPlants.forEach { plant ->
                                    try {
                                        val cachedRecord = plantLastRecords[plant.plant_id]
                                        val lastRecord = getLastPlantRecord(
                                            serverInput = serverInput,
                                            plantId = plant.plant_id,
                                            cachedRecord = cachedRecord,
                                            cacheTimeoutMs = 60000 // 1 minuto
                                        )
                                        newRecords[plant.plant_id] = lastRecord
                                    } catch (e: Exception) {
                                        Log.e("PlantAlerts", "Error obteniendo último registro para ${plant.plant_id}: ${e.message}")
                                    }
                                }
                                
                                // Actualizar estado en el hilo principal
                                withContext(Dispatchers.Main) {
                                    plantLastRecords = newRecords
                                }
                            }
                        }
                    }

                    ListaPlantasServidorScreen(
                        serverInput = serverInput,
                        serverPlants = serverPlants,
                        isConnecting = isConnecting,
                        onPlantClick = { plantId ->
                            selectedPlantId = plantId
                            pantalla = Pantalla.DatosPlantaFiltro
                        },
                        onEditPlant = { plant ->
                            plantaEditando = plant
                            pantalla = Pantalla.EditarPlanta
                        },
                        plantLastRecords = plantLastRecords
                    )
                }

// Datos de la planta elegida (nueva pantalla de filtros)
                if (pantalla == Pantalla.DatosPlantaFiltro && selectedPlantId != null && serverInput.isNotBlank()) {
                    val selId = selectedPlantId!!
                    LaunchedEffect(selId) {
                        isConnecting = true
                        try {
                            serverPlantData = getServerPlantData(serverInput, selId)
                        } catch (e: Exception) {
                            Log.e("Server", "Error obteniendo datos de planta", e)
                            serverPlantData = emptyList()
                        } finally {
                            isConnecting = false
                        }
                    }

                    PlantDataFilterScreen(
                        serverInput = serverInput,
                        selectedPlantId = selId,
                        selectedPlantName = selectedPlantName,
                        serverPlantData = serverPlantData,
                        isConnecting = isConnecting,
                        onViewRecords = { lista ->
                            registrosFiltrados = lista
                            pantalla = Pantalla.ListaRegistrosFiltrados
                        }
                    )
                }

                if (pantalla == Pantalla.ListaRegistrosFiltrados) {
                    ListaRegistrosFiltradosScreen(
                        title = "Registros filtrados",
                        items = registrosFiltrados,
                        onBack = { pantalla = Pantalla.DatosPlantaFiltro }
                    )
                }
                
                // Pantalla de edición de planta
                if (pantalla == Pantalla.EditarPlanta && plantaEditando != null && serverInput.isNotBlank()) {
                    EditarPlantaScreen(
                        serverInput = serverInput,
                        plant = plantaEditando!!,
                        onBack = {
                            pantalla = Pantalla.ListaPlantasServidor
                            plantaEditando = null
                        },
                        onSave = {
                            // Recargar lista de plantas después de guardar
                            coroutineScope.launch {
                                try {
                                    serverPlants = getServerPlants(serverInput)
                                } catch (e: Exception) {
                                    Log.e("Server", "Error recargando plantas", e)
                                }
                            }
                        }
                    )
                }

                // Pantalla: Huertos
                if (pantalla == Pantalla.ConectarServidor && !showCamaraPersonalizada && imagenSeleccionada == null && showVistaPrevia == null && showResultadosAnalisis == null) {
                    ConectarServidorScreen(
                        serverInput = serverInput,
                        onServerInputChange = { serverInput = it },
                        recentServers = recentServers,
                        onRecentServerClick = { addr -> 
                            if (addr.isEmpty()) {
                                // Limpiar todas las IPs recientes
                                recentServers = emptyList()
                                saveRecentServers(recentServers)
                            } else if (addr.startsWith("DELETE:")) {
                                // Eliminar una IP específica
                                val ipToDelete = addr.substring(7) // Remover "DELETE:" del inicio
                                recentServers = recentServers.filter { it != ipToDelete }
                                saveRecentServers(recentServers)
                            } else {
                                // Seleccionar una IP para conectar
                                serverInput = addr
                                if (!recentServers.contains(addr)) {
                                    recentServers = (listOf(addr) + recentServers).take(5)
                                    saveRecentServers(recentServers)
                                }
                            }
                        },
                        huertosGuardados = huertosGuardados,
                        onHuertoClick = { ip ->
                            serverInput = ip
                            if (!recentServers.contains(ip)) {
                                recentServers = (listOf(ip) + recentServers).take(5)
                                saveRecentServers(recentServers)
                            }
                            actualizarUltimaConexion(ip)
                            pantalla = Pantalla.ListaPlantasServidor
                        },
                        onEditarHuerto = { huerto ->
                            huertoEditando = huerto
                            nuevoNombreHuerto = huerto.nombre
                            showEditarHuertoDialog = true
                        },
                        onConnectClick = {
                            // Conectar directamente sin mostrar dialogs adicionales
                                if (!recentServers.contains(serverInput)) {
                                    recentServers = (listOf(serverInput) + recentServers).take(5)
                                saveRecentServers(recentServers)
                                }
                                actualizarUltimaConexion(serverInput)
                                pantalla = Pantalla.ListaPlantasServidor
                            },
                        onTestConnection = {
                            // Implementar prueba de conexión
                                    coroutineScope.launch {
                                val result = testServerConnection(serverInput)
                                            snackbarHostState.showSnackbar(result)
                            }
                        },
                        canConnect = serverInput.contains(":") &&
                                    serverInput.split(":").size == 2 &&
                                    serverInput.split(":")[1].all { it.isDigit() },
                        onGuardarHuerto = { nombre, ip, puerto ->
                            val nuevoHuerto = HuertoGuardado(
                                nombre = nombre,
                                ip = ip,
                                puerto = puerto.toIntOrNull() ?: 2000,
                                ultimaConexion = System.currentTimeMillis()
                            )
                            huertosGuardados = huertosGuardados + nuevoHuerto
                            saveHuertosGuardados(huertosGuardados)
                        }
                    )
                }

                // Pantalla de cámara personalizada
                if (showCamaraPersonalizada) {
                    CameraScreen(
                        onBack = {
                            showCamaraPersonalizada = false
                        },
                                            onPhotoTaken = { file ->
                        showVistaPrevia = file
                        showCamaraPersonalizada = false
                        // Analizar la imagen cuando se toma la foto
                        try {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                imageAnalysis = analyzeImageQuality(bitmap)
                                bitmap.recycle()
                            }
                        } catch (e: Exception) {
                            Log.e("ImageAnalysis", "Error analizando imagen", e)
                        }
                    }
                    )
                }

                

                            // Pantalla de análisis de plantas
            if (pantalla == Pantalla.AnalisisPlanta && !showCamaraPersonalizada && imagenSeleccionada == null && showVistaPrevia == null && showResultadosAnalisis == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Título principal
                    Text(
                        "📸 Captura de Imágenes",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                    
                    // Tarjeta principal con opciones
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Botón de cámara
                            Button(
                                onClick = { 
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        showCamaraPersonalizada = true
                                    } else {
                                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Camera,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Cámara",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            // Botón de galería
                            Button(
                                onClick = {
                                    fotos = getFotosNormales(FOTOS_DIR)
                                    showAnalisisEnGaleria = false
                                    pantalla = Pantalla.Galeria
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Galería",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    // Tarjeta de información y tips
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                "ℹ️ Información",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Text(
                                "📏 Tamaño de imagen: 256x256 píxeles",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                "💡 Tips para mejores fotos:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                "• Enfoca la hoja dentro del recuadro\n" +
                                "• Mantén la cámara estable\n" +
                                "• Usa buena iluminación\n" +
                                "• Acerca la cámara al objeto",
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

                

                            // Galería (moderna)
            if (pantalla == Pantalla.Galeria && !showCamaraPersonalizada && imagenSeleccionada == null) {
                    Column(Modifier.fillMaxSize()) {
                    // Toggle para alternar entre fotos y análisis
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón Fotos
                                    Button(
                                onClick = { 
                                    showAnalisisEnGaleria = false
                                    fotos = getFotosNormales(FOTOS_DIR)
                                },
                                        colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!showAnalisisEnGaleria) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📸 Fotos")
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Botón Análisis
                            Button(
                                onClick = { 
                                    showAnalisisEnGaleria = true
                                    fotos = getFotosAnalisis(FOTOS_DIR)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showAnalisisEnGaleria) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Analytics,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("🔬 Análisis")
                                }
                            }
                        }
                    }
                    
                    // Contenido de la galería
                Box(Modifier.fillMaxSize()) {
                    if (fotos.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(24.dp),
                                elevation = CardDefaults.cardElevation(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        if (showAnalisisEnGaleria) "No hay análisis guardados" else "No hay fotos tomadas",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        if (showAnalisisEnGaleria) "Realiza análisis de plantas para ver resultados guardados" else "Usa la cámara para añadir algunas fotos",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(fotos.size) { idx ->
                                val file = fotos[idx]
                                val bitmap = remember(file.path) {
                                    try {
                                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                                    } catch (e: Exception) {
                                        Log.e("CherryApp", "Error cargando bitmap para galería", e)
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Card(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clickable { 
                                                imagenSeleccionada = file
                                                currentPhotoIndex = fotos.indexOf(file)
                                            },
                                        elevation = CardDefaults.cardElevation(4.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Foto ${idx + 1}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.aspectRatio(1f),
                                        elevation = CardDefaults.cardElevation(4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                "Error",
                                                Modifier.align(Alignment.Center),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Botón flotante de cámara moderno
                    FloatingActionButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showCamaraPersonalizada = true
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Abrir cámara",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    }
                }
            }

                            // Pantalla completa de imagen seleccionada (rediseñada con estilo moderno)
            if (imagenSeleccionada != null) {
                val file = imagenSeleccionada!!
                val bitmap = remember(file.path) {
                    try {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    } catch (e: Exception) {
                        Log.e("CherryApp", "Error cargando bitmap para vista completa", e)
                        null
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(rotationZ = rotationAngle),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Error al cargar la imagen", Modifier.align(Alignment.Center))
                    }
                    
                    // Barra superior moderna con controles
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón cerrar
                            IconButton(
                                onClick = { imagenSeleccionada = null },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = "Cerrar", 
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Botón de rotación
                            IconButton(
                                onClick = { rotationAngle += 90f },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.RotateRight,
                                    contentDescription = "Rotar 90°",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Botones de acción
                            Row {
                                IconButton(
                                    onClick = {
                                        // Mostrar información de la imagen
                                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                        val info = "Tamaño: ${bitmap?.width} x ${bitmap?.height} píxeles\n" +
                                                  "Archivo: ${file.name}\n" +
                                                  "Tamaño archivo: ${file.length() / 1024} KB\n" +
                                                  "Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))}"
                                        coroutineScope.launch { 
                                            snackbarHostState.showSnackbar(info, duration = SnackbarDuration.Long) 
                                        }
                                        bitmap?.recycle()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info, 
                                        contentDescription = "Información", 
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.provider", file))
                                            type = "image/jpeg"
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share, 
                                        contentDescription = "Compartir", 
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        // Validar que no sea una imagen de análisis previo
                                        if (file.name.startsWith("ANALISIS_")) {
                                            showAnalysisRestrictionDialog = true
                                            return@IconButton
                                        }
                                        
                                        // Preparar la imagen para análisis real
                                        val tempFile = File(context.cacheDir, "temp_real_analysis_${System.currentTimeMillis()}.jpg")
                                        try {
                                            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                                            if (originalBitmap != null) {
                                                val matrix = Matrix()
                                                matrix.postRotate(rotationAngle)
                                                val rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                                                
                                                val outputStream = tempFile.outputStream()
                                                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                                                outputStream.close()
                                                
                                                originalBitmap.recycle()
                                                rotatedBitmap.recycle()
                                                
                                                // Marcar que viene de galería
                                                analisisFromGaleria = true
                                                
                                                // Mostrar pantalla de análisis real
                                                imagenRealAnalysis = tempFile
                                                showRealAnalysisScreen = true
                                                imagenSeleccionada = null
                                                rotationAngle = 0f
                                                imageAnalysis = null
                                                showAnalysisDetails = false
                                                
                                                // Iniciar análisis real con TensorFlow Lite
                                                coroutineScope.launch { 
                                                    realAnalysisProgress = 0f
                                                    realAnalysisMessage = "Iniciando análisis..."
                                                    
                                                    // Simular carga real con diferentes etapas
                                                    val etapas = listOf(
                                                        "Preparando imagen..." to 0.2f,
                                                        "Cargando modelo de IA..." to 0.4f,
                                                        "Analizando características..." to 0.6f,
                                                        "Procesando con TensorFlow..." to 0.8f,
                                                        "Generando resultados..." to 1.0f
                                                    )
                                                    
                                                    etapas.forEach { (mensaje, progreso) ->
                                                        realAnalysisMessage = mensaje
                                                        delay(800) // 800ms por etapa = 4 segundos total
                                                        realAnalysisProgress = progreso
                                                    }
                                                    
                                                    // Análisis real con TensorFlow Lite
                                                    Log.d("TensorFlow", "Iniciando análisis real desde galería...")
                                                    if (tensorFlowInterpreter != null) {
                                                        Log.d("TensorFlow", "Interpreter disponible, procesando imagen...")
                                                        val originalBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                                        if (originalBitmap != null) {
                                                            Log.d("TensorFlow", "Imagen cargada, ejecutando análisis...")
                                                            realAnalysisResult = analyzeImageWithTensorFlow(tensorFlowInterpreter, originalBitmap)
                                                            Log.d("TensorFlow", "Resultado del análisis: $realAnalysisResult")
                                                            originalBitmap.recycle()
                                                        } else {
                                                            Log.e("TensorFlow", "No se pudo cargar la imagen")
                                                        }
                                                    } else {
                                                        Log.e("TensorFlow", "Interpreter es null, usando fallback")
                                                        // Fallback si no se puede cargar el modelo
                                                        realAnalysisResult = DiseasePrediction(
                                                            prediction = "Error: Modelo no disponible",
                                                            confidence = 0.0f,
                                                            preventionMeasures = listOf("No se pudo cargar el modelo de IA.")
                                                        )
                                                    }
                                                    
                                                    delay(500) // Pausa final
                                                    showRealAnalysisScreen = false
                                                    showResultadosAnalisis = "Análisis real completado"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            coroutineScope.launch { 
                                                snackbarHostState.showSnackbar("Error al procesar la imagen: ${e.message}") 
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                        Icon(
                                            Icons.Default.Send, 
                                        contentDescription = "Analizar con IA", 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                }
                                
                                IconButton(
                                    onClick = { showConfirmDelete = file },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Borrar", 
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Barra inferior moderna con navegación
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flecha izquierda
                            IconButton(
                                onClick = {
                                    if (currentPhotoIndex > 0) {
                                        currentPhotoIndex--
                                        imagenSeleccionada = fotos[currentPhotoIndex]
                                    } else {
                                        coroutineScope.launch { 
                                            snackbarHostState.showSnackbar("Primera imagen") 
                                        }
                                    }
                                },
                                enabled = currentPhotoIndex > 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.NavigateBefore,
                                    contentDescription = "Anterior",
                                    tint = if (currentPhotoIndex > 0) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // Indicador de posición moderno
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${currentPhotoIndex + 1} / ${fotos.size}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            
                            // Flecha derecha
                            IconButton(
                                onClick = {
                                    if (currentPhotoIndex < fotos.size - 1) {
                                        currentPhotoIndex++
                                        imagenSeleccionada = fotos[currentPhotoIndex]
                                    } else {
                                        coroutineScope.launch { 
                                            snackbarHostState.showSnackbar("Última imagen") 
                                        }
                                    }
                                },
                                enabled = currentPhotoIndex < fotos.size - 1,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.NavigateNext,
                                    contentDescription = "Siguiente",
                                    tint = if (currentPhotoIndex < fotos.size - 1) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

                // Vista previa de foto (rediseñada)
                if (showVistaPrevia != null) {
                    val file = showVistaPrevia!!
                    val bitmap = remember(file.path) {
                        try {
                            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                        } catch (e: Exception) {
                            Log.e("CherryApp", "Error cargando bitmap para vista previa", e)
                            null
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Vista previa de foto",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(rotationZ = rotationAngle)
                            )
                        } else {
                            Text("Error al cargar la imagen", Modifier.align(Alignment.Center))
                        }
                        
                        // Análisis de imagen en la parte superior central
                        if (imageAnalysis != null) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(16.dp)
                                    .fillMaxWidth(0.9f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (imageAnalysis!!.isGood) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                                    else 
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                                ),
                                elevation = CardDefaults.cardElevation(8.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            if (imageAnalysis!!.isGood) "✅ Buena calidad" else "⚠️ Problemas detectados",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (imageAnalysis!!.isGood) 
                                                MaterialTheme.colorScheme.onPrimaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(Modifier.weight(1f))
                                        IconButton(
                                            onClick = { showAnalysisDetails = !showAnalysisDetails },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                if (showAnalysisDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = "Ver detalles",
                                                tint = if (imageAnalysis!!.isGood) 
                                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                                else 
                                                    MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    
                                    if (showAnalysisDetails) {
                                        Spacer(Modifier.height(12.dp))
                                        if (imageAnalysis!!.issues.isNotEmpty()) {
                                            Text(
                                                "Problemas detectados:",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (imageAnalysis!!.isGood) 
                                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                                else 
                                                    MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            imageAnalysis!!.issues.forEach { issue ->
                                                Text(
                                                    "• $issue",
                                                    fontSize = 13.sp,
                                                    color = if (imageAnalysis!!.isGood) 
                                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                                    else 
                                                        MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                        if (imageAnalysis!!.suggestions.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "Sugerencias:",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (imageAnalysis!!.isGood) 
                                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                                else 
                                                    MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            imageAnalysis!!.suggestions.forEach { suggestion ->
                                                Text(
                                                    "• $suggestion",
                                                    fontSize = 13.sp,
                                                    color = if (imageAnalysis!!.isGood) 
                                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                                    else 
                                                        MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Botones de acción modernos en la parte inferior
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            ),
                            elevation = CardDefaults.cardElevation(12.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                // Botón principal - Guardar
                                Button(
                                    onClick = {
                                        val destFile = File(FOTOS_DIR, "IMG_${System.currentTimeMillis()}.jpg")
                                        try {
                                            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                                            if (originalBitmap != null) {
                                                val matrix = Matrix()
                                                matrix.postRotate(rotationAngle)
                                                val rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                                                
                                                val outputStream = destFile.outputStream()
                                                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                                                outputStream.close()
                                                
                                                originalBitmap.recycle()
                                                rotatedBitmap.recycle()
                                                
                                                fotos = getFotosNormales(FOTOS_DIR)
                                                coroutineScope.launch { snackbarHostState.showSnackbar("Foto guardada en galería de la app") }
                                                showVistaPrevia = null
                                                rotationAngle = 0f
                                                imageAnalysis = null
                                                showAnalysisDetails = false
                                            }
                                        } catch (e: IOException) {
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Error al guardar foto") }
                                            Log.e("GuardarFoto", "Error al guardar foto en galería interna", e)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        "Guardar Foto",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(Modifier.height(12.dp))
                                
                                // Botones secundarios
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            showVistaPrevia = null
                                            showCamaraPersonalizada = true
                                            imageAnalysis = null
                                            showAnalysisDetails = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "Eliminar",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Button(
                                        onClick = {
                                            // Validar que no sea una imagen de análisis previo
                                            if (file.name.startsWith("ANALISIS_")) {
                                                showAnalysisRestrictionDialog = true
                                                return@Button
                                            }
                                            
                                            // Preparar la imagen para análisis real con TensorFlow
                                            val tempFile = File(context.cacheDir, "temp_real_analysis_${System.currentTimeMillis()}.jpg")
                                                try {
                                                    val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                                                    if (originalBitmap != null) {
                                                        val matrix = Matrix()
                                                        matrix.postRotate(rotationAngle)
                                                        val rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                                                        
                                                        val outputStream = tempFile.outputStream()
                                                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                                                        outputStream.close()
                                                        
                                                        originalBitmap.recycle()
                                                        rotatedBitmap.recycle()
                                                        
                                                    // Marcar que NO viene de galería (viene de vista previa)
                                                    analisisFromGaleria = false
                                                    
                                                    // Mostrar pantalla de análisis real
                                                    imagenRealAnalysis = tempFile
                                                    showRealAnalysisScreen = true
                                                            showVistaPrevia = null
                                                            rotationAngle = 0f
                                                            imageAnalysis = null
                                                            showAnalysisDetails = false
                                                        
                                                    // Iniciar análisis real con TensorFlow Lite
                                                            coroutineScope.launch { 
                                                        realAnalysisProgress = 0f
                                                        realAnalysisMessage = "Iniciando análisis..."
                                                            
                                                        // Simular carga real con diferentes etapas
                                                            val etapas = listOf(
                                                                "Preparando imagen..." to 0.2f,
                                                            "Cargando modelo de IA..." to 0.4f,
                                                            "Analizando características..." to 0.6f,
                                                            "Procesando con TensorFlow..." to 0.8f,
                                                            "Generando resultados..." to 1.0f
                                                            )
                                                            
                                                            etapas.forEach { (mensaje, progreso) ->
                                                            realAnalysisMessage = mensaje
                                                            delay(800) // 800ms por etapa = 4 segundos total
                                                            realAnalysisProgress = progreso
                                                        }
                                                        
                                                        // Análisis real con TensorFlow Lite
                                                        Log.d("TensorFlow", "Iniciando análisis real...")
                                                        if (tensorFlowInterpreter != null) {
                                                            Log.d("TensorFlow", "Interpreter disponible, procesando imagen...")
                                                            val originalBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                                            if (originalBitmap != null) {
                                                                Log.d("TensorFlow", "Imagen cargada, ejecutando análisis...")
                                                                realAnalysisResult = analyzeImageWithTensorFlow(tensorFlowInterpreter, originalBitmap)
                                                                Log.d("TensorFlow", "Resultado del análisis: $realAnalysisResult")
                                                                originalBitmap.recycle()
                                                            } else {
                                                                Log.e("TensorFlow", "No se pudo cargar la imagen")
                                                            }
                                                        } else {
                                                            Log.e("TensorFlow", "Interpreter es null, usando fallback")
                                                            // Fallback si no se puede cargar el modelo
                                                            realAnalysisResult = DiseasePrediction(
                                                                prediction = "Error: Modelo no disponible",
                                                                confidence = 0.0f,
                                                                preventionMeasures = listOf("No se pudo cargar el modelo de IA.")
                                                            )
                                                                    }
                                                                    
                                                                    delay(500) // Pausa final
                                                        showRealAnalysisScreen = false
                                                        showResultadosAnalisis = "Análisis real completado"
                                                                }
                                                    }
                                                } catch (e: Exception) {
                                                    coroutineScope.launch { 
                                                        snackbarHostState.showSnackbar("Error al procesar la imagen: ${e.message}") 
                                                    }
                                                }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !analizandoPlanta
                                    ) {
                                        if (analizandoPlanta) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.onSecondary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                "Analizar con I.A.",
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

                // Pantalla de análisis real con TensorFlow Lite
                if (showRealAnalysisScreen) {
                    RealAnalysisScreen(
                        progress = realAnalysisProgress,
                        message = realAnalysisMessage,
                        imagen = imagenRealAnalysis
                    )
                }



                // Resultados de análisis
                if (showResultadosAnalisis != null) {
                    // Determinar qué tipo de resultados mostrar basado en la imagen
                    if (realAnalysisResult != null) {
                        // Mostrar resultados del análisis real con TensorFlow
                        ResultadosRealAnalisisScreen(
                            imagen = imagenRealAnalysis,
                            resultado = realAnalysisResult!!,
                            onBack = { 
                                showResultadosAnalisis = null
                                if (analisisFromGaleria) {
                                    // Si viene de galería, regresar a galería
                                    pantalla = Pantalla.Galeria
                                    analisisFromGaleria = false
                                } else {
                                    // Si viene de vista previa, regresar a vista previa
                                showVistaPrevia = imagenRealAnalysis
                                }
                                imagenRealAnalysis = null
                                realAnalysisResult = null
                            },
                            isCapturing = isCapturing,
                            onGuardarCaptura = {
                                // Función para guardar captura completa del análisis
                                isCapturing = true
                                coroutineScope.launch {
                                    try {
                                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        val capturaFile = File(FOTOS_DIR, "ANALISIS_${timestamp}.jpg")
                                        
                                        // Crear captura completa del análisis
                                        val density = Density(context)
                                        val bitmap = createAnalysisScreenshot(context, imagenRealAnalysis, realAnalysisResult!!, density)
                                        
                                        if (bitmap != null) {
                                            val outputStream = capturaFile.outputStream()
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                                            outputStream.close()
                                            bitmap.recycle()
                                            
                                            fotos = getFotosAnalisis(FOTOS_DIR)
                                            snackbarHostState.showSnackbar("Captura completa del análisis guardada en galería")
                                        } else {
                                            snackbarHostState.showSnackbar("Error al crear captura del análisis")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error al guardar captura: ${e.message}")
                                        Log.e("Captura", "Error guardando captura", e)
                                    } finally {
                                        isCapturing = false
                                    }
                                }
                            },
                            onCompartirCaptura = {
                                // Función para compartir captura completa del análisis
                                isCapturing = true
                                coroutineScope.launch {
                                    try {
                                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        val tempFile = File(FOTOS_DIR, "ANALISIS_COMPARTIR_${timestamp}.jpg")
                                        
                                        // Crear captura completa del análisis
                                        val density = Density(context)
                                        val bitmap = createAnalysisScreenshot(context, imagenRealAnalysis, realAnalysisResult!!, density)
                                        
                                        if (bitmap != null) {
                                            val outputStream = tempFile.outputStream()
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                                            outputStream.close()
                                            bitmap.recycle()
                                            
                                            // Compartir la captura usando FileProvider
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                type = "image/jpeg"
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Compartir análisis")
                                            context.startActivity(shareIntent)
                                        } else {
                                            snackbarHostState.showSnackbar("Error al crear captura del análisis")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error al compartir captura: ${e.message}")
                                        Log.e("Captura", "Error compartiendo captura", e)
                                    } finally {
                                        isCapturing = false
                                    }
                                }
                            }
                        )
                    } else {
                        // Solo mostrar resultados reales
                        // Esto no debería ejecutarse ya que realAnalysisResult debería estar disponible
                        Log.w("Resultados", "No hay resultados de análisis disponibles")
                    }
                }

                // --- DIÁLOGOS ---
                if (showError && error != null) {
                    AlertDialog(
                        onDismissRequest = { showError = false; error = null },
                        title = { Text("Error") },
                        text = { Text(error ?: "Ocurrió un error desconocido.") },
                        confirmButton = {
                            Button(onClick = { showError = false; error = null }) { Text("OK") }
                        }
                    )
                }
                
                // Diálogo de restricción para análisis de imágenes previas
                if (showAnalysisRestrictionDialog) {
                    AlertDialog(
                        onDismissRequest = { showAnalysisRestrictionDialog = false },
                        title = { Text("⚠️ Imagen no analizable") },
                        text = {
                            Text("Esta imagen es un resultado de análisis previo y no se puede analizar nuevamente. Por favor, selecciona una foto original para realizar el análisis con IA.") 
                        },
                        confirmButton = {
                            Button(onClick = { showAnalysisRestrictionDialog = false }) { 
                                Text("Aceptar") 
                            }
                        }
                    )
                }
                
                // Diálogo para nuevo huerto
                
                
                // Diálogo para editar huerto
                if (showEditarHuertoDialog && huertoEditando != null) {
                    val huerto = huertoEditando!!
                    AlertDialog(
                        onDismissRequest = { 
                            showEditarHuertoDialog = false
                            huertoEditando = null
                            nuevoNombreHuerto = ""
                        },
                        title = { Text("✏️ Editar Huerto: ${huerto.nombre}") },
                        text = {
                            Column {
                                Text("Escribe un nuevo nombre para tu huerto (máximo 20 caracteres):")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = nuevoNombreHuerto,
                                    onValueChange = { 
                                        if (it.length <= 20) nuevoNombreHuerto = it
                                    },
                                    label = { Text("Nuevo nombre del huerto") },
                                    placeholder = { Text("Mi Huerto de Tomates") },
                                    singleLine = true
                                )
                                Text(
                                    text = "${nuevoNombreHuerto.length}/20",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (nuevoNombreHuerto.length > 20) Color.Red else Color.Gray
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { 
                                    if (nuevoNombreHuerto.isNotBlank() && nuevoNombreHuerto != huerto.nombre) {
                                        editarHuerto(huerto, nuevoNombreHuerto)
                                    }
                                    showEditarHuertoDialog = false
                                    huertoEditando = null
                                    nuevoNombreHuerto = ""
                                },
                                enabled = nuevoNombreHuerto.isNotBlank() && nuevoNombreHuerto != huerto.nombre
                            ) {
                                Text("Guardar")
                            }
                        },
                        dismissButton = {
                            Row {
                                TextButton(
                                    onClick = { 
                                        showEliminarHuertoDialog = true
                                        showEditarHuertoDialog = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Elim. huerto")
                                }
                                TextButton(
                                    onClick = { 
                                        showEditarHuertoDialog = false
                                        huertoEditando = null
                                        nuevoNombreHuerto = ""
                                    }
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        }
                    )
                }
                
                // Diálogo de confirmación para eliminar huerto
                if (showEliminarHuertoDialog && huertoEditando != null) {
                    val huerto = huertoEditando!!
                    AlertDialog(
                        onDismissRequest = { 
                            showEliminarHuertoDialog = false
                            huertoEditando = null
                            nuevoNombreHuerto = ""
                        },
                        title = { Text("🗑️ Eliminar Huerto") },
                        text = { Text("¿Estás seguro de que quieres eliminar el huerto '${huerto.nombre}'? Esta acción no se puede deshacer.") },
                        confirmButton = {
                            TextButton(
                                onClick = { 
                                    eliminarHuerto(huerto)
                                    showEliminarHuertoDialog = false
                                    huertoEditando = null
                                    nuevoNombreHuerto = ""
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Eliminar")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { 
                                    showEliminarHuertoDialog = false
                                    showEditarHuertoDialog = true
                                }
                            ) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                if (showConfirmDelete != null) {
                    val fileToDelete = showConfirmDelete!!
                    AlertDialog(
                        onDismissRequest = { showConfirmDelete = null },
                        title = { Text("Confirmar Borrado") },
                        text = { Text("¿Seguro que quieres borrar esta foto?") },
                        confirmButton = {
                            Button(onClick = {
                                if (fileToDelete.delete()) {
                                    fotos = if (showAnalisisEnGaleria) getFotosAnalisis(FOTOS_DIR) else getFotosNormales(FOTOS_DIR)
                                    if (imagenSeleccionada == fileToDelete) {
                                        imagenSeleccionada = null
                                    }
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Foto borrada") }
                                } else {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Error al borrar foto") }
                                }
                                showConfirmDelete = null
                            }) { Text("Borrar") }
                        },
                        dismissButton = {
                            Button(onClick = { showConfirmDelete = null }) { Text("Cancelar") }
                        }
                    )
                }

            }
        }
    }
}

@Composable
fun SplashScreen(progress: Float, message: String, visible: Boolean, showProgress: Boolean) {
    // Animación de fade out para la transición
    val fadeOutAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "fadeOut"
    )
    // Animación de fade in para el logo
    val logoAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "logoAlpha"
    )
    
    // Animación de escala para el logo
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200),
        label = "logoScale"
    )
    
    // Animación de pulse para el logo
    val pulseAnimation by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(fadeOutAlpha)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            // Bloquear todas las interacciones táctiles
            .pointerInput(Unit) {
                detectDragGestures { _, _ -> 
                    // No hacer nada - bloquear todos los gestos
                }
            }
    ) {
        // Contenido principal centrado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo principal con animaciones
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .alpha(logoAlpha)
                    .scale(logoScale * pulseAnimation),
                contentAlignment = Alignment.Center
            ) {
                // Logo de la app (plant.png)
                Image(
                    painter = painterResource(id = R.drawable.plant),
                    contentDescription = "Logo de la app",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Título de la app
            Text(
                "CultivApp",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtítulo
            Text(
                "Tu aplicación completa para el cultivo",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Barra de progreso elegante y fluida (solo se muestra cuando showProgress es true)
            if (showProgress) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            )
                    ) {
                        // Barra de progreso animada y fluida
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Mensaje de estado
                Text(
                    message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Logo CUCEI en el pie de página (invisible)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_cucei),
                        contentDescription = "Logo CUCEI",
                        modifier = Modifier.size(120.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = if (isSystemInDarkTheme()) {
                            androidx.compose.ui.graphics.ColorFilter.tint(
                                color = MaterialTheme.colorScheme.onSurface,
                                blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn
                            )
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onStartApp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo y título
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
            Text("🌱", fontSize = 64.sp)
            Spacer(Modifier.width(16.dp))
            Text("CultivApp", fontSize = 64.sp, fontWeight = FontWeight.Bold)
        }
        
        // Mensaje de bienvenida
        Text(
            "¡Bienvenido/a a CultivApp!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            "Tu aplicación completa para el cultivo y gestión de plantas",
            fontSize = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Instrucciones
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "¿Cómo comenzar?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    "1. Presiona el botón 'Comenzar' para acceder a la aplicación",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    "2. Una vez dentro, presiona el icono de menú (☰) en la esquina superior izquierda",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    "3. Desde el menú podrás acceder a todas las funcionalidades:",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("📸 Tomar Foto: Captura imágenes con la cámara")
                    Text("🖼️ Ver Galería: Visualiza las fotos que has tomado")
                    Text("📂 Cargar Datos (JSON): Carga y visualiza tus registros")
                    Text("📊 Datos de Plantas: Gestiona datos de plantas y gráficos")
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Botón para comenzar
        Button(
            onClick = onStartApp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSystemInDarkTheme()) DarkButton else WhiteButton,
                contentColor = if (isSystemInDarkTheme()) WhiteText else BlackText
            )
        ) {
            Text("Comenzar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AnalisisScreen(progress: Float, message: String, imagen: File?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.surface
            )
            // Bloquear todas las interacciones táctiles
            .pointerInput(Unit) {
                detectDragGestures { _, _ -> 
                    // No hacer nada - bloquear todos los gestos
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono de análisis
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = "Análisis",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Título
            Text(
                "Analizando Planta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mensaje de estado
            Text(
                message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Barra de progreso
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(6.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    // Barra de progreso animada
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Porcentaje
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Imagen que se está analizando (si existe)
            if (imagen != null) {
                val bitmap = remember(imagen.path) {
                    try {
                        BitmapFactory.decodeFile(imagen.absolutePath)?.asImageBitmap()
                    } catch (e: Exception) {
                        Log.e("AnalisisScreen", "Error cargando bitmap", e)
                        null
                    }
                }
                
                if (bitmap != null) {
                    Card(
                        modifier = Modifier
                            .size(200.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Imagen en análisis",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultadosAnalisisScreen(imagen: File?, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con botón de regreso
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "RESULTADOS DEL ANÁLISIS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Imagen analizada
            item {
                if (imagen != null) {
                    val bitmap = remember(imagen.path) {
                        try {
                            BitmapFactory.decodeFile(imagen.absolutePath)?.asImageBitmap()
                        } catch (e: Exception) {
                            Log.e("ResultadosAnalisisScreen", "Error cargando bitmap", e)
                            null
                        }
                    }
                    
                    if (bitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Imagen analizada",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            
            // Estado de la planta
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Estado de la planta: Saludable",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Nivel de confianza
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Confianza: 90%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            // Sección de recomendaciones
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "RECOMENDACIONES",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Lista de recomendaciones
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RecommendationItem(
                                text = "Continúa monitoreando tu planta para detectar cualquier cambio o signo de plagas."
                            )
                            RecommendationItem(
                                text = "Asegúrate de que la planta tenga suficiente luz solar y un riego adecuado para un crecimiento óptimo."
                            )
                            RecommendationItem(
                                text = "Aplica fertilizante según las indicaciones para asegurar una nutrición adecuada."
                            )
                        }
                    }
                }
            }
            
            // Espaciador final
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun RecommendationItem(text: String, iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// Funciones para TensorFlow Lite
fun loadTensorFlowModel(context: Context): Interpreter? {
    try {
        Log.d("TensorFlow", "Iniciando carga del modelo...")
        
        val modelFile = File(context.getExternalFilesDir(null), "tomato.tflite")
        Log.d("TensorFlow", "Ruta del modelo: ${modelFile.absolutePath}")
        
        if (!modelFile.exists()) {
            Log.d("TensorFlow", "Modelo no existe, copiando desde assets...")
            try {
            context.assets.open("tomato.tflite").use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("TensorFlow", "Modelo copiado exitosamente")
            } catch (e: Exception) {
                Log.e("TensorFlow", "Error copiando modelo: ${e.message}")
                e.printStackTrace()
                return null
            }
        } else {
            Log.d("TensorFlow", "Modelo ya existe en: ${modelFile.absolutePath}")
        }
        
        if (!modelFile.exists()) {
            Log.e("TensorFlow", "Modelo aún no existe después de intentar copiarlo")
            return null
        }
        
        val options = Interpreter.Options()
        options.setNumThreads(4)
        
        Log.d("TensorFlow", "Creando Interpreter con archivo: ${modelFile.absolutePath}")
        val interpreter = Interpreter(modelFile, options)
        Log.d("TensorFlow", "✅ Modelo cargado exitosamente!")
        return interpreter
    } catch (e: Throwable) {
        Log.e("TensorFlow", "❌ Error cargando modelo: ${e.javaClass.simpleName}")
        Log.e("TensorFlow", "Mensaje: ${e.message}")
        e.printStackTrace()
        return null
    }
}



fun analyzeImageWithTensorFlow(interpreter: Interpreter, bitmap: Bitmap): DiseasePrediction? {
    return try {
        Log.d("TensorFlow", "Iniciando análisis de imagen...")
        Log.d("TensorFlow", "Tamaño original de imagen: ${bitmap.width}x${bitmap.height}")
        
        // Redimensionar imagen a 128x128 (resolución del modelo entrenado)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
        Log.d("TensorFlow", "Imagen redimensionada a: ${resizedBitmap.width}x${resizedBitmap.height}")
        
        // Obtener información del modelo
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        Log.d("TensorFlow", "Forma de entrada del modelo: ${inputShape.joinToString(", ")}")
        Log.d("TensorFlow", "Forma de salida del modelo: ${outputShape.joinToString(", ")}")
        
        // Preparar buffer de entrada con el tamaño correcto
        val inputBuffer = TensorBuffer.createFixedSize(inputShape, org.tensorflow.lite.DataType.FLOAT32)
        Log.d("TensorFlow", "Buffer de entrada creado con tamaño: ${inputBuffer.buffer.capacity()} bytes")
        
        // Convertir bitmap a array de floats
        val pixels = IntArray(128 * 128)
        resizedBitmap.getPixels(pixels, 0, 128, 0, 0, 128, 128)
        
        val floatArray = FloatArray(128 * 128 * 3)
        var pixelIndex = 0
        for (i in pixels.indices) {
            val pixel = pixels[i]
            floatArray[pixelIndex++] = ((pixel shr 16) and 0xFF) / 255.0f  // R
            floatArray[pixelIndex++] = ((pixel shr 8) and 0xFF) / 255.0f   // G
            floatArray[pixelIndex++] = (pixel and 0xFF) / 255.0f           // B
        }
        
        inputBuffer.loadArray(floatArray)
        Log.d("TensorFlow", "Datos de imagen cargados en buffer")
        
        // Preparar buffer de salida
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, org.tensorflow.lite.DataType.FLOAT32)
        Log.d("TensorFlow", "Buffer de salida preparado")
        
        // Ejecutar inferencia
        Log.d("TensorFlow", "Ejecutando inferencia...")
        interpreter.run(inputBuffer.buffer, outputBuffer.buffer)
        Log.d("TensorFlow", "Inferencia completada")
        
        // Obtener resultados
        val outputArray = outputBuffer.floatArray
        Log.d("TensorFlow", "Resultados obtenidos: ${outputArray.joinToString(", ")}")
        
        // Encontrar la clase con mayor probabilidad
        var maxIndex = 0
        var maxValue = outputArray[0]
        for (i in outputArray.indices) {
            if (outputArray[i] > maxValue) {
                maxValue = outputArray[i]
                maxIndex = i
            }
        }
        
        Log.d("TensorFlow", "Índice máximo: $maxIndex, Valor máximo: $maxValue")
        
        // Mapear índice a clase
        val classLabels = listOf(
            "Tomato_Bacterial_spot",
            "Tomato_Early_blight", 
            "Tomato_Late_blight",
            "Tomato_Leaf_Mold",
            "Tomato_Septoria_leaf_spot",
            "Tomato_Spider_mites_Two_spotted_spider_mite",
            "Tomato__Target_Spot",
            "Tomato__Tomato_YellowLeaf__Curl_Virus",
            "Tomato__Tomato_mosaic_virus",
            "Tomato_healthy"
        )
        
        val predictedClass = classLabels[maxIndex]
        val confidence = maxValue
        
        Log.d("TensorFlow", "Clase predicha: $predictedClass, Confianza: $confidence")
        
        // Obtener medidas de prevención en español
        val preventionMeasures = when (predictedClass) {
            "Tomato_Bacterial_spot" -> listOf(
                "Elimina inmediatamente las hojas y frutos infectados para evitar la propagación de la bacteria.",
                "Usa semillas certificadas libres de patógenos y considera variedades resistentes a mancha bacteriana.",
                "Aplica fungicidas a base de cobre (oxicloruro de cobre o hidróxido de cobre) cada 7-10 días, especialmente en condiciones húmedas.",
                "Implementa rotación de cultivos de al menos 3 años, evitando plantar tomates en la misma área donde hubo infección.",
                "Riega en la base de las plantas usando sistemas de goteo para mantener las hojas secas y reducir la dispersión de la bacteria."
            )
            "Tomato_Early_blight" -> listOf(
                "Retira y destruye las hojas enfermas tan pronto como las detectes para reducir el inóculo de la enfermedad.",
                "Asegura un espaciado adecuado entre plantas (60-90 cm) para mejorar la circulación de aire y reducir la humedad foliar.",
                "Aplica fungicidas protectores como clorotalonil o mancozeb preventivamente, comenzando 2-3 semanas después del trasplante.",
                "Utiliza acolchado (mulch) orgánico alrededor de las plantas para prevenir salpicaduras de tierra que contienen esporas del patógeno.",
                "Riega temprano en la mañana para que las hojas se sequen rápidamente, evitando períodos prolongados de humedad que favorecen la enfermedad."
            )
            "Tomato_Late_blight" -> listOf(
                "Destruye completamente todas las plantas infectadas quemándolas o enterrándolas profundamente, nunca las uses en compost.",
                "Mantén un excelente flujo de aire entre plantas con espaciados de 90 cm o más y poda las hojas inferiores que toquen el suelo.",
                "Aplica fungicidas sistémicos como mefenoxam o clorotalonil preventivamente cuando las condiciones sean favorables (humedad alta, temperaturas frescas).",
                "Evita el riego por aspersión; utiliza riego por goteo y riega solo en la base de las plantas en las primeras horas del día.",
                "Inspecciona diariamente durante períodos húmedos y aplica tratamientos de emergencia si detectas los primeros síntomas (manchas aceitosas en hojas)."
            )
            "Tomato_Leaf_Mold" -> listOf(
                "Mejora significativamente la ventilación en invernaderos abriendo ventanas laterales y usando ventiladores para reducir la humedad relativa por debajo del 85%.",
                "Mantén un espaciado de 60-75 cm entre plantas y poda las hojas inferiores para aumentar la circulación de aire alrededor del follaje.",
                "Riega temprano en la mañana usando riego por goteo en la base de las plantas, nunca mojes el follaje directamente.",
                "Aplica fungicidas como azoxystrobin o clorotalonil cuando detectes los primeros signos de la enfermedad o en condiciones de alta humedad.",
                "Elimina las hojas gravemente infectadas y considera variedades resistentes a moho foliar si cultivas en condiciones de alta humedad."
            )
            "Tomato_Septoria_leaf_spot" -> listOf(
                "Elimina todas las hojas infectadas de la planta y del suelo, especialmente al final de la temporada, ya que el hongo sobrevive en los restos vegetales.",
                "Usa acolchado plástico o orgánico para prevenir que las esporas del suelo salpiquen hacia las hojas durante el riego o la lluvia.",
                "Practica la rotación de cultivos de 2-3 años con plantas no relacionadas (evita pimientos, papas y berenjenas) para reducir la población del patógeno.",
                "Aplica fungicidas protectores como clorotalonil o mancozeb cada 7-14 días durante períodos de alta humedad o lluvia frecuente.",
                "Riega en la base de las plantas temprano en la mañana para que el follaje permanezca seco el mayor tiempo posible durante el día."
            )
            "Tomato_Spider_mites_Two_spotted_spider_mite" -> listOf(
                "Inspecciona regularmente el envés de las hojas buscando ácaros, telarañas finas y manchas amarillentas; actúa rápidamente al detectarlos.",
                "Aumenta la humedad ambiental rociando agua sobre las hojas (solo si no hay otras enfermedades fúngicas) o usando humidificadores, ya que los ácaros prefieren ambientes secos.",
                "Aplica tratamientos con jabón insecticida, aceite de neem o aceite hortícola cubriendo especialmente el envés de las hojas donde se concentran los ácaros.",
                "Introduce depredadores naturales como ácaros fitoseidos (Phytoseiulus persimilis) si estás en invernadero para control biológico a largo plazo.",
                "Elimina las hojas gravemente infestadas y evita el uso excesivo de fertilizantes nitrogenados que promueven brotes tiernos preferidos por los ácaros."
            )
            "Tomato__Target_Spot" -> listOf(
                "Elimina y destruye todas las hojas infectadas para reducir el inóculo del hongo y prevenir la propagación a hojas sanas.",
                "Mantén un espaciado adecuado de 75-90 cm entre plantas para mejorar la circulación de aire y reducir la humedad foliar que favorece el desarrollo del hongo.",
                "Riega únicamente en la base de las plantas usando sistemas de goteo, evitando mojar el follaje especialmente durante las horas de la tarde.",
                "Aplica fungicidas protectores como clorotalonil o azoxystrobin cada 10-14 días, comenzando cuando las plantas estén bien establecidas.",
                "Practica la rotación de cultivos y elimina completamente los restos vegetales al final de la temporada para reducir las fuentes de inóculo para el próximo cultivo."
            )
            "Tomato__Tomato_YellowLeaf__Curl_Virus" -> listOf(
                "Elimina y quema inmediatamente todas las plantas infectadas para prevenir que las moscas blancas transmitan el virus a plantas sanas.",
                "Instala mallas anti-insectos (malla 50 mesh) en invernaderos y usa trampas amarillas pegajosas para monitorear y reducir la población de moscas blancas.",
                "Controla las moscas blancas con insecticidas sistémicos como imidacloprid o pimetrozina, aplicándolos según las recomendaciones del producto.",
                "Siembra variedades resistentes al virus del rizado amarillo cuando sea posible, ya que ofrecen la mejor protección contra esta enfermedad devastadora.",
                "Evita plantar cerca de campos con cultivos hospedantes de moscas blancas y destruye las malas hierbas que pueden servir como reservorios del virus."
            )
            "Tomato__Tomato_mosaic_virus" -> listOf(
                "Destruye completamente las plantas infectadas y no uses sus frutos o semillas, ya que el virus puede transmitirse a través de ellos.",
                "Usa únicamente semillas certificadas libres de virus y considera variedades resistentes al virus del mosaico para futuras plantaciones.",
                "Controla los pulgones que transmiten el virus aplicando jabones insecticidas, aceites o piretroides cuando detectes su presencia.",
                "Lava tus manos y desinfecta todas las herramientas de jardín con una solución de lejía al 10% después de tocar plantas infectadas para evitar la transmisión mecánica.",
                "Elimina las malas hierbas cercanas que pueden ser hospedantes del virus y de los pulgones, y mantén el área de cultivo libre de restos vegetales infectados."
            )
            "Tomato_healthy" -> listOf(
                "Continúa con las prácticas preventivas: monitorea regularmente tus plantas buscando signos tempranos de enfermedades o plagas para actuar rápidamente si aparecen.",
                "Mantén un programa de riego consistente (1-2 veces por semana, según el clima) en la base de las plantas, evitando el estrés hídrico que debilita las defensas naturales.",
                "Aplica fertilizantes balanceados cada 3-4 semanas durante la temporada de crecimiento, prestando atención a niveles adecuados de nitrógeno, fósforo y potasio.",
                "Poda las hojas inferiores que toquen el suelo y mantén el área alrededor de las plantas libre de malezas para reducir el riesgo de enfermedades y mejorar la circulación de aire.",
                "Considera aplicar tratamientos preventivos con fungicidas suaves o productos orgánicos como azufre en polvo durante períodos de alta humedad para proteger la salud continua de las plantas."
            )
            else -> listOf("No hay medidas de prevención disponibles para esta condición. Consulta con un especialista en agricultura para obtener recomendaciones específicas.")
        }
        
        Log.d("TensorFlow", "Análisis completado exitosamente")
        DiseasePrediction(predictedClass, confidence, preventionMeasures)
        
    } catch (e: Exception) {
        Log.e("TensorFlow", "Error en análisis: ${e.message}")
        e.printStackTrace()
        null
    }
}

// Función para capturar pantalla completa
fun captureScreen(
    context: Context,
    view: android.view.View,
    density: Density
): Bitmap? {
    return try {
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)
        bitmap
    } catch (e: Exception) {
        Log.e("ScreenCapture", "Error capturando pantalla: ${e.message}")
        null
    }
}

// Función auxiliar para dividir texto en líneas según el ancho disponible
fun breakTextIntoLines(paint: android.graphics.Paint, text: String, maxWidth: Int): List<String> {
    val lines = mutableListOf<String>()
    val words = text.split(" ")
    var currentLine = ""
    
    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        val width = paint.measureText(testLine)
        
        if (width <= maxWidth) {
            currentLine = testLine
        } else {
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            // Si una sola palabra es más ancha que el máximo, la dividimos
            if (paint.measureText(word) > maxWidth) {
                // Dividir palabra muy larga (aunque esto es raro en español)
                var remainingWord = word
                while (remainingWord.isNotEmpty() && paint.measureText(remainingWord) > maxWidth) {
                    var charCount = remainingWord.length - 1
                    while (charCount > 0 && paint.measureText(remainingWord.substring(0, charCount)) > maxWidth) {
                        charCount--
                    }
                    lines.add(remainingWord.substring(0, charCount))
                    remainingWord = remainingWord.substring(charCount)
                }
                currentLine = remainingWord
            } else {
                currentLine = word
            }
        }
    }
    
    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }
    
    return lines.ifEmpty { listOf(text) } // Si no se pudo dividir, devolver el texto original
}

// Función para crear captura completa del análisis
fun createAnalysisScreenshot(
    context: Context,
    imagen: File?,
    resultado: DiseasePrediction,
    density: Density
): Bitmap? {
    return try {
        // Dimensiones de la captura (ancho fijo, alto dinámico)
        val width = 1080 // Ancho fijo para buena calidad
        val padding = 32 // Padding interno
        val cardSpacing = 16 // Espacio entre tarjetas
        val headerHeight = 80 // Altura del header
        val footerHeight = 120 // Altura del footer con botones
        
        // Calcular altura total basada en el contenido
        var totalHeight = headerHeight + padding
        
        // Altura de la imagen (si existe)
        if (imagen != null) {
            totalHeight += 250 + cardSpacing // Imagen + espacio
        }
        
        // Calcular altura de recomendaciones primero
        val recTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#424242")
            textSize = 24f
            isAntiAlias = true
        }
        val maxTextWidth = width - (padding * 2) - 40
        var recommendationsHeight = 70f // Altura inicial del título y padding
        resultado.preventionMeasures.take(5).forEach { measure ->
            val fullText = "• $measure"
            val lines = breakTextIntoLines(recTextPaint, fullText, maxTextWidth)
            recommendationsHeight += lines.size * 32f + 8f // 32px por línea + 8px espacio entre recomendaciones
        }
        recommendationsHeight += 20f // Padding final
        
        // Calcular altura del estado también
        val statusTextPaint = android.graphics.Paint().apply {
            textSize = 32f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val statusText = if (resultado.prediction == "Tomato_healthy") "Estado: Saludable" else "Enfermedad: ${traducirEnfermedad(resultado.prediction)}"
        val statusLinesCount = breakTextIntoLines(statusTextPaint, statusText, width - (padding * 2) - 40).size
        val statusHeight = maxOf(60, (statusLinesCount * 40) + 20)
        
        // Altura de las tarjetas de información
        totalHeight += statusHeight + cardSpacing // Estado de la planta (calculada)
        totalHeight += 80 + cardSpacing // Confianza
        totalHeight += recommendationsHeight.toInt() + cardSpacing // Recomendaciones (calculada)
        totalHeight += 100 + cardSpacing // Información adicional
        
        totalHeight += footerHeight + padding
        
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Fondo
        canvas.drawColor(android.graphics.Color.WHITE)
        
        var currentY = padding
        
        // Header
        val headerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1976D2")
            textSize = 48f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RESULTADOS DEL ANÁLISIS REAL", padding.toFloat(), currentY + 50f, headerPaint)
        currentY += headerHeight
        
        // Imagen (si existe)
        if (imagen != null) {
            try {
                val imageBitmap = BitmapFactory.decodeFile(imagen.absolutePath)
                if (imageBitmap != null) {
                    // Calcular dimensiones manteniendo la proporción original
                    val maxWidth = width - (padding * 2)
                    val maxHeight = 300 // Altura máxima
                    
                    val originalWidth = imageBitmap.width
                    val originalHeight = imageBitmap.height
                    
                    // Calcular escala manteniendo proporción
                    val scaleX = maxWidth.toFloat() / originalWidth
                    val scaleY = maxHeight.toFloat() / originalHeight
                    val scale = minOf(scaleX, scaleY) // Usar la escala más pequeña para mantener proporción
                    
                    val scaledWidth = (originalWidth * scale).toInt()
                    val scaledHeight = (originalHeight * scale).toInt()
                    
                    // Centrar la imagen horizontalmente
                    val xOffset = padding + (maxWidth - scaledWidth) / 2
                    
                    val scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, scaledWidth, scaledHeight, true)
                    canvas.drawBitmap(scaledBitmap, xOffset.toFloat(), currentY.toFloat(), null)
                    scaledBitmap.recycle()
                    imageBitmap.recycle()
                    
                    // Actualizar currentY con la altura real de la imagen escalada
                    currentY += scaledHeight + cardSpacing
                }
            } catch (e: Exception) {
                Log.e("Screenshot", "Error cargando imagen: ${e.message}")
                currentY += 250 + cardSpacing // Altura por defecto si hay error
            }
        }
        
        // Estado de la planta (usar variables ya calculadas)
        val isHealthy = resultado.prediction == "Tomato_healthy"
        val statusColor = if (isHealthy) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#F44336")
        
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        // Usar statusText y statusHeight ya calculados arriba
        val statusMaxWidth = width - (padding * 2) - 40
        val statusLines = breakTextIntoLines(textPaint, statusText, statusMaxWidth)
        
        val cardPaint = android.graphics.Paint().apply {
            color = statusColor
            isAntiAlias = true
        }
        canvas.drawRoundRect(padding.toFloat(), currentY.toFloat(), (width - padding).toFloat(), (currentY + statusHeight).toFloat(), 12f, 12f, cardPaint)
        
        // Dibujar líneas del estado (centrado verticalmente)
        val lineHeight = 40f // Espaciado entre líneas
        val totalTextHeight = statusLines.size * lineHeight // Altura total del texto
        val centerY = currentY + statusHeight / 2f // Centro vertical del rectángulo
        // Ajustar posición Y: centro del rectángulo - mitad de altura del texto + ajuste de línea base
        var statusY = centerY - (totalTextHeight / 2f) + (lineHeight / 2f)
        statusLines.forEach { line ->
            canvas.drawText(line, (padding + 20).toFloat(), statusY.toFloat(), textPaint)
            statusY += lineHeight
        }
        currentY += statusHeight + cardSpacing
        
        // Confianza
        val confidenceText = "Confianza: ${(resultado.confidence * 100).toInt()}%"
        val confidencePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FF9800")
            isAntiAlias = true
        }
        canvas.drawRoundRect(padding.toFloat(), currentY.toFloat(), (width - padding).toFloat(), (currentY + 60).toFloat(), 12f, 12f, confidencePaint)
        canvas.drawText(confidenceText, (padding + 20).toFloat(), currentY + 40f, textPaint)
        currentY += 80 + cardSpacing
        
        // Recomendaciones
        val recPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E3F2FD")
            isAntiAlias = true
        }
        val recRectHeight = recommendationsHeight.toInt()
        canvas.drawRoundRect(padding.toFloat(), currentY.toFloat(), (width - padding).toFloat(), (currentY + recRectHeight).toFloat(), 12f, 12f, recPaint)
        
        val recTitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1976D2")
            textSize = 36f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RECOMENDACIONES", (padding + 20).toFloat(), currentY + 40f, recTitlePaint)
        
        // Usar recTextPaint y maxTextWidth ya calculados arriba
        var recY = currentY + 70
        resultado.preventionMeasures.take(5).forEach { measure ->
            val fullText = "• $measure"
            val lines = breakTextIntoLines(recTextPaint, fullText, maxTextWidth)
            lines.forEach { line ->
                canvas.drawText(line, (padding + 20).toFloat(), recY.toFloat(), recTextPaint)
                recY += 32 // Espaciado entre líneas
            }
            recY += 8 // Espacio adicional entre recomendaciones
        }
        currentY += recRectHeight + cardSpacing
        
        // Información adicional
        val infoPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F3E5F5")
            isAntiAlias = true
        }
        canvas.drawRoundRect(padding.toFloat(), currentY.toFloat(), (width - padding).toFloat(), (currentY + 80).toFloat(), 12f, 12f, infoPaint)
        
        val infoTitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#7B1FA2")
            textSize = 28f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Análisis Real con IA", (padding + 20).toFloat(), currentY + 30f, infoTitlePaint)
        
        val infoTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#424242")
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText("TensorFlow Lite - Detección de enfermedades en tomate", (padding + 20).toFloat(), currentY + 60f, infoTextPaint)
        currentY += 100 + cardSpacing
        
        // Footer con botones
        val footerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F5F5F5")
            isAntiAlias = true
        }
        canvas.drawRoundRect(padding.toFloat(), currentY.toFloat(), (width - padding).toFloat(), (currentY + 100).toFloat(), 16f, 16f, footerPaint)
        
        val footerTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1976D2")
            textSize = 32f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Capturar Análisis", (width / 2 - 200).toFloat(), currentY + 40f, footerTextPaint)
        
        val buttonPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1976D2")
            isAntiAlias = true
        }
        canvas.drawRoundRect((width / 2 - 150).toFloat(), currentY + 50f, (width / 2 - 50).toFloat(), currentY + 90f, 8f, 8f, buttonPaint)
        canvas.drawRoundRect((width / 2 + 50).toFloat(), currentY + 50f, (width / 2 + 150).toFloat(), currentY + 90f, 8f, 8f, buttonPaint)
        
        val buttonTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText("Guardar", (width / 2 - 120).toFloat(), currentY + 75f, buttonTextPaint)
        canvas.drawText("Compartir", (width / 2 + 70).toFloat(), currentY + 75f, buttonTextPaint)
        
        bitmap
    } catch (e: Exception) {
        Log.e("AnalysisScreenshot", "Error creando captura: ${e.message}")
        null
    }
}

// Funciones auxiliares

// Función para traducir nombres de enfermedades al español
fun traducirEnfermedad(enfermedad: String): String {
    return when (enfermedad) {
        "Tomato_Bacterial_spot" -> "Mancha bacteriana del tomate"
        "Tomato_Early_blight" -> "Tizón temprano del tomate"
        "Tomato_Late_blight" -> "Tizón tardío del tomate"
        "Tomato_Leaf_Mold" -> "Moho de las hojas del tomate"
        "Tomato_Septoria_leaf_spot" -> "Mancha foliar de Septoria"
        "Tomato_Spider_mites_Two_spotted_spider_mite" -> "Ácaros de dos manchas"
        "Tomato__Target_Spot" -> "Mancha objetivo del tomate"
        "Tomato__Tomato_YellowLeaf__Curl_Virus" -> "Virus del rizado amarillo de la hoja"
        "Tomato__Tomato_mosaic_virus" -> "Virus del mosaico del tomate"
        "Tomato_healthy" -> "Planta saludable"
        else -> enfermedad.replace("Tomato_", "").replace("_", " ")
    }
}
fun parseJson(jsonString: String?): List<Registro> {
    if (jsonString.isNullOrBlank()) {
        Log.w("parseJson", "JSON string es nulo o vacío.")
        return emptyList()
    }
    val list = mutableListOf<Registro>()
    try {
        try {
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val temp = jsonObject.optString("temperatura", "N/A")
                val hum = jsonObject.optString("humedad", "N/A")
                val lum = jsonObject.optString("luminosidad", "N/A")
                list.add(Registro(temp, hum, lum))
            }
        } catch (e: org.json.JSONException) {
            val jsonObject = org.json.JSONObject(jsonString)
            if (jsonObject.has("datos")) {
                val jsonArray = jsonObject.getJSONArray("datos")
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val temp = item.optString("temperatura", "N/A")
                    val hum = item.optString("humedad", "N/A")
                    val lum = item.optString("luminosidad", "N/A")
                    list.add(Registro(temp, hum, lum))
                }
            } else {
                val temp = jsonObject.optString("temperatura", "N/A")
                val hum = jsonObject.optString("humedad", "N/A")
                val lum = jsonObject.optString("luminosidad", "N/A")
                list.add(Registro(temp, hum, lum))
            }
        }
    } catch (e: org.json.JSONException) {
        Log.e("parseJson", "Error al parsear JSON: ${e.localizedMessage}")
        Log.e("parseJson", "JSON recibido: $jsonString")
    } catch (e: Exception) {
        Log.e("parseJson", "Error inesperado al parsear JSON: ${e.localizedMessage}")
    }
    return list
}

fun getFotos(directory: File): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
        Log.w("getFotos", "El directorio no existe o no es un directorio: ${directory.absolutePath}")
        return emptyList()
    }
    return directory.listFiles { file ->
        file.isFile && (file.extension.equals("jpg", ignoreCase = true) || file.extension.equals("jpeg", ignoreCase = true))
    }?.sortedDescending()?.toList() ?: emptyList<File>().also {
        Log.d("getFotos", "No se encontraron archivos jpg/jpeg en ${directory.absolutePath}")
    }
}

fun getFotosNormales(directory: File): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }
    return directory.listFiles { file ->
        file.isFile && 
        (file.extension.equals("jpg", ignoreCase = true) || file.extension.equals("jpeg", ignoreCase = true)) &&
        !file.name.startsWith("ANALISIS_")
    }?.sortedDescending()?.toList() ?: emptyList()
}

fun getFotosAnalisis(directory: File): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }
    return directory.listFiles { file ->
        file.isFile && 
        (file.extension.equals("jpg", ignoreCase = true) || file.extension.equals("jpeg", ignoreCase = true)) &&
        file.name.startsWith("ANALISIS_")
    }?.sortedDescending()?.toList() ?: emptyList()
}

// Función para analizar planta enviando foto al servidor
suspend fun analizarPlanta(file: File): String? {
    return try {
        withContext(Dispatchers.IO) {
            val url = java.net.URL("http://192.168.1.100:5000/analizar_planta")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "image/jpeg")
            
            file.inputStream().use { input ->
                connection.outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                connection.inputStream.bufferedReader().readText()
            } else {
                null
            }
        }
    } catch (e: Exception) {
        Log.e("analizarPlanta", "Error al analizar planta", e)
        null
    }
}

// Funciones de análisis de imagen
fun analyzeImageQuality(bitmap: Bitmap): ImageAnalysis {
    val issues = mutableListOf<String>()
    val suggestions = mutableListOf<String>()
    
    // Análisis de iluminación
    val brightness = calculateBrightness(bitmap)
    when {
        brightness < 0.3 -> {
            issues.add("Imagen muy oscura")
            suggestions.add("Mejora la iluminación o activa el flash")
        }
        brightness > 0.8 -> {
            issues.add("Imagen muy brillante")
            suggestions.add("Reduce la luz o ajusta el flash")
        }
    }
    
    // Análisis de contraste
    val contrast = calculateContrast(bitmap)
    if (contrast < 0.2) {
        issues.add("Poco contraste")
        suggestions.add("Mejora la iluminación o el enfoque")
    }
    
    // Análisis de enfoque (muy poco estricto para 256x256)
    val sharpness = calculateSharpness(bitmap)
    if (sharpness < 0.02) { // Reducido de 0.05 a 0.02
        issues.add("Imagen desenfocada")
        suggestions.add("Mantén la cámara estable y enfoca mejor")
    }
    
    // Análisis de ruido
    val noise = calculateNoise(bitmap)
    if (noise > 0.4) {
        issues.add("Mucho ruido")
        suggestions.add("Mejora la iluminación")
    }
    
    val isGood = issues.isEmpty()
    if (isGood) {
        suggestions.add("¡Excelente foto! Buena calidad para análisis")
    }
    
    return ImageAnalysis(isGood, issues, suggestions)
}

fun calculateBrightness(bitmap: Bitmap): Float {
    var totalBrightness = 0f
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    
    for (pixel in pixels) {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        totalBrightness += (r + g + b) / 3f / 255f
    }
    
    return totalBrightness / pixels.size
}

fun calculateContrast(bitmap: Bitmap): Float {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    
    val brightnesses = pixels.map { pixel ->
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        (r + g + b) / 3f
    }
    
    val mean = brightnesses.average().toFloat()
    val variance = brightnesses.map { (it - mean) * (it - mean) }.average().toFloat()
    val stdDev = kotlin.math.sqrt(variance)
    
    return (stdDev / 255f).coerceIn(0f, 1f)
}

fun calculateSharpness(bitmap: Bitmap): Float {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    
    var edgeSum = 0f
    for (y in 1 until bitmap.height - 1) {
        for (x in 1 until bitmap.width - 1) {
            val center = pixels[y * bitmap.width + x]
            val right = pixels[y * bitmap.width + (x + 1)]
            val bottom = pixels[(y + 1) * bitmap.width + x]
            
            val centerBrightness = ((center shr 16) and 0xFF + (center shr 8) and 0xFF + center and 0xFF) / 3f
            val rightBrightness = ((right shr 16) and 0xFF + (right shr 8) and 0xFF + right and 0xFF) / 3f
            val bottomBrightness = ((bottom shr 16) and 0xFF + (bottom shr 8) and 0xFF + bottom and 0xFF) / 3f
            
            edgeSum += kotlin.math.abs(centerBrightness - rightBrightness) + kotlin.math.abs(centerBrightness - bottomBrightness)
        }
    }
    
    return (edgeSum / (bitmap.width * bitmap.height * 255f)).coerceIn(0f, 1f)
}

fun calculateNoise(bitmap: Bitmap): Float {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    
    var noiseSum = 0f
    for (y in 1 until bitmap.height - 1) {
        for (x in 1 until bitmap.width - 1) {
            val center = pixels[y * bitmap.width + x]
            val neighbors = listOf(
                pixels[y * bitmap.width + (x - 1)],
                pixels[y * bitmap.width + (x + 1)],
                pixels[(y - 1) * bitmap.width + x],
                pixels[(y + 1) * bitmap.width + x]
            )
            
            val centerBrightness = ((center shr 16) and 0xFF + (center shr 8) and 0xFF + center and 0xFF) / 3f
            val neighborBrightness = neighbors.map { pixel ->
                ((pixel shr 16) and 0xFF + (pixel shr 8) and 0xFF + pixel and 0xFF) / 3f
            }.average().toFloat()
            
            noiseSum += kotlin.math.abs(centerBrightness - neighborBrightness)
        }
    }
    
    return (noiseSum / (bitmap.width * bitmap.height * 255f)).coerceIn(0f, 1f)
}



@Composable
fun RealAnalysisScreen(progress: Float, message: String, imagen: File?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.surface
            )
            // Bloquear todas las interacciones táctiles
            .pointerInput(Unit) {
                detectDragGestures { _, _ -> 
                    // No hacer nada - bloquear todos los gestos
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono de análisis real
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = "Análisis Real",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Título
            Text(
                "Análisis Real con IA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mensaje de estado
            Text(
                message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Barra de progreso
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(6.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    // Barra de progreso animada
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                                    )
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Porcentaje
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Imagen que se está analizando (si existe)
            if (imagen != null) {
                val bitmap = remember(imagen.path) {
                    try {
                        BitmapFactory.decodeFile(imagen.absolutePath)?.asImageBitmap()
                    } catch (e: Exception) {
                        Log.e("RealAnalysisScreen", "Error cargando bitmap", e)
                        null
                    }
                }
                
                if (bitmap != null) {
                    Card(
                        modifier = Modifier
                            .size(200.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Imagen en análisis real",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnfermedadScreen(progress: Float, message: String, imagen: File?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.surface
            )
            // Bloquear todas las interacciones táctiles
            .pointerInput(Unit) {
                detectDragGestures { _, _ -> 
                    // No hacer nada - bloquear todos los gestos
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono de análisis (mismo que la pantalla normal)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = "Análisis",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Título (mismo que la pantalla normal)
            Text(
                "Analizando Planta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mensaje de estado (mismo que la pantalla normal)
            Text(
                message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Barra de progreso (mismo que la pantalla normal)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(6.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    // Barra de progreso animada (mismo que la pantalla normal)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Porcentaje (mismo que la pantalla normal)
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Imagen que se está analizando (si existe)
            if (imagen != null) {
                val bitmap = remember(imagen.path) {
                    try {
                        BitmapFactory.decodeFile(imagen.absolutePath)?.asImageBitmap()
                    } catch (e: Exception) {
                        Log.e("EnfermedadScreen", "Error cargando bitmap", e)
                        null
                    }
                }
                
                if (bitmap != null) {
                    Card(
                        modifier = Modifier
                            .size(200.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Imagen en análisis",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun ResultadosRealAnalisisScreen(
    imagen: File?, 
    resultado: DiseasePrediction, 
    onBack: () -> Unit,
    onGuardarCaptura: () -> Unit,
    onCompartirCaptura: () -> Unit,
    isCapturing: Boolean = false
) {
    val view = LocalView.current
    val density = LocalDensity.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con botón de regreso
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "RESULTADOS DEL ANÁLISIS REAL",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            // Imagen analizada
            item {
                if (imagen != null) {
                    val bitmap = remember(imagen.path) {
                        try {
                            BitmapFactory.decodeFile(imagen.absolutePath)?.asImageBitmap()
                        } catch (e: Exception) {
                            Log.e("ResultadosRealAnalisisScreen", "Error cargando bitmap", e)
                            null
                        }
                    }
                    
                    if (bitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Imagen analizada",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            
            // Estado de la planta (basado en si es healthy o no)
            item {
                val isHealthy = resultado.prediction == "Tomato_healthy"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHealthy) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = if (isHealthy) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (isHealthy) "Estado de la planta: Saludable" else "Enfermedad detectada: ${traducirEnfermedad(resultado.prediction)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isHealthy) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Nivel de confianza
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Confianza: ${(resultado.confidence * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            // Sección de recomendaciones
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "RECOMENDACIONES",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (resultado.prediction == "Tomato_healthy") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Lista de recomendaciones
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            resultado.preventionMeasures.forEach { measure ->
                                RecommendationItem(
                                    text = measure,
                                    iconColor = if (resultado.prediction == "Tomato_healthy") 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            
            // Información adicional del análisis real
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Análisis Real con IA",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Este análisis fue realizado usando un modelo de TensorFlow Lite entrenado específicamente para detectar enfermedades en plantas de tomate.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Botones de captura
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Capturar Análisis",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Botón de guardar captura
                            Button(
                                onClick = onGuardarCaptura,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isCapturing
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isCapturing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Capturando...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = "Guardar",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Guardar",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            // Botón de compartir captura
                            Button(
                                onClick = onCompartirCaptura,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isCapturing
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isCapturing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Capturando...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Compartir",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Compartir",
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
            
            // Espaciador final
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ResultadosEnfermedadScreen(imagen: File?, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con botón de regreso
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "RESULTADOS DEL ANÁLISIS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Imagen analizada
            item {
                if (imagen != null) {
                    val bitmap = remember(imagen.path) {
                        try {
                            BitmapFactory.decodeFile(imagen.absolutePath)?.asImageBitmap()
                        } catch (e: Exception) {
                            Log.e("ResultadosEnfermedadScreen", "Error cargando bitmap", e)
                            null
                        }
                    }
                    
                    if (bitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Imagen analizada",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            
            // Estado de la planta (70% de salud)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Estado de la planta: 70% de salud",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Enfermedad detectada
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Enfermedad detectada: Mancha bacteriana del tomate",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            // Nivel de confianza
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Confianza: 75%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            // Sección de recomendaciones
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "RECOMENDACIONES",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Lista de recomendaciones
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RecommendationItem(
                                text = "Prevenir la mancha bacteriana usando semillas que estén libres de la enfermedad.",
                                iconColor = MaterialTheme.colorScheme.error
                            )
                            RecommendationItem(
                                text = "Implementar la rotación de cultivos para reducir la prevalencia de la enfermedad.",
                                iconColor = MaterialTheme.colorScheme.error
                            )
                            RecommendationItem(
                                text = "Aplicar fungicidas a base de cobre para controlar la enfermedad.",
                                iconColor = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // Espaciador final
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Funciones auxiliares

