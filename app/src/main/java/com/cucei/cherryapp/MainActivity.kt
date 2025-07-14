package com.cucei.cherryapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import android.graphics.Bitmap
import com.cucei.cherryapp.ui.CameraScreen
import com.cucei.cherryapp.ui.FotoDestino

sealed class Pantalla {
    object Inicio : Pantalla()
    object MostrarDatos : Pantalla()
    object Galeria : Pantalla()
    object Camara : Pantalla()
}

data class Registro(val temperatura: String, val humedad: String, val luminosidad: String)

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
    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Inicio) }
    var registros by remember { mutableStateOf<List<Registro>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var fotos by remember { mutableStateOf<List<File>>(emptyList()) }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    val FOTOS_DIR = File(context.filesDir, "CherryFotos")
    if (!FOTOS_DIR.exists()) FOTOS_DIR.mkdirs()

    var cargarRed by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var showGuardarDialog by remember { mutableStateOf<File?>(null) }
    var imagenSeleccionada by remember { mutableStateOf<File?>(null) }
    var showConfirmDelete by remember { mutableStateOf<File?>(null) }
    var showSendDialog by remember { mutableStateOf<File?>(null) }
    var calidadFoto by remember { mutableStateOf(2) } // 1, 2 o 5 (MP)
    var showCamaraDialog by remember { mutableStateOf(false) }
    var showCamaraPersonalizada by remember { mutableStateOf(false) }
    val resoluciones = mapOf(
        1 to (1280 to 800),   // 1MP aprox
        2 to (1920 to 1080),  // 2MP aprox
        5 to (2592 to 1944)   // 5MP aprox
    )

    // Cámara
    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && fotoUri != null) {
            val file = File(fotoUri!!.path!!)
            // Redimensionar la imagen según la calidad seleccionada
            val (ancho, alto) = resoluciones[calidadFoto] ?: (1920 to 1080)
            try {
                val original = BitmapFactory.decodeFile(file.absolutePath)
                if (original != null) {
                    val scaled = Bitmap.createScaledBitmap(original, ancho, alto, true)
                    val out = file.outputStream()
                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                    out.close()
                    original.recycle()
                    scaled.recycle()
                }
            } catch (e: Exception) {
                error = "No se pudo ajustar la resolución: ${e.localizedMessage}"
                showError = true
            }
            showGuardarDialog = file
        } else if (!success) {
            error = "No se pudo tomar la foto"
            showError = true
        }
    }

    fun abrirCamara() {
        try {
            val nombreFoto = File(FOTOS_DIR, "foto_${FOTOS_DIR.listFiles()?.size?.plus(1) ?: 1}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                nombreFoto
            )
            fotoUri = uri
            takePhotoLauncher.launch(uri)
        } catch (e: Exception) {
            error = "No se pudo tomar la foto: ${e.localizedMessage}"
            showError = true
        }
    }

    // Permisos
    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            abrirCamara()
        } else {
            error = "Permiso de cámara denegado"
            showError = true
        }
    }
    val requestStoragePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            abrirCamara()
        } else {
            error = "Permiso de almacenamiento denegado"
            showError = true
        }
    }

    // File picker para JSON
    val pickJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val json = input?.bufferedReader()?.readText()
                val datos = parseJson(json)
                registros = datos
                pantalla = Pantalla.MostrarDatos
            } catch (e: Exception) {
                error = "Error al abrir el archivo: ${e.localizedMessage}"
                showError = true
            }
        }
    }

    // Pantalla principal
    if (pantalla == Pantalla.Inicio) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text("🍒", fontSize = 32.sp)
                Spacer(Modifier.width(8.dp))
                Text("Cherry", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { pickJsonLauncher.launch("application/json") }, modifier = Modifier.fillMaxWidth()) {
                Text("📂 Abrir archivo JSON local")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                cargarRed = true
                cargando = true
                pantalla = Pantalla.MostrarDatos
                registros = emptyList()
                error = null
                showError = false
            }, modifier = Modifier.fillMaxWidth()) {
                Text("📡 Cargar datos desde red")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { showCamaraDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("📸 Tomar foto")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                fotos = getFotos(FOTOS_DIR)
                pantalla = Pantalla.Galeria
            }, modifier = Modifier.fillMaxWidth()) {
                Text("🖼️ Ver galería de fotos")
            }
        }
    }

    // Pantalla de cámara personalizada
    if (showCamaraPersonalizada) {
        CameraScreen(
            onBack = { showCamaraPersonalizada = false },
            onPhotoSaved = { file ->
                // Mover la foto a la galería interna (CherryFotos)
                val destFile = File(FOTOS_DIR, "foto_${System.currentTimeMillis()}.jpg")
                file.copyTo(destFile, overwrite = true)
                file.delete()
                fotos = getFotos(FOTOS_DIR)
                showCamaraPersonalizada = false
                error = "Foto guardada en la galería de la app."
                showError = true
            },
            onPhotoSent = { file ->
                // Simular envío al servidor
                file.delete() // Opcional: eliminar tras enviar
                showCamaraPersonalizada = false
                error = "(Simulado) Foto enviada al servidor."
                showError = true
            }
        )
    }

    // Efecto para cargar datos desde red
    LaunchedEffect(cargarRed) {
        if (cargarRed) {
            try {
                val json = withContext(Dispatchers.IO) {
                    java.net.URL("http://192.168.1.100:5000/datos").readText()
                }
                registros = parseJson(json)
            } catch (e: Exception) {
                error = "Error de red: ${e.localizedMessage}"
                showError = true
                pantalla = Pantalla.Inicio
            } finally {
                cargarRed = false
                cargando = false
            }
        }
    }

    // Mostrar datos JSON
    if (pantalla == Pantalla.MostrarDatos) {
        Column(Modifier.fillMaxSize()) {
            Button(onClick = { pantalla = Pantalla.Inicio }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("← Volver")
            }
            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (registros.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay datos para mostrar.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
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
                                Text("🌡️ Temperatura: ${reg.temperatura}%")
                                Text("💧 Humedad: ${reg.humedad}%")
                                Text("🔆 Luminosidad: ${reg.luminosidad}%")
                            }
                        }
                    }
                }
            }
        }
    }

    // Galería
    if (pantalla == Pantalla.Galeria) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text("🍒", fontSize = 32.sp)
                Spacer(Modifier.width(8.dp))
                Text("Galería", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = { pantalla = Pantalla.Inicio }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("← Volver")
            }
            if (fotos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay fotos tomadas.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(fotos.size) { idx ->
                        val file = fotos[idx]
                        val bitmap = remember(file) {
                            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
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
                        }
                    }
                }
            }
        }
    }

    // Pantalla completa de imagen seleccionada
    if (imagenSeleccionada != null) {
        val file = imagenSeleccionada!!
        val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Imagen seleccionada",
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Botón cerrar (X) esquina superior izquierda
            IconButton(
                onClick = { imagenSeleccionada = null },
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
            // Botón borrar esquina superior derecha
            IconButton(
                onClick = { showConfirmDelete = file },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar")
            }
            // Botón compartir esquina inferior izquierda
            IconButton(
                onClick = {
                    val uri = FileProvider.getUriForFile(
                        context,
                        context.packageName + ".provider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir imagen"))
                },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir")
            }
            // Botón enviar al servidor esquina inferior derecha
            IconButton(
                onClick = { showSendDialog = file },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar al servidor")
            }
        }
    }

    // Confirmación de borrado
    if (showConfirmDelete != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = null },
            title = { Text("¿Borrar foto?") },
            text = { Text("¿Seguro que quieres borrar esta foto? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDelete?.delete()
                    fotos = getFotos(FOTOS_DIR)
                    imagenSeleccionada = null
                    showConfirmDelete = null
                }) { Text("Sí, borrar") }
            },
            dismissButton = {
                Button(onClick = { showConfirmDelete = null }) { Text("No") }
            }
        )
    }

    // Diálogo para enviar al servidor
    if (showSendDialog != null) {
        AlertDialog(
            onDismissRequest = { showSendDialog = null },
            title = { Text("Enviar foto al servidor") },
            text = { Text("(Simulado) ¿Enviar esta foto al servidor para análisis?") },
            confirmButton = {
                Button(onClick = {
                    error = "(Simulado) Foto enviada al servidor de análisis."
                    showError = true
                    showSendDialog = null
                }) { Text("Enviar") }
            },
            dismissButton = {
                Button(onClick = { showSendDialog = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo modal para submenú de cámara
    if (showCamaraDialog) {
        AlertDialog(
            onDismissRequest = { showCamaraDialog = false },
            title = { Text("Opciones de cámara") },
            text = {
                Column {
                    Text("Selecciona el tipo de cámara:")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showCamaraDialog = false
                            showCamaraPersonalizada = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📱 Cámara personalizada")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showCamaraDialog = false
                            // Solicitar permisos antes de abrir cámara del sistema
                            when {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED -> {
                                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                                }
                                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT < 29 -> {
                                    requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                                else -> {
                                    abrirCamara()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📸 Cámara del sistema ")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCamaraDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Popup de error
    if (showError && error != null) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error") },
            text = { Text(error ?: "") },
            confirmButton = {
                Button(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }
}

fun parseJson(json: String?): List<Registro> {
    if (json == null) return emptyList()
    val obj = JSONObject(json)
    val arr = obj.optJSONArray("datos") ?: return emptyList()
    return List(arr.length()) { i ->
        val reg = arr.getJSONObject(i)
        Registro(
            temperatura = reg.optString("temperatura"),
            humedad = reg.optString("humedad"),
            luminosidad = reg.optString("luminosidad")
        )
    }
}

fun getFotos(dir: File): List<File> {
    return dir.listFiles()?.sortedBy { it.name } ?: emptyList()
}