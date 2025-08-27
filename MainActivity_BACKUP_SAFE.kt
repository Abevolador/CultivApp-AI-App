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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

// Sealed class Pantalla actualizada
sealed class Pantalla {
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
    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Bienvenida) }
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
    
    // Navigation Drawer state
    var drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var scope = rememberCoroutineScope()
    
    // ViewModel para datos de plantas
    val plantDataViewModel = remember { PlantDataViewModel() }

    // Para el doble toque para salir y Snackbar
    var backPressedTime by remember { mutableStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val activity = (LocalContext.current as? Activity)

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
            ),
            DrawerItem(
                title = "Ver Gráficos",
                icon = { Icon(Icons.Filled.Analytics, contentDescription = null) },
                screen = Pantalla.Graficos,
                description = "Analiza las tendencias de tus datos"
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
            Pantalla.Galeria, Pantalla.MostrarDatos, Pantalla.AnalisisPlanta, Pantalla.DatosPlantas, Pantalla.Graficos -> {
                Log.d("BackHandler", "Principal: Volviendo a Inicio desde $pantalla")
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
            Log.d("BackHandler", "Cerrando resultados de análisis")
            showResultadosAnalisis = null
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

    // Navigation Drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "🍒 Cherry App",
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
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (pantalla != Pantalla.Bienvenida) {
                    TopAppBar(
                        title = {
                            Text(
                                when (pantalla) {
                                    Pantalla.Inicio -> "Inicio"
                                    Pantalla.AnalisisPlanta -> "Análisis de Plantas"
                                    Pantalla.Galeria -> "Galería"
                                    Pantalla.MostrarDatos -> "Datos JSON"
                                    Pantalla.DatosPlantas -> "Datos de Plantas"
                                    Pantalla.Graficos -> "Gráficos"
                                    else -> "Cherry App"
                                }
                            )
                        },
                        navigationIcon = {
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
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { contentPadding ->
            Box(modifier = Modifier.padding(contentPadding)) {

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
                            Text("🍒", fontSize = 48.sp)
                            Spacer(Modifier.width(16.dp))
                            Text("Cherry", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "¡Bienvenido/a a Cherry App!",
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
                                Text("📊 Datos de Plantas: Gestiona datos de plantas")
                                Text("📈 Ver Gráficos: Analiza las tendencias de tus datos")
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { 
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showCamaraPersonalizada = true
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText)
                        ) { Text("📸 Abrir Cámara Personalizada") }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            fotos = getFotos(FOTOS_DIR)
                            pantalla = Pantalla.Galeria
                        },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText)
                        ) { Text("🖼️ Ver Galería de la App") }
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
                                        colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText)
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

                // Galería
                if (pantalla == Pantalla.Galeria && !showCamaraPersonalizada && imagenSeleccionada == null) {
                    Column(Modifier.fillMaxSize()) {
                        if (fotos.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No hay fotos tomadas. Usa la cámara para añadir algunas.")
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                contentPadding = PaddingValues(4.dp)
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
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Foto ${idx + 1}",
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .padding(4.dp)
                                                .clickable { imagenSeleccionada = file }
                                        )
                                    } else {
                                        Box(modifier = Modifier
                                            .aspectRatio(1f)
                                            .padding(4.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                            Text("Error", Modifier.align(Alignment.Center))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Pantalla completa de imagen seleccionada
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
                            .clickable(onClick = { imagenSeleccionada = null })
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Imagen seleccionada",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Error al cargar la imagen", Modifier.align(Alignment.Center))
                        }
                        // Botones sobre la imagen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { imagenSeleccionada = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            Row {
                                IconButton(onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.provider", file))
                                        type = "image/jpeg"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.onBackground)
                                }
                                IconButton(onClick = {
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
                                }) {
                                    if (analizandoPlanta) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    } else {
                                        Icon(Icons.Default.Send, contentDescription = "Enviar a Analizar", tint = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                                IconButton(onClick = { showConfirmDelete = file }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    }
                }

                // Vista previa de foto
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
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Error al cargar la imagen", Modifier.align(Alignment.Center))
                        }
                        
                        // Botones de acción
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    val destFile = File(FOTOS_DIR, "IMG_${System.currentTimeMillis()}.jpg")
                                    try {
                                        file.copyTo(destFile, overwrite = true)
                                        fotos = getFotos(FOTOS_DIR)
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Foto guardada en galería de la app") }
                                        showVistaPrevia = null
                                    } catch (e: IOException) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Error al guardar foto") }
                                        Log.e("GuardarFoto", "Error al guardar foto en galería interna", e)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Guardar Foto")
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        showVistaPrevia = null
                                        showCamaraPersonalizada = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Eliminar y Reintentar")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        analizandoPlanta = true
                                        coroutineScope.launch {
                                            val resultado = analizarPlanta(file)
                                            analizandoPlanta = false
                                            if (resultado != null) {
                                                showResultadosAnalisis = resultado
                                                showVistaPrevia = null
                                            } else {
                                                coroutineScope.launch { 
                                                    snackbarHostState.showSnackbar("No se pudo conectar al servidor para el análisis") 
                                                }
                                                val destFile = File(FOTOS_DIR, "IMG_${System.currentTimeMillis()}.jpg")
                                                try {
                                                    file.copyTo(destFile, overwrite = true)
                                                    fotos = getFotos(FOTOS_DIR)
                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Foto guardada en galería de la app para análisis posterior") }
                                                } catch (e: IOException) {
                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Error al guardar foto") }
                                                }
                                                showVistaPrevia = null
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Mandar a Analizar")
                                }
                            }
                        }
                    }
                }

                // Resultados de análisis
                if (showResultadosAnalisis != null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { showResultadosAnalisis = null }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                                }
                                Text("Resultados del Análisis", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    item {
                                        Text(
                                            showResultadosAnalisis!!,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
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
            Text("🍒", fontSize = 64.sp)
            Spacer(Modifier.width(16.dp))
            Text("Cherry", fontSize = 64.sp, fontWeight = FontWeight.Bold)
        }
        
        // Mensaje de bienvenida
        Text(
            "¡Bienvenido/a a Cherry App!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            "Tu aplicación completa para el análisis y gestión de plantas",
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
                    Text("📊 Datos de Plantas: Gestiona datos de plantas")
                    Text("📈 Ver Gráficos: Analiza las tendencias de tus datos")
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
            colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText)
        ) {
            Text("Comenzar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Funciones auxiliares
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

