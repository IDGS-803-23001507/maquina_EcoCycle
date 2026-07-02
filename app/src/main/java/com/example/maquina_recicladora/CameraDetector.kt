package com.example.maquina_recicladora

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import androidx.camera.core.CameraSelector
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
    onBottleDetected: () -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var botellaDetectada by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {


        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = { ctx ->

                val previewView =
                    PreviewView(ctx)

                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(ctx)



                cameraProviderFuture.addListener({


                    val cameraProvider =
                        cameraProviderFuture.get()

                    val preview =
                        Preview.Builder()
                            .build()

                    preview.setSurfaceProvider(
                        previewView.surfaceProvider
                    )

                    val detector =
                        BottleDetector(context)



                    val analyzer =
                        ImageAnalysis.Builder()
                            .setBackpressureStrategy(
                                ImageAnalysis
                                    .STRATEGY_KEEP_ONLY_LATEST
                            )
                            .build()



                    analyzer.setAnalyzer(
                        Executors.newSingleThreadExecutor()
                    ) { image ->



                        val bitmap =
                            imageProxyToBitmap(image)



                        if(bitmap != null){


                            val zonaBotella =
                                recortarZonaBotella(
                                    bitmap
                                )



                            val detectada =
                                detector.detect(
                                    zonaBotella
                                )

                            if(
                                detectada &&
                                !botellaDetectada
                            ){

                                botellaDetectada = true

                                onBottleDetected()

                            }

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


                    } catch(e: Exception){

                        e.printStackTrace()

                    }

                },
                    ContextCompat.getMainExecutor(ctx))

                previewView

            }

        )



        // Zona donde debe colocarse la botella

        Box(
            modifier = Modifier
                .width(220.dp)
                .height(300.dp)
                .border(
                    width = 4.dp,
                    color = Color.Green
                )
        )



        Text(
            text = "Coloca la botella aquí",
            color = Color.White,
            modifier = Modifier
                .align(
                    Alignment.BottomCenter
                )
                .padding(
                    bottom = 40.dp
                )
        )

    }

}

private fun recortarZonaBotella(
    bitmap: Bitmap
): Bitmap {

    val width =
        bitmap.width

    val height =
        bitmap.height



    val cropWidth =
        (width * 0.55).toInt()



    val cropHeight =
        (height * 0.75).toInt()



    val left =
        (width - cropWidth) / 2



    val top =
        (height - cropHeight) / 2




    return Bitmap.createBitmap(
        bitmap,
        left,
        top,
        cropWidth,
        cropHeight
    )

}


@OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun imageProxyToBitmap(
    imageProxy: ImageProxy
): Bitmap? {

    val image =
        imageProxy.image
            ?: return null

    val yBuffer =
        image.planes[0].buffer


    val uBuffer =
        image.planes[1].buffer


    val vBuffer =
        image.planes[2].buffer



    val ySize =
        yBuffer.remaining()


    val uSize =
        uBuffer.remaining()


    val vSize =
        vBuffer.remaining()



    val nv21 =
        ByteArray(
            ySize +
                    uSize +
                    vSize
        )



    yBuffer.get(
        nv21,
        0,
        ySize
    )


    vBuffer.get(
        nv21,
        ySize,
        vSize
    )


    uBuffer.get(
        nv21,
        ySize + vSize,
        uSize
    )



    val yuvImage =
        android.graphics.YuvImage(
            nv21,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

    val output =
        ByteArrayOutputStream()



    yuvImage.compressToJpeg(
        android.graphics.Rect(
            0,
            0,
            image.width,
            image.height
        ),
        90,
        output
    )

    return BitmapFactory.decodeByteArray(
        output.toByteArray(),
        0,
        output.size()
    )
}