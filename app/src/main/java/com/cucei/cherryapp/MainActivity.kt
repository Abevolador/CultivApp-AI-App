package com.cucei.cherryapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.window.DialogProperties
import com.cucei.cherryapp.ui.CameraScreen
import com.cucei.cherryapp.ui.PlantDataScreen
import com.cucei.cherryapp.ui.ChartsScreen
import com.cucei.cherryapp.viewmodel.PlantDataViewModel
import com.cucei.cherryapp.ui.theme.WhiteButton
import com.cucei.cherryapp.ui.theme.BlackText
import com.cucei.cherryapp.ui.theme.DarkButton
import com.cucei.cherryapp.ui.theme.WhiteText

// Sealed class Pantalla actualizada
sealed class Pantalla {
    object SplashScreen : Pantalla()
    object Bienvenida : Pantalla()
    object Inicio : Pantalla()
    object MostrarDatos : Pantalla()
    object Galeria : Pantalla()
    object Camara : Pantalla()
    object AnalisisPlanta : Pantalla()
    object DatosPlantas : Pantalla()
    object Graficos : Pantalla()
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
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CherryApp() {
    val context = LocalContext.current
    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.SplashScreen) }
    var registros by remember { mutableStateOf<List<Registro>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var fotos by remember { mutableStateOf<List<File>>(emptyList()) }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    val FOTOS_DIR = remember { File(context.filesDir, "CherryFotos").apply { mkdirs() } }

    var showGuardarDialog by remember { mutableStateOf<File?>(null) }
    var imagenSeleccionada by remember { mutableStateOf<File?>(null) }
    var showConfirmDelete by remember { mutableStateOf<File?>(null) }
    var showSendDialog by remember { mutableStateOf<File?>(null) }
    var showCamaraDialog by remember { mutableStateOf(false) }
    var showCamaraPersonalizada by remember { mutableStateOf(false) }
    var showVistaPrevia by remember { mutableStateOf<File?>(null) }
    var showResultadosAnalisis by remember { mutableStateOf<String?>(null) }
    var analizandoPlanta by remember { mutableStateOf(false) }
    var currentPhotoIndex by remember { mutableStateOf(0) }
    var rotationAngle by remember { mutableStateOf(0f) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var showAnalysisDetails by remember { mutableStateOf(false) }
    
    // Nuevas variables para la pantalla de análisis con barra de carga
    var showAnalisisScreen by remember { mutableStateOf(false) }
    var analisisProgress by remember { mutableStateOf(0f) }
    var analisisMessage by remember { mutableStateOf("Iniciando análisis...") }
    var imagenAnalizada by remember { mutableStateOf<File?>(null) }
    
    // Variables para la pantalla de análisis de enfermedades (presión larga)
    var showEnfermedadScreen by remember { mutableStateOf(false) }
    var enfermedadProgress by remember { mutableStateOf(0f) }
    var enfermedadMessage by remember { mutableStateOf("Iniciando detección...") }
    var imagenEnfermedad by remember { mutableStateOf<File?>(null) }
    
    // Variables para doble toque
    var lastTapTime by remember { mutableStateOf(0L) }
    var isDoubleTap by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // Variables para análisis real con TensorFlow Lite
    var showRealAnalysisScreen by remember { mutableStateOf(false) }
    var realAnalysisProgress by remember { mutableStateOf(0f) }
    var realAnalysisMessage by remember { mutableStateOf("Iniciando análisis...") }
    var imagenRealAnalysis by remember { mutableStateOf<File?>(null) }
    var realAnalysisResult by remember { mutableStateOf<DiseasePrediction?>(null) }
    

    

    
    // Navigation Drawer state
    var drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var scope = rememberCoroutineScope()
    
    // ViewModel para datos de plantas
    val plantDataViewModel = remember { PlantDataViewModel() }
    
    // TensorFlow Lite interpreter
    val tensorFlowInterpreter = remember { loadTensorFlowModel(context) }

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
            DrawerItem(
                title = "Cargar Datos (JSON)",
                icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                screen = Pantalla.MostrarDatos,
                description = "Carga y visualiza tus registros de plantas"
            ),
            DrawerItem(
                title = "Datos de Plantas",
                icon = { Icon(Icons.Filled.Analytics, contentDescription = null) },
                screen = Pantalla.DatosPlantas,
                description = "Gestiona y visualiza datos de plantas"
            )
        )
    }

    // --- MANEJO DEL BOTÓN ATRÁS ---
    BackHandler(enabled = pantalla != Pantalla.Bienvenida) {
        Log.d("BackHandler", "Principal: Pantalla actual: $pantalla")
        when (pantalla) {
            Pantalla.Bienvenida -> {
                // No hacer nada, ya estamos en bienvenida
            }
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
            Pantalla.Galeria, Pantalla.MostrarDatos, Pantalla.DatosPlantas, Pantalla.Graficos -> {
                Log.d("BackHandler", "Principal: Volviendo a Inicio desde $pantalla")
                pantalla = Pantalla.Inicio
            }
            Pantalla.AnalisisPlanta -> {
                Log.d("BackHandler", "Principal: Volviendo a Inicio desde Análisis de Plantas")
                pantalla = Pantalla.Inicio
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
            // Regresar a la vista previa de la foto
            if (imagenEnfermedad != null) {
                showVistaPrevia = imagenEnfermedad
                imagenEnfermedad = null
            } else if (imagenAnalizada != null) {
                showVistaPrevia = imagenAnalizada
                imagenAnalizada = null
            }
        }
    }

    // BackHandler para la pantalla de análisis con barra de carga
    if (showAnalisisScreen) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Cerrando pantalla de análisis")
            showAnalisisScreen = false
            analisisProgress = 0f
            analisisMessage = "Iniciando análisis..."
            imagenAnalizada = null
        }
    }

    // BackHandler para la pantalla de análisis de enfermedades
    if (showEnfermedadScreen) {
        BackHandler(enabled = true) {
            Log.d("BackHandler", "Cerrando pantalla de análisis de enfermedades")
            showEnfermedadScreen = false
            enfermedadProgress = 0f
            enfermedadMessage = "Iniciando detección..."
            imagenEnfermedad = null
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
            showGuardarDialog = file
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
    val pickJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().readText()
                    registros = parseJson(json)
                    pantalla = Pantalla.MostrarDatos
                }
            } catch (e: Exception) {
                coroutineScope.launch { snackbarHostState.showSnackbar("Error al abrir el archivo: ${e.localizedMessage}") }
                Log.e("CherryApp", "Error al abrir JSON", e)
            }
        }
    }

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
                                    fotos = getFotos(FOTOS_DIR)
                                    pantalla = item.screen
                                }
                                Pantalla.MostrarDatos -> {
                                    pickJsonLauncher.launch("application/json")
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
                                Pantalla.Bienvenida -> "CultivApp"
                                Pantalla.Inicio -> "Inicio"
                                Pantalla.AnalisisPlanta -> "Análisis de Plantas"
                                Pantalla.Galeria -> "Galería"
                                Pantalla.MostrarDatos -> "Datos JSON"
                                Pantalla.DatosPlantas -> "Datos de Plantas"
                                Pantalla.Graficos -> "Gráficos"
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
                            // Botón invisible para análisis real con TensorFlow Lite
                            IconButton(
                                onClick = {
                                    // Preparar la imagen para análisis real
                                    val tempFile = File(context.cacheDir, "temp_real_analysis_${System.currentTimeMillis()}.jpg")
                                    try {
                                        val originalBitmap = BitmapFactory.decodeFile(showVistaPrevia!!.absolutePath)
                                        if (originalBitmap != null) {
                                            val matrix = Matrix()
                                            matrix.postRotate(rotationAngle)
                                            val rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                                            
                                            val outputStream = tempFile.outputStream()
                                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                                            outputStream.close()
                                            
                                            originalBitmap.recycle()
                                            rotatedBitmap.recycle()
                                            
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
                                modifier = Modifier.size(40.dp)
                            ) {
                                // Botón invisible pero presionable
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            color = androidx.compose.ui.graphics.Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }
                            
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

                // Pantalla de Bienvenida
                if (pantalla == Pantalla.Bienvenida) {
                    WelcomeScreen(
                        onStartApp = {
                            pantalla = Pantalla.Inicio
                        }
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
                                Text("📸 Tomar Foto: Captura imágenes con la cámara")
                                Text("🖼️ Ver Galería: Visualiza las fotos que has tomado")
                                Text("📂 Cargar Datos (JSON): Carga y visualiza tus registros")
                                Text("📊 Datos de Plantas: Gestiona datos de plantas y gráficos")
                            }
                        }
                    }
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

                // Pantalla de datos de plantas
                if (pantalla == Pantalla.DatosPlantas && !showCamaraPersonalizada && imagenSeleccionada == null && showVistaPrevia == null && showResultadosAnalisis == null) {
                    PlantDataScreen(
                        onBack = { pantalla = Pantalla.Inicio },
                        viewModel = plantDataViewModel,
                        onNavigateToCharts = { pantalla = Pantalla.Graficos }
                    )
                }

                // Pantalla de gráficos
                if (pantalla == Pantalla.Graficos && !showCamaraPersonalizada && imagenSeleccionada == null && showVistaPrevia == null && showResultadosAnalisis == null) {
                    ChartsScreen(
                        onBack = { pantalla = Pantalla.DatosPlantas },
                        viewModel = plantDataViewModel
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
                                    fotos = getFotos(FOTOS_DIR)
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
                                "• Enfoca el objeto dentro del recuadro\n" +
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

                // Mostrar datos JSON
                if (pantalla == Pantalla.MostrarDatos && !showCamaraPersonalizada && imagenSeleccionada == null) {
                    Column(Modifier.fillMaxSize()) {
                        if (registros.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No hay datos para mostrar.", fontSize = 18.sp)
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { pickJsonLauncher.launch("application/json") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSystemInDarkTheme()) DarkButton else WhiteButton,
                                            contentColor = if (isSystemInDarkTheme()) WhiteText else BlackText
                                        )
                                    ) {
                                        Text("Cargar archivo JSON")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)) {
                                items(registros.size) { i ->
                                    val reg = registros[i]
                                    Card(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text("Registro ${i + 1}", fontWeight = FontWeight.Bold)
                                            Text("🌡️ Temperatura: ${reg.temperatura}")
                                            Text("💧 Humedad: ${reg.humedad}")
                                            Text("🔆 Luminosidad: ${reg.luminosidad}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                            // Galería (rediseñada con estilo moderno)
            if (pantalla == Pantalla.Galeria && !showCamaraPersonalizada && imagenSeleccionada == null) {
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
                                        "No hay fotos tomadas",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Usa la cámara para añadir algunas fotos",
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
                                        analizandoPlanta = true
                                        coroutineScope.launch {
                                            val resultado = analizarPlanta(file)
                                            analizandoPlanta = false
                                            if (resultado != null) {
                                                showResultadosAnalisis = resultado
                                                imagenSeleccionada = null
                                            } else {
                                                coroutineScope.launch { 
                                                    snackbarHostState.showSnackbar("No se pudo conectar al servidor para el análisis") 
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    if (analizandoPlanta) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Send, 
                                            contentDescription = "Enviar a Analizar", 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
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
                                                
                                                fotos = getFotos(FOTOS_DIR)
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
                                            val currentTime = System.currentTimeMillis()
                                            val timeDiff = currentTime - lastTapTime
                                            
                                            if (timeDiff < 500) {
                                                // Doble toque - Análisis de enfermedad
                                                isDoubleTap = true
                                                val tempFile = File(context.cacheDir, "temp_enfermedad_${System.currentTimeMillis()}.jpg")
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
                                                        
                                                        // Mostrar pantalla de análisis de enfermedades
                                                        imagenEnfermedad = tempFile
                                                        showEnfermedadScreen = true
                                                            showVistaPrevia = null
                                                            rotationAngle = 0f
                                                            imageAnalysis = null
                                                            showAnalysisDetails = false
                                                        
                                                        // Iniciar simulación de análisis de enfermedades (5 segundos)
                                                            coroutineScope.launch { 
                                                            enfermedadProgress = 0f
                                                            enfermedadMessage = "Iniciando análisis..."
                                                            
                                                            // Simular diferentes etapas del análisis (5 segundos total)
                                                            val etapas = listOf(
                                                                "Preparando imagen..." to 0.2f,
                                                                "Analizando características..." to 0.4f,
                                                                "Procesando con IA..." to 0.6f,
                                                                "Evaluando salud de la planta..." to 0.8f,
                                                                "Generando recomendaciones..." to 1.0f
                                                            )
                                                            
                                                            etapas.forEach { (mensaje, progreso) ->
                                                                enfermedadMessage = mensaje
                                                                delay(1000) // 1000ms por etapa = 5 segundos total
                                                                enfermedadProgress = progreso
                                                            }
                                                            
                                                            delay(500) // Pausa final
                                                            showEnfermedadScreen = false
                                                            showResultadosAnalisis = "Detección completada"
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    coroutineScope.launch { 
                                                        snackbarHostState.showSnackbar("Error al procesar la imagen: ${e.message}") 
                                                    }
                                                }
                                            } else {
                                                // Presión normal - Análisis normal (con retraso para permitir doble toque)
                                                coroutineScope.launch {
                                                    delay(300) // Esperar 300ms para ver si viene un segundo toque
                                                    
                                                    // Verificar si ha habido otro toque en este tiempo
                                                    val timeSinceLastTap = System.currentTimeMillis() - currentTime
                                                    if (timeSinceLastTap >= 300) {
                                                        // No ha habido doble toque, ejecutar análisis normal
                                                        isDoubleTap = false
                                                        val tempFile = File(context.cacheDir, "temp_rotated_${System.currentTimeMillis()}.jpg")
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
                                                                
                                                                // Mostrar pantalla de análisis con barra de carga
                                                                imagenAnalizada = tempFile
                                                                showAnalisisScreen = true
                                                            showVistaPrevia = null
                                                            rotationAngle = 0f
                                                            imageAnalysis = null
                                                            showAnalysisDetails = false
                                                                
                                                                // Iniciar simulación de análisis
                                                                coroutineScope.launch {
                                                                    analisisProgress = 0f
                                                                    analisisMessage = "Iniciando análisis..."
                                                                    
                                                                    // Simular diferentes etapas del análisis
                                                                    val etapas = listOf(
                                                                        "Preparando imagen..." to 0.2f,
                                                                        "Analizando características..." to 0.4f,
                                                                        "Procesando con IA..." to 0.6f,
                                                                        "Evaluando salud de la planta..." to 0.8f,
                                                                        "Generando recomendaciones..." to 1.0f
                                                                    )
                                                                    
                                                                    etapas.forEach { (mensaje, progreso) ->
                                                                        analisisMessage = mensaje
                                                                        delay(600) // 600ms por etapa = 3 segundos total
                                                                        analisisProgress = progreso
                                                                    }
                                                                    
                                                                    delay(500) // Pausa final
                                                                    showAnalisisScreen = false
                                                                    showResultadosAnalisis = "Análisis completado"
                                                                }
                                                    }
                                                } catch (e: Exception) {
                                                    coroutineScope.launch { 
                                                        snackbarHostState.showSnackbar("Error al procesar la imagen: ${e.message}") 
                                                    }
                                                }
                                            }
                                                }
                                            }
                                            
                                            lastTapTime = currentTime
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

                // Pantalla de análisis con barra de carga
                if (showAnalisisScreen) {
                    AnalisisScreen(
                        progress = analisisProgress,
                        message = analisisMessage,
                        imagen = imagenAnalizada
                    )
                }

                // Pantalla de análisis de enfermedades con barra de carga
                if (showEnfermedadScreen) {
                    EnfermedadScreen(
                        progress = enfermedadProgress,
                        message = enfermedadMessage,
                        imagen = imagenEnfermedad
                    )
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
                                // Regresar a la vista previa de la foto
                                showVistaPrevia = imagenRealAnalysis
                                imagenRealAnalysis = null
                                realAnalysisResult = null
                            }
                        )
                    } else if (imagenEnfermedad != null) {
                        ResultadosEnfermedadScreen(
                            imagen = imagenEnfermedad,
                            onBack = { 
                                showResultadosAnalisis = null
                                // Regresar a la vista previa de la foto
                                showVistaPrevia = imagenEnfermedad
                                imagenEnfermedad = null
                            }
                        )
                    } else {
                        ResultadosAnalisisScreen(
                            imagen = imagenAnalizada,
                            onBack = { 
                                showResultadosAnalisis = null
                                // Regresar a la vista previa de la foto
                                showVistaPrevia = imagenAnalizada
                                imagenAnalizada = null
                            }
                        )
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

                if (showConfirmDelete != null) {
                    val fileToDelete = showConfirmDelete!!
                    AlertDialog(
                        onDismissRequest = { showConfirmDelete = null },
                        title = { Text("Confirmar Borrado") },
                        text = { Text("¿Seguro que quieres borrar esta foto?") },
                        confirmButton = {
                            Button(onClick = {
                                if (fileToDelete.delete()) {
                                    fotos = getFotos(FOTOS_DIR)
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

                if (showCamaraDialog) {
                    AlertDialog(
                        onDismissRequest = { showCamaraDialog = false },
                        title = { Text("Seleccionar Cámara") },
                        text = { Text("¿Qué cámara deseas utilizar?") },
                        confirmButton = {
                            Button(onClick = {
                                showCamaraDialog = false
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    showCamaraPersonalizada = true
                                } else {
                                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }) { Text("Personalizada") }
                        },
                        dismissButton = {
                            Button(onClick = {
                                showCamaraDialog = false
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    abrirCamaraNativa()
                                } else {
                                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }) { Text("Nativa del Sistema") }
                        },
                        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
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
    return try {
        Log.d("TensorFlow", "Iniciando carga del modelo...")
        
        val modelFile = File(context.getExternalFilesDir(null), "tomato.tflite")
        Log.d("TensorFlow", "Ruta del modelo: ${modelFile.absolutePath}")
        
        if (!modelFile.exists()) {
            Log.d("TensorFlow", "Modelo no existe, copiando desde assets...")
            // Copiar desde assets si no existe
            context.assets.open("tomato.tflite").use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("TensorFlow", "Modelo copiado exitosamente")
        } else {
            Log.d("TensorFlow", "Modelo ya existe en: ${modelFile.absolutePath}")
        }
        
        val options = Interpreter.Options()
        options.setNumThreads(4) // Usar 4 hilos para mejor rendimiento
        
        val interpreter = Interpreter(modelFile, options)
        Log.d("TensorFlow", "Modelo cargado exitosamente!")
        interpreter
    } catch (e: Exception) {
        Log.e("TensorFlow", "Error cargando modelo: ${e.message}")
        e.printStackTrace()
        null
    }
}



fun analyzeImageWithTensorFlow(interpreter: Interpreter, bitmap: Bitmap): DiseasePrediction? {
    return try {
        Log.d("TensorFlow", "Iniciando análisis de imagen...")
        Log.d("TensorFlow", "Tamaño original de imagen: ${bitmap.width}x${bitmap.height}")
        
        // Redimensionar imagen a 256x256
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
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
        val pixels = IntArray(256 * 256)
        resizedBitmap.getPixels(pixels, 0, 256, 0, 0, 256, 256)
        
        val floatArray = FloatArray(256 * 256 * 3)
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
                "Prevén la mancha bacteriana usando semillas libres de la enfermedad.",
                "Implementa la rotación de cultivos para reducir la prevalencia de la enfermedad.",
                "Aplica fungicidas a base de cobre para controlar la enfermedad."
            )
            "Tomato_Early_blight" -> listOf(
                "Prevén el tizón temprano practicando una buena higiene del jardín.",
                "Asegúrate de un riego adecuado para evitar salpicar tierra en las hojas.",
                "Aplica fungicidas según sea necesario para controlar la enfermedad."
            )
            "Tomato_Late_blight" -> listOf(
                "Prevén el tizón tardío proporcionando buena circulación de aire en tu jardín o invernadero.",
                "Evita el riego por aspersión, ya que las hojas húmedas pueden favorecer la enfermedad.",
                "Aplica fungicidas cuando sea necesario para manejar la enfermedad."
            )
            "Tomato_Leaf_Mold" -> listOf(
                "Prevén el moho de las hojas asegurando buena circulación de aire y espaciado entre plantas.",
                "Evita mojar las hojas al regar, y riega el suelo en su lugar.",
                "Aplica fungicidas si la enfermedad está presente y empeorando."
            )
            "Tomato_Septoria_leaf_spot" -> listOf(
                "Prevén la mancha foliar de Septoria manteniendo una buena higiene del jardín.",
                "Evita el riego por aspersión para mantener las hojas secas.",
                "Aplica fungicidas si la enfermedad se convierte en un problema."
            )
            "Tomato_Spider_mites_Two_spotted_spider_mite" -> listOf(
                "Prevén las infestaciones de ácaros inspeccionando regularmente tus plantas en busca de signos de infestación.",
                "Aumenta la humedad en el área de cultivo para desalentar los ácaros.",
                "Usa jabón insecticida o aceite de neem para controlar los ácaros si es necesario."
            )
            "Tomato__Target_Spot" -> listOf(
                "Prevén la mancha objetivo asegurando buena circulación de aire y evitando el hacinamiento de plantas.",
                "Riega en la base de las plantas, manteniendo las hojas secas.",
                "Aplica fungicidas según sea necesario para controlar la enfermedad."
            )
            "Tomato__Tomato_YellowLeaf__Curl_Virus" -> listOf(
                "Prevén el Virus del Rizado Amarillo de la Hoja del Tomate usando plantas de tomate libres de virus.",
                "Controla las moscas blancas, que transmiten el virus, con insecticidas.",
                "Elimina y destruye las plantas infectadas para prevenir la propagación de la enfermedad."
            )
            "Tomato__Tomato_mosaic_virus" -> listOf(
                "Prevén el virus del mosaico del tomate usando semillas libres de virus y variedades de tomate resistentes a enfermedades.",
                "Controla los pulgones, que transmiten el virus, con insecticidas.",
                "Elimina y destruye las plantas infectadas para prevenir una mayor propagación."
            )
            "Tomato_healthy" -> listOf(
                "Si tu planta de tomate está saludable, continúa monitoreando regularmente plagas y enfermedades.",
                "Sigue buenas prácticas de jardinería, incluyendo riego, fertilización y mantenimiento adecuados."
            )
            else -> listOf("No hay medidas de prevención disponibles para esta condición.")
        }
        
        Log.d("TensorFlow", "Análisis completado exitosamente")
        DiseasePrediction(predictedClass, confidence, preventionMeasures)
        
    } catch (e: Exception) {
        Log.e("TensorFlow", "Error en análisis: ${e.message}")
        e.printStackTrace()
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
fun ResultadosRealAnalisisScreen(imagen: File?, resultado: DiseasePrediction, onBack: () -> Unit) {
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

