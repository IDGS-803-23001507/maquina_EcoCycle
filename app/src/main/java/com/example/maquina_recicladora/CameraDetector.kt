package com.example.maquina_recicladora

import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun CameraDetector(
    onNewFrame: (ByteArray) -> Unit,
    borderColor: Color = Color.Red,
    mensaje: String = "Coloca la botella aquí"
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = { ctx ->

                val previewView = PreviewView(ctx)

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({

                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->

                        val jpeg = imageProxyToJpeg(image)
                        if (jpeg != null) {
                            onNewFrame(jpeg)
                        }

                        image.close()
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analyzer
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        Box(
            modifier = Modifier
                .width(220.dp)
                .height(300.dp)
                .border(width = 4.dp, color = borderColor)
        )

        Text(
            text = mensaje,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        )
    }
}

@OptIn(ExperimentalGetImage::class)
private fun imageProxyToJpeg(imageProxy: ImageProxy): ByteArray? {
    return try {
        val image = imageProxy.image ?: return null

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21, ImageFormat.NV21, image.width, image.height, null
        )

        val output = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, image.width, image.height),
            70, output
        )

        output.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
