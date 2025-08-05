package com.cucei.cherryapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log // Importante para depuración
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
import androidx.activity.compose.BackHandler // Importante
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
import com.cucei.cherryapp.ui.FotoDestino
import com.cucei.cherryapp.ui.theme.WhiteButton
import com.cucei.cherryapp.ui.theme.BlackText

// Sealed class Pantalla y data class Registro permanecen igual
sealed class Pantalla {
    object Inicio : Pantalla()
    object MostrarDatos : Pantalla()
    object Galeria : Pantalla()
    object Camara : Pantalla() // Si la usas explícitamente como un estado de 'pantalla'
}

data class Registro(val temperatura: String, val humedad: String, val luminosidad: String)

// MainActivity permanece igual
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
    // ASEGÚRATE DE NO TENER onBackPressed() SOBRESCRITO AQUÍ
    // NI onBackPressedDispatcher.addCallback en un init {}
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CherryApp() {
    val context = LocalContext.current
    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Inicio) }
    var registros by remember { mutableStateOf<List<Registro>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) } // Considera usar Snackbar para errores también
    var fotos by remember { mutableStateOf<List<File>>(emptyList()) }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    val FOTOS_DIR = remember { File(context.filesDir, "CherryFotos").apply { mkdirs() } }


    var cargarRed by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var showGuardarDialog by remember { mutableStateOf<File?>(null) }
    var imagenSeleccionada by remember { mutableStateOf<File?>(null) }
    var showConfirmDelete by remember { mutableStateOf<File?>(null) }
    var showSendDialog by remember { mutableStateOf<File?>(null) }
    var calidadFoto by remember { mutableStateOf(2) }
    var showCamaraDialog by remember { mutableStateOf(false) } // Para el diálogo de selección de cámara
    var showCamaraPersonalizada by remember { mutableStateOf(false) } // Para mostrar tu CameraScreen

    val resoluciones = remember { mapOf(
        1 to (1280 to 800),
        2 to (1920 to 1080),
        5 to (2592 to 1944)
    )}

    // Para el doble toque para salir y Snackbar
    var backPressedTime by remember { mutableStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val activity = (LocalContext.current as? Activity)

    // --- MANEJO DEL BOTÓN ATRÁS ---
    // Este BackHandler es para la navegación principal y el doble toque para salir
    BackHandler(enabled = pantalla == Pantalla.Inicio || pantalla == Pantalla.Galeria || pantalla == Pantalla.MostrarDatos) {
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
            Pantalla.Galeria, Pantalla.MostrarDatos -> {
                Log.d("BackHandler", "Principal: Volviendo a Inicio desde $pantalla")
                pantalla = Pantalla.Inicio
            }
            else -> {
                // No debería llegar aquí si 'enabled' está bien configurado,
                // pero es bueno tener un log por si acaso.
                Log.d("BackHandler", "Principal: Estado no manejado o BackHandler incorrecto - $pantalla")
            }
        }
    }

    // BackHandler para cerrar la imagen en pantalla completa
    if (imagenSeleccionada != null) {
        BackHandler(enabled = true) { // Se activa solo cuando imagenSeleccionada no es null
            Log.d("BackHandler", "Cerrando imagen a pantalla completa")
            imagenSeleccionada = null
        }
    }

    // BackHandler para la cámara personalizada
    if (showCamaraPersonalizada) {
        BackHandler(enabled = true) { // Se activa solo cuando showCamaraPersonalizada es true
            Log.d("BackHandler", "Cerrando cámara personalizada")
            showCamaraPersonalizada = false
            // Opcional: si la cámara era una "pantalla", volver a inicio
            // if (pantalla == Pantalla.Camara) pantalla = Pantalla.Inicio
        }
    }
    // --- FIN MANEJO DEL BOTÓN ATRÁS ---


    // Cámara (ActivityResultLauncher)
    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && fotoUri != null) {
            val file = File(fotoUri!!.path!!)
            val (ancho, alto) = resoluciones[calidadFoto] ?: (1920 to 1080)
            try {
                val original = BitmapFactory.decodeFile(file.absolutePath)
                if (original != null) {
                    val scaled = Bitmap.createScaledBitmap(original, ancho, alto, true)
                    file.outputStream().use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    original.recycle()
                    scaled.recycle()
                }
            } catch (e: Exception) {
                // Usar Snackbar para errores sería más consistente
                coroutineScope.launch { snackbarHostState.showSnackbar("No se pudo ajustar la resolución: ${e.localizedMessage}") }
                Log.e("CherryApp", "Error al ajustar resolución", e)
            }
            showGuardarDialog = file // O directamente guardar y actualizar 'fotos'
        } else if (!success) {
            coroutineScope.launch { snackbarHostState.showSnackbar("No se pudo tomar la foto") }
        }
        fotoUri = null // Limpiar Uri después de usarla
    }

    fun abrirCamaraNativa() { // Renombrada para claridad
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val nombreFoto = File(FOTOS_DIR, "IMG_${timestamp}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Asegúrate que esto coincida con tu AndroidManifest.xml
                nombreFoto
            )
            fotoUri = uri // Guardar URI para usarla en takePhotoLauncher
            takePhotoLauncher.launch(uri)
        } catch (e: Exception) {
            coroutineScope.launch { snackbarHostState.showSnackbar("No se pudo abrir la cámara: ${e.localizedMessage}") }
            Log.e("CherryApp", "Error al abrir cámara", e)
        }
    }

    // Permisos (Deberían pedirse antes de llamar a abrirCamaraNativa o showCamaraPersonalizada)
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            // Decidir qué cámara abrir aquí o en el diálogo
            // Por ejemplo, si el diálogo ya decidió "Cámara Nativa":
            // abrirCamaraNativa()
            // O si decidió "Cámara Personalizada":
            // showCamaraPersonalizada = true
            coroutineScope.launch { snackbarHostState.showSnackbar("Permiso de cámara concedido.") }
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Permiso de cámara denegado.") }
        }
    }
    // Podrías necesitar también WRITE_EXTERNAL_STORAGE para versiones antiguas si guardas fuera de filesDir,
    // pero para `context.filesDir` no es necesario. READ_MEDIA_IMAGES para acceder a galería compartida.


    // File picker para JSON
    val pickJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().readText()
                    registros = parseJson(json) // Asegúrate que parseJson maneje errores
                    pantalla = Pantalla.MostrarDatos
                }
            } catch (e: Exception) {
                coroutineScope.launch { snackbarHostState.showSnackbar("Error al abrir el archivo: ${e.localizedMessage}") }
                Log.e("CherryApp", "Error al abrir JSON", e)
            }
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) { // Aplicar padding del Scaffold

            // Pantalla principal
            if (pantalla == Pantalla.Inicio && !showCamaraPersonalizada && imagenSeleccionada == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                        Text("🍒", fontSize = 32.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Cherry", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { pickJsonLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText)
                    ) { Text("📂 Abrir archivo JSON local") }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        cargarRed = true
                        cargando = true // Mostrar indicador de carga
                        pantalla = Pantalla.MostrarDatos
                        registros = emptyList()
                    },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText)
                    ) { Text("📡 Cargar datos desde red") }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { showCamaraDialog = true }, // Este abre el diálogo de selección
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText)
                    ) { Text("📸 Tomar foto") }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        fotos = getFotos(FOTOS_DIR)
                        pantalla = Pantalla.Galeria
                    },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText)
                    ) { Text("🖼️ Ver galería de fotos") }
                }
            }

            // Pantalla de cámara personalizada
            if (showCamaraPersonalizada) {
                CameraScreen( // Asumiendo que CameraScreen es un @Composable
                    onBack = {
                        showCamaraPersonalizada = false
                        // if (pantalla == Pantalla.Camara) pantalla = Pantalla.Inicio // Opcional
                    },
                    onPhotoSaved = { file ->
                        val destFile = File(FOTOS_DIR, "IMG_${System.currentTimeMillis()}.jpg")
                        try {
                            file.copyTo(destFile, overwrite = true)
                            file.delete() // Borra el temporal si es necesario
                            fotos = getFotos(FOTOS_DIR) // Actualizar la lista de fotos
                            coroutineScope.launch { snackbarHostState.showSnackbar("Foto guardada en la galería de la app.") }
                        } catch (e: IOException) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Error al guardar foto.") }
                            Log.e("CherryApp", "Error guardando foto de CameraScreen", e)
                        }
                        showCamaraPersonalizada = false
                    },
                    onPhotoSent = { file ->
                        // Simular envío
                        Log.d("CherryApp", "Simulando envío de foto: ${file.name}")
                        file.delete() // Opcional
                        showCamaraPersonalizada = false
                        coroutineScope.launch { snackbarHostState.showSnackbar("(Simulado) Foto enviada al servidor.") }
                    }
                )
            }

            // Efecto para cargar datos desde red
            LaunchedEffect(cargarRed) {
                if (cargarRed) {
                    cargando = true // Asegurarse que 'cargando' se active aquí
                    try {
                        val json = withContext(Dispatchers.IO) {
                            // Considera un timeout y manejo de errores más robusto
                            java.net.URL("http://192.168.1.100:5000/datos").readText()
                        }
                        registros = parseJson(json)
                    } catch (e: Exception) {
                        Log.e("CherryApp", "Error de red", e)
                        coroutineScope.launch { snackbarHostState.showSnackbar("Error de red: ${e.localizedMessage}") }
                        pantalla = Pantalla.Inicio // Volver a inicio si hay error de red
                    } finally {
                        cargarRed = false
                        cargando = false
                    }
                }
            }

            // Mostrar datos JSON
            if (pantalla == Pantalla.MostrarDatos && !showCamaraPersonalizada && imagenSeleccionada == null) {
                Column(Modifier.fillMaxSize()) {
                    Button(onClick = { pantalla = Pantalla.Inicio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhiteButton, contentColor = BlackText)
                    ) { Text("← Volver") }

                    if (cargando) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (registros.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay datos para mostrar.")
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
                                        Text("🌡️ Temperatura: ${reg.temperatura}") // Quité el % si no es parte del dato
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween // Para separar título y botón
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🍒", fontSize = 32.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("Galería", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { pantalla = Pantalla.Inicio }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver a Inicio")
                        }
                    }
                    if (fotos.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay fotos tomadas. Usa la cámara para añadir algunas.")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp), // Padding para la grid
                            contentPadding = PaddingValues(4.dp) // Padding interno de la grid
                        ) {
                            items(fotos.size) { idx ->
                                val file = fotos[idx]
                                // Considerar cargar bitmaps en un hilo de fondo si son muchos o grandes
                                val bitmap = remember(file.path) { // Usar file.path o file.lastModified() como key
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
                                    // Placeholder o indicador de error si el bitmap no se carga
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
                val file = imagenSeleccionada!! // Sabemos que no es null aquí
                // Cargar bitmap podría ser costoso, considera optimizaciones si es necesario
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
                        .background(MaterialTheme.colorScheme.background) // Fondo oscuro para la imagen
                        .clickable(onClick = { imagenSeleccionada = null }) // Cerrar al tocar fuera (opcional)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier.fillMaxSize() // O .wrapContentSize() para verla completa
                            // .align(Alignment.Center) si usas wrapContentSize
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
                            IconButton(onClick = { showConfirmDelete = file }) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }


            // --- DIÁLOGOS ---
            if (showError && error != null) { // Convertido a Snackbar en muchos lugares, pero si aún lo usas:
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
                                fotos = getFotos(FOTOS_DIR) // Actualizar lista
                                if (imagenSeleccionada == fileToDelete) { // Si estaba en vista completa
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
                            // Pedir permiso ANTES de abrir la cámara
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showCamaraPersonalizada = true
                                // Opcional: si la cámara personalizada es una "pantalla"
                                // pantalla = Pantalla.Camara
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                // La lógica para abrir la cámara se moverá al callback del permiso
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
                                // En el callback del permiso, podrías llamar a abrirCamaraNativa()
                                // o guardar una acción pendiente.
                            }
                        }) { Text("Nativa del Sistema") }
                    },
                    properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
                )
            }
            // ... otros diálogos (showGuardarDialog, showSendDialog) si los necesitas
        }
    }
}

// Asegúrate de que estas funciones estén definidas y sean correctas
fun parseJson(jsonString: String?): List<Registro> {
    if (jsonString.isNullOrBlank()) {
        Log.w("parseJson", "JSON string es nulo o vacío.")
        return emptyList()
    }
    val list = mutableListOf<Registro>()
    try {
        // Asumiendo que el JSON es un Array de Objetos
        // Si la raíz es un objeto que contiene un array, ajusta esto.
        // Ejemplo: { "datos": [ ... ] } requeriría `JSONObject(jsonString).getJSONArray("datos")`
        val jsonArray = org.json.JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            // Usar optString para evitar excepciones si la clave no existe, y proveer un default.
            val temp = jsonObject.optString("temperatura", "N/A")
            val hum = jsonObject.optString("humedad", "N/A")
            val lum = jsonObject.optString("luminosidad", "N/A")
            list.add(Registro(temp, hum, lum))
        }
    } catch (e: org.json.JSONException) {
        Log.e("parseJson", "Error al parsear JSON: ${e.localizedMessage}")
        // Podrías devolver la lista parcialmente parseada o vacía, según prefieras.
        // O lanzar una excepción personalizada si el llamador debe manejarla.
    }
    return list
}

fun getFotos(directory: File): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
        Log.w("getFotos", "El directorio no existe o no es un directorio: ${directory.absolutePath}")
        return emptyList()
    }
    // Filtrar por extensión y asegurarse de que sean archivos
    return directory.listFiles { file ->
        file.isFile && (file.extension.equals("jpg", ignoreCase = true) || file.extension.equals("jpeg", ignoreCase = true))
    }?.sortedDescending()?.toList() ?: emptyList<File>().also {
        Log.d("getFotos", "No se encontraron archivos jpg/jpeg en ${directory.absolutePath}")
    }
}

