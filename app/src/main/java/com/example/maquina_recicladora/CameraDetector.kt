package com.example.maquina_recicladora

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
import java.nio.ByteBuffer
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
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(android.util.Size(1280, 720))
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

        Box(
            modifier = Modifier
                .width(220.dp)
                .height(300.dp)
                .border(width = 4.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
        )

        Text(
            text = mensaje,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalGetImage::class)
private fun imageProxyToJpeg(imageProxy: ImageProxy): ByteArray? {
    return try {
        val image = imageProxy.image ?: return null
        val planes = image.planes
        val w = image.width
        val h = image.height
        val nv21 = ByteArray(w * h * 3 / 2)

        val yPlane = planes[0]
        val yBuf = yPlane.buffer
        val yStride = yPlane.rowStride
        val yPix = yPlane.pixelStride

        var dstOff = 0
        for (row in 0 until h) {
            yBuf.position(row * yStride)
            if (yPix == 1 && yStride == w) {
                yBuf.get(nv21, dstOff, w)
                dstOff += w
            } else {
                for (col in 0 until w) {
                    nv21[dstOff++] = yBuf.get(yBuf.position() + col * yPix)
                }
            }
        }

        val uPlane = planes[1]
        val vPlane = planes[2]
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        val uStride = uPlane.rowStride
        val vStride = vPlane.rowStride
        val uPix = uPlane.pixelStride
        val vPix = vPlane.pixelStride

        val cw = w / 2
        val ch = h / 2
        for (row in 0 until ch) {
            val uRowBase = row * uStride
            val vRowBase = row * vStride
            for (col in 0 until cw) {
                nv21[dstOff++] = vBuf.get(vRowBase + col * vPix)
                nv21[dstOff++] = uBuf.get(uRowBase + col * uPix)
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, w, h, null)
        val output = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, w, h), 95, output)
        output.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
