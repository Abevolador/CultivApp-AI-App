package com.cucei.cherryapp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onPhotoTaken: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estados
    var error by remember { mutableStateOf<String?>(null) }
    var isTakingPhoto by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setTargetResolution(android.util.Size(1920, 1920)) // Resolución alta para mejor calidad
            .build() 
    }

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
                title = { Text("Escaneo Enfocado") },
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
            // Vista previa de cámara con recuadro de enfoque
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
                        // Vista previa de cámara
                        CameraPreview(
                            imageCapture = imageCapture,
                            cameraExecutor = cameraExecutor,
                            lifecycleOwner = lifecycleOwner,
                            onCameraReady = { cam -> camera = cam }
                        )
                        
                        // Capa semitransparente oscura
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                        
                        // Recuadro de enfoque central
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .border(
                                    width = 3.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(Color.Transparent)
                        )
                        
                        // Texto de instrucción
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 120.dp)
                        ) {
                            Text(
                                text = "Enfoca el objeto dentro del recuadro",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.7f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                
                // Controles inferiores
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Botón de captura
                    Button(
                        onClick = {
                            if (!isTakingPhoto) {
                                isTakingPhoto = true
                                takeFocusedPhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    cameraExecutor = cameraExecutor,
                                    onPhotoReady = { file ->
                                        isTakingPhoto = false
                                        onPhotoTaken(file)
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
                        enabled = !isTakingPhoto,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isTakingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = Color.Black
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
    lifecycleOwner: LifecycleOwner,
    onCameraReady: (Camera) -> Unit
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
            .aspectRatio(1f) // Vista cuadrada
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
                    
                    // Configurar autoenfoque continuo
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                    
                    // Configurar autoenfoque continuo en el área central
                    camera.cameraControl.enableTorch(false)
                    camera.cameraControl.setLinearZoom(0f)
                    
                    // Configurar autoenfoque continuo en el área central
                    try {
                        val previewViewSize = previewView.width to previewView.height
                        if (previewViewSize.first > 0 && previewViewSize.second > 0) {
                            val centerX = previewViewSize.first / 2f
                            val centerY = previewViewSize.second / 2f
                            
                            // Crear un MeteringPoint en el centro
                            val meteringPointFactory = previewView.meteringPointFactory
                            val centerPoint = meteringPointFactory.createPoint(centerX, centerY)
                            
                            val focusMeteringAction = FocusMeteringAction.Builder(
                                centerPoint,
                                FocusMeteringAction.FLAG_AF
                            ).setAutoCancelDuration(5, java.util.concurrent.TimeUnit.SECONDS).build()
                            
                            camera.cameraControl.startFocusAndMetering(focusMeteringAction)
                        }
                    } catch (e: Exception) {
                        // Si falla el autoenfoque, continuar sin él
                    }
                    
                    onCameraReady(camera)
                } catch (e: Exception) {
                    // Manejar error
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

fun takeFocusedPhoto(
    context: Context,
    imageCapture: ImageCapture,
    cameraExecutor: ExecutorService,
    onPhotoReady: (File) -> Unit,
    onError: (String) -> Unit
) {
    val photoFile = File(
        context.filesDir,
        "foto_enfocada_${System.currentTimeMillis()}.jpg"
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
                        // Calcular el área de recorte central (recuadro de enfoque)
                        val size = minOf(originalBitmap.width, originalBitmap.height)
                        val x = (originalBitmap.width - size) / 2
                        val y = (originalBitmap.height - size) / 2
                        
                        // Recortar la imagen al área central
                        val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)
                        
                        // Redimensionar a 512x512
                        val finalBitmap = Bitmap.createScaledBitmap(croppedBitmap, 512, 512, true)
                        
                        // Guardar la imagen final
                        val outputStream = photoFile.outputStream()
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.close()
                        
                        // Liberar memoria
                        originalBitmap.recycle()
                        croppedBitmap.recycle()
                        finalBitmap.recycle()
                        
                        onPhotoReady(photoFile)
                    } else {
                        onError("No se pudo decodificar la imagen")
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