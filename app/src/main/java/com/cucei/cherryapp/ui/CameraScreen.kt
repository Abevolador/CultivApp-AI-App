package com.cucei.cherryapp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class FotoDestino {
    GALERIA, SERVIDOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onPhotoTaken: (File) -> Unit // Nueva función para cuando se toma la foto
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estados
    var calidadFoto by remember { mutableStateOf(2) } // 1, 2, 5 MP
    var showQualityDropdown by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isTakingPhoto by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Resoluciones actualizadas según los requerimientos
    val resoluciones = mapOf(
        1 to 1280,  // VGA (640x480) -> 1MP (1280x720)
        2 to 1920,  // 2MP (1920x1080)
        5 to 2592   // 5MP (2592x1944)
    )
    val calidadOptions = listOf(1, 2, 5)
    val calidadLabels = mapOf(
        1 to "VGA (640x480)",
        2 to "1MP (1280x720)",
        5 to "2MP (1920x1080)"
    )

    // Permisos
    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) error = "Se requiere permiso de cámara"
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cámara") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Vista previa de cámara y controles
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        CameraPreview(
                            imageCapture = imageCapture,
                            cameraExecutor = cameraExecutor,
                            lifecycleOwner = lifecycleOwner
                        )
                    }
                }
                // Controles inferiores
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ComboBox de calidad
                    Box {
                        OutlinedButton(onClick = { showQualityDropdown = true }) {
                            Text(calidadLabels[calidadFoto] ?: "${calidadFoto} MP")
                        }
                        DropdownMenu(
                            expanded = showQualityDropdown,
                            onDismissRequest = { showQualityDropdown = false }
                        ) {
                            calidadOptions.forEach { mp ->
                                DropdownMenuItem(
                                    text = { Text(calidadLabels[mp] ?: "${mp} MP") },
                                    onClick = {
                                        calidadFoto = mp
                                        showQualityDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // Botón de captura
                    Button(
                        onClick = {
                            if (!isTakingPhoto) {
                                isTakingPhoto = true
                                takePhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    cameraExecutor = cameraExecutor,
                                    resolucion = resoluciones[calidadFoto] ?: 1920,
                                    onPhotoReady = { file, _ ->
                                        isTakingPhoto = false
                                        onPhotoTaken(file) // Usar la nueva función
                                    },
                                    onError = { errorMsg ->
                                        isTakingPhoto = false
                                        error = errorMsg
                                    }
                                )
                            }
                        },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        enabled = !isTakingPhoto
                    ) {
                        if (isTakingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Camera,
                                contentDescription = "Tomar foto",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }
            
            // Error dialog
            if (error != null) {
                AlertDialog(
                    onDismissRequest = { error = null },
                    title = { Text("Error") },
                    text = { Text(error!!) },
                    confirmButton = {
                        Button(onClick = { error = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    cameraExecutor: ExecutorService,
    lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f) // Hacer la vista cuadrada
            .clip(RoundedCornerShape(16.dp))
            .border(4.dp, Color.White, RoundedCornerShape(16.dp)),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    // Manejar error
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    cameraExecutor: ExecutorService,
    resolucion: Int,
    onPhotoReady: (File, Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    val photoFile = File(
        context.filesDir,
        "foto_${System.currentTimeMillis()}.jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    if (originalBitmap != null) {
                        val size = minOf(originalBitmap.width, originalBitmap.height)
                        val x = (originalBitmap.width - size) / 2
                        val y = (originalBitmap.height - size) / 2
                        val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)
                        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, resolucion, resolucion, true)
                        val outputStream = photoFile.outputStream()
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.close()
                        originalBitmap.recycle()
                        croppedBitmap.recycle()
                        onPhotoReady(photoFile, scaledBitmap)
                    }
                } catch (e: Exception) {
                    onError("Error al procesar la imagen: ${e.message}")
                }
            }
            override fun onError(exception: ImageCaptureException) {
                onError("Error al tomar la foto: ${exception.message}")
            }
        }
    )
} 