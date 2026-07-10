package com.example.maquina_recicladora

import android.util.Log

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun CameraDetector(
    onAligned: (ByteArray) -> Unit,
    onStatusChange: (String, Boolean) -> Unit,
    borderColor: Color = Color.Red
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                
                // Configurar ML Kit Object Detection
                val options = ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableMultipleObjects()
                    .build()
                val objectDetector = ObjectDetection.getClient(options)
                val executor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(android.util.Size(640, 480))
                        .build()

                    var lastAlignedMs = 0L
                    var lastSentMs = 0L
                    var lastStatus = ""
                    var lastIsPerfect = false

                    analyzer.setAnalyzer(executor) { imageProxy ->
                        Log.e("ECO_DEBUG", "CameraX entregó un frame.")
                        try {
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val rawBitmap = imageProxy.toBitmap()
                                val matrix = android.graphics.Matrix()
                                matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                                val bitmap = android.graphics.Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                
                                objectDetector.process(image)
                                    .addOnSuccessListener(executor) { detectedObjects ->
                                        Log.e("ECO_DEBUG", "ML Kit procesó frame. Objetos detectados: ${detectedObjects.size}")
                                        var maxArea = 0
                                    for (obj in detectedObjects) {
                                        val box = obj.boundingBox
                                        
                                        // Las coordenadas del bounding box de ML Kit siempre están en el sistema de
                                        // coordenadas de la imagen original sin rotar. Si la cámara está rotada 90 o 270 grados,
                                        // el "width" de la caja original es el "height" físico en la pantalla, y viceversa.
                                        val rotation = imageProxy.imageInfo.rotationDegrees
                                        val isRotated = rotation == 90 || rotation == 270
                                        
                                        val physicalWidth = if (isRotated) box.height() else box.width()
                                        val physicalHeight = if (isRotated) box.width() else box.height()
                                        
                                        // FILTRO: Una botella suele ser más alta que ancha.
                                        // Si la altura física no es mayor que el ancho físico, ignoramos el objeto.
                                        if (physicalHeight <= physicalWidth) {
                                            Log.e("ECO_DEBUG", "Objeto ignorado por forma (ancho: $physicalWidth, alto: $physicalHeight)")
                                            continue
                                        }

                                        val area = width * height
                                        if (area > maxArea) {
                                            maxArea = area
                                        }
                                    }
                                    
                                    val imgArea = imageProxy.width * imageProxy.height
                                    var isPerfect = false
                                    var status = "Busca una botella..."
                                    
                                    if (maxArea > 0) {
                                        val ratio = maxArea.toFloat() / imgArea.toFloat()
                                        Log.e("ECO_DEBUG", "Área del objeto: ${(ratio * 100).toInt()}% de la pantalla")
                                        if (ratio < 0.10f) {
                                            status = "Acerca la botella"
                                        } else if (ratio > 0.60f) {
                                            status = "Aleja la botella"
                                        } else {
                                            status = "¡Perfecto! Mantén ahí"
                                            isPerfect = true
                                        }
                                    } else {
                                        // Log.e("ECO_DEBUG", "No hay objetos válidos detectados") // omitido para no espamear
                                    }
                                    if (status != lastStatus || isPerfect != lastIsPerfect) {
                                        lastStatus = status
                                        lastIsPerfect = isPerfect
                                        ctx.mainExecutor.execute {
                                            onStatusChange(status, isPerfect)
                                        }
                                    }
                                    
                                    if (!isPerfect) {
                                        lastAlignedMs = 0L
                                    }

                                    val now = System.currentTimeMillis()
                                    if (isPerfect && (now - lastSentMs > 5000)) {
                                        if (lastAlignedMs == 0L) {
                                            lastAlignedMs = now
                                        } else if (now - lastAlignedMs > 1500) {
                                            Log.e("ECO_DEBUG", "¡Botella alineada! Capturando foto y enviando a Visor...")
                                            lastAlignedMs = 0L
                                            lastSentMs = now
                                            val jpeg = bitmapToJpeg(bitmap)
                                            if (jpeg != null) {
                                                ctx.mainExecutor.execute {
                                                    onAligned(jpeg)
                                                }
                                            } else {
                                                Log.e("ECO_DEBUG", "Error: bitmapToJpeg devolvió null")
                                            }
                                        } else {
                                            Log.e("ECO_DEBUG", "Botella alineada, esperando estabilización (${1500 - (now - lastAlignedMs)}ms)")
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ECO_DEBUG", "Error en ML Kit: ${e.message}", e)
                                }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                Log.e("ECO_DEBUG", "mediaImage es null")
                                imageProxy.close()
                            }
                        } catch (e: Exception) {
                            Log.e("ECO_DEBUG", "Excepción CRÍTICA en analyzer: ${e.message}", e)
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            analyzer,
                            preview
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Contorno dinámico (guía para la botella)
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(340.dp)
                .border(width = 6.dp, color = borderColor, shape = RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.1f))
        )
    }
}

private fun bitmapToJpeg(bitmap: Bitmap): ByteArray? {
    return try {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        output.toByteArray()
    } catch (e: Exception) {
        Log.e("ECO_DEBUG", "Error en bitmapToJpeg: ${e.message}", e)
        null
    }
}
