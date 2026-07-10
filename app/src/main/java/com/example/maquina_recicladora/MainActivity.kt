package com.example.maquina_recicladora

import android.os.Bundle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.maquina_recicladora.ui.theme.Maquina_recicladoraTheme
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.HorizontalDivider
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.rememberSaveable
import com.airbnb.lottie.compose.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Maquina_recicladoraTheme {
                AppMaquina()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppMaquina() {

    var pantalla by remember { mutableStateOf("bienvenida") }
    var sessionId by remember { mutableStateOf("") }
    var usuarioId by remember { mutableStateOf("") }
    var botellasContadas by remember { mutableIntStateOf(0) }

    AnimatedContent(
        targetState = pantalla,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = ""
    ) { pantallaActual ->

        when (pantallaActual) {

            "bienvenida" -> BienvenidaScreen { sid, uid ->
                sessionId = sid
                usuarioId = uid
                botellasContadas = 0
                pantalla = "inicio"
            }

            "inicio" -> InicioScreen {
                pantalla = "validacion"
            }

            "validacion" -> ValidacionBotellaScreen(
                sessionId = sessionId,
                usuarioId = usuarioId,
                machineId = EcoCycleConfig.MACHINE_ID,
                onFinalizar = { count ->
                    botellasContadas = count
                    pantalla = "resumen"
                }
            )

            "resumen" -> ResumenFinalScreen(
                botellas = botellasContadas,
                puntos = botellasContadas * 20,
                onReiniciar = {
                    pantalla = "bienvenida"
                }
            )
        }
    }
}
@Composable
fun BienvenidaScreen(
    onContinuar: (sessionId: String, usuarioId: String) -> Unit
) {

    val db = FirebaseFirestore.getInstance()


    val sid = rememberSaveable {

        "machine_001_" + System.currentTimeMillis()

    }


    var vinculada by remember {

        mutableStateOf(false)

    }

    var uid by remember {

        mutableStateOf("")

    }


    val qrBitmap = remember(sid) {

        generarQR(sid)

    }



    // Crear sesión en Firestore

    LaunchedEffect(sid) {


        val datos = hashMapOf(

            "maquina_id" to "MQ-ECO-01",

            "estado" to "esperando_usuario",

            "usuario_id" to null,

            "fecha" to FieldValue.serverTimestamp()

        )


        db.collection("sesiones_reciclaje")
            .document(sid)
            .set(datos)
            .addOnSuccessListener {

                println("SESION CREADA: $sid")

            }
            .addOnFailureListener {

                println("ERROR CREANDO SESION: ${it.message}")

            }

    }





    // Escuchar vinculación

    DisposableEffect(sid) {


        println("ESCUCHANDO SESION: $sid")



        val listener = db.collection("sesiones_reciclaje")
            .document(sid)
            .addSnapshotListener { snapshot, error ->



                if(error != null){

                    println(
                        "ERROR LISTENER: ${error.message}"
                    )

                    return@addSnapshotListener

                }



                if(snapshot != null && snapshot.exists()){


                    val usuId =
                        snapshot.getString("usuario_id")



                    println(
                        "USUARIO DETECTADO: $usuId"
                    )



                    if(usuId != null && !vinculada){

                        uid = usuId
                        vinculada = true


                    }


                }


            }





        onDispose {

            listener.remove()
        }

    }

    LaunchedEffect(vinculada){

        if(vinculada){

            delay(3000)
            onContinuar(sid, uid)

        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B5D4B),
                        Color(0xFF063D32)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.hojas),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.28f
        )

    var startAnimation by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale_anim"
    )
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier.scale(scaleAnim),
        contentAlignment = Alignment.TopCenter
    ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .padding(top = 50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 70.dp,
                            bottom = 40.dp,
                            start = 24.dp,
                            end = 24.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Bienvenido",
                        textAlign = TextAlign.Center,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Escanea el QR para comenzar",
                        textAlign = TextAlign.Center
                    )



                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )



                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(220.dp)
                    )

                }

            }





            Card(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2FAE32)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {



                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {



                    Image(
                        painter = painterResource(R.drawable.logo_ecocycle),
                        contentDescription = null,
                        modifier = Modifier.size(70.dp)
                    )


                }

            }

        }





        if(vinculada) {



            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.55f)
                    ),
                contentAlignment = Alignment.Center
            ) {



                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        val composition by rememberLottieComposition(
                            LottieCompositionSpec.RawRes(
                                R.raw.success
                            )
                        )

                        LottieAnimation(
                            composition = composition,
                            iterations = 1,
                            modifier = Modifier.size(180.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "Máquina vinculada",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
fun generarQR(texto: String): Bitmap {

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, 512, 512)

    val width = bitMatrix.width
    val height = bitMatrix.height

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(
                x,
                y,
                if (bitMatrix[x, y]) android.graphics.Color.BLACK
                else android.graphics.Color.WHITE
            )
        }
    }

    return bitmap
}

@Composable
fun InicioScreen(onIniciar: () -> Unit) {

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF003B34),
                        Color(0xFF00594D),
                        Color(0xFF003B34)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.hojas),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.20f
        )

        Box(
            contentAlignment = Alignment.TopCenter
        ) {

            Card(
                modifier = Modifier
                    .width(520.dp)
                    .padding(top = 45.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 75.dp,
                            bottom = 40.dp,
                            start = 40.dp,
                            end = 40.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "¡Listos para Reciclar!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A2A28),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Presiona INICIAR para abrir la compuerta",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = onIniciar,
                        modifier = Modifier
                            .width(220.dp)
                            .height(55.dp)
                            .scale(scalePulse),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF11B311)
                        )
                    ) {
                        Text(
                            text = "INICIAR",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF11B311)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Image(
                        painter = painterResource(R.drawable.logo_ecocycle),
                        contentDescription = "Logo",
                        modifier = Modifier.size(58.dp)
                    )

                }
            }
        }
    }
}

@Composable
fun ResumenFinalScreen(
    botellas: Int,
    puntos: Int,
    onReiniciar: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        onReiniciar()
    }

    var startAnimation by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val scaleAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, 
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "scale_anim"
    )
    androidx.compose.runtime.LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF003B34),
                        Color(0xFF00594D),
                        Color(0xFF003B34)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.hojas),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.20f
        )

        Box(
            modifier = Modifier.scale(scaleAnim),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .width(520.dp)
                    .padding(top = 45.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 75.dp,
                            bottom = 35.dp,
                            start = 25.dp,
                            end = 25.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¡Sesión Completada!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17322E)
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Botellas",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = botellas.toString(),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF17322E)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(70.dp),
                            thickness = 2.dp,
                            color = Color(0xFFE0E0E0)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Puntos Obtenidos",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = puntos.toString(),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF44C225)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = onReiniciar,
                        modifier = Modifier
                            .width(200.dp)
                            .height(55.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1FAE3A)
                        )
                    ) {
                        Text(
                            text = "CERRAR",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.size(90.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_ecocycle),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimacionBotellaGuia() {
    val infiniteTransition = rememberInfiniteTransition(label = "bottle_tilt")
    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = 35f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt_angle_3d"
    )

    Box(
        modifier = Modifier
            .size(280.dp)
            .graphicsLayer {
                cameraDistance = 8 * density
                rotationX = tiltAngle
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_botella_animada_base),
            contentDescription = "Coloca la botella así",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ValidacionBotellaScreen(
    sessionId: String,
    usuarioId: String,
    machineId: String,
    onFinalizar: (count: Int) -> Unit
) {
    var guardando by remember { androidx.compose.runtime.mutableStateOf(false) }

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var mensaje by remember {
        mutableStateOf("Coloca una botella frente a la cámara")
    }

    var botellas by remember {
        mutableIntStateOf(0)
    }

    var validando by remember {
        mutableStateOf(false)
    }

    var esBotella by remember {
        mutableStateOf(false)
    }

    var statusText by remember {
        mutableStateOf("Esperando cámara...")
    }

    var latestJpeg by remember {
        mutableStateOf<ByteArray?>(null)
    }

    var lastDetectMs by remember {
        mutableLongStateOf(0L)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        statusText = "Coloca una botella frente a la cámara"
        while (true) {
            delay(300)
            val jpeg = latestJpeg
            if (jpeg == null) {
                mensaje = "Esperando cámara..."
                continue
            }
            if (validando) continue

            val now = System.currentTimeMillis()
            if (now - lastDetectMs < 1500) continue
            lastDetectMs = now

            statusText = "Enviando a Visor..."
            val detected = ApiClient.detectarEnVisor(jpeg)
            when (detected) {
                null -> {
                    esBotella = false
                    mensaje = "Error de conexión (Visor)"
                    statusText = "Reintentando..."
                }
                true -> {
                    esBotella = true
                    validando = true
                    mensaje = "Botella detectada"
                    statusText = "Abriendo compuerta..."
                    ApiClient.validarBotella(
                        sessionId = sessionId,
                        machineId = machineId,
                        esBotella = true
                    )
                    botellas++
                    statusText = "Botella #$botellas almacenada!"
                    mensaje = "Botella aceptada"
                    delay(2000)
                    mensaje = "Coloca la siguiente"
                    validando = false
                    lastDetectMs = 0
                }
                false -> {
                    esBotella = false
                    mensaje = "No se detectó botella"
                    statusText = "Intenta de nuevo"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF003B34),
                        Color(0xFF00594D),
                        Color(0xFF003B34)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Box(
            contentAlignment = Alignment.TopCenter
        ) {

            Card(
                modifier = Modifier
                    .width(520.dp)
                    .padding(top = 45.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 30.dp,
                            bottom = 30.dp,
                            start = 20.dp,
                            end = 20.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Valida tus botellas",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17322E)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val messageColor = when {
                        validando || esBotella -> Color(0xFF2E6D32)
                        mensaje.contains("Error") || mensaje.contains("No se") || mensaje.contains("Intenta") -> Color(0xFFD32F2F)
                        else -> Color(0xFF424242)
                    }
                    val messageBg = when {
                        validando || esBotella -> Color(0xFFEAF5E8)
                        mensaje.contains("Error") || mensaje.contains("No se") || mensaje.contains("Intenta") -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF5F5F5)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = messageBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mensaje,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = messageColor
                            )
                            if (statusText.isNotEmpty()) {
                                Text(
                                    text = statusText,
                                    fontSize = 12.sp,
                                    color = messageColor.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .height(320.dp)
                        ) {
                            if (hasCameraPermission) {
                                CameraDetector(
                                    onNewFrame = { bytes -> latestJpeg = bytes },
                                    borderColor = if (esBotella) Color.Green else Color.Red,
                                    mensaje = "Alinea la botella aquí"
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Se requiere cámara", color = Color.White)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                            Text("Dar Permiso")
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Botella animada fuera de la cámara
                        AnimacionBotellaGuia()
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Botellas: $botellas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17322E)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!guardando) {
                                guardando = true
                                scope.launch {
                                    val db = FirebaseFirestore.getInstance()
                                    val docRef = db.collection("sesiones_reciclaje").document(sessionId)
                                    
                                    if (botellas == 0) {
                                        docRef.delete()
                                    } else {
                                        docRef.update(
                                            "estado", "completada",
                                            "botellas", botellas,
                                            "puntos", botellas * 20
                                        )
                                        ApiClient.registrarSesion(
                                            usuarioId = usuarioId,
                                            maquinaId = machineId,
                                            botellas = botellas
                                        )
                                    }
                                    
                                    ApiClient.limpiarSesionMaquina(machineId)
                                    onFinalizar(botellas)
                                }
                            }
                        },
                        modifier = Modifier
                            .width(220.dp)
                            .height(55.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF44C225)
                        ),
                        enabled = !guardando
                    ) {
                        Text(
                            text = if (guardando) "GUARDANDO..." else "FINALIZAR",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF11B311)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_ecocycle),
                        contentDescription = "Logo",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }
    }
}