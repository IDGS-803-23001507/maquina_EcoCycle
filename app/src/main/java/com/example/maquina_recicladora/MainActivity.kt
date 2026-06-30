package com.example.maquina_recicladora

import android.os.Bundle
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
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
import com.example.maquina_recicladora.network.ApiClient
import com.example.maquina_recicladora.network.FinalizarSesionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var userId by remember { mutableStateOf("") }
    var totalBotellas by remember { mutableStateOf(0) }

    AnimatedContent(
        targetState = pantalla,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = ""
    ) { pantallaActual ->
        when (pantallaActual) {
            "bienvenida" -> BienvenidaScreen(
                onVinculada = { sid, uid ->
                    sessionId = sid
                    userId = uid
                    pantalla = "inicio"
                }
            )

            "inicio" -> InicioScreen(
                sessionId = sessionId,
                onIniciar = {
                    pantalla = "conteo"
                }
            )

            "conteo" -> ConteoScreen(
                sessionId = sessionId,
                onFinalizar = { botellas ->
                    totalBotellas = botellas
                    pantalla = "despedida"
                }
            )

            "despedida" -> DespedidaScreen(
                sessionId = sessionId,
                userId = userId,
                totalBotellas = totalBotellas,
                onReiniciar = {
                    sessionId = ""
                    userId = ""
                    totalBotellas = 0
                    pantalla = "bienvenida"
                }
            )
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
            bitmap.setPixel(x, y,
                if (bitMatrix[x, y]) android.graphics.Color.BLACK
                else android.graphics.Color.WHITE
            )
        }
    }
    return bitmap
}

const val MACHINE_ID = EcoCycleConfig.MACHINE_ID

@Composable
fun BienvenidaScreen(onVinculada: (sessionId: String, userId: String) -> Unit) {
    val sid = rememberSaveable { "${MACHINE_ID}_${System.currentTimeMillis()}" }
    val qrBitmap = remember { generarQR(sid) }
    var vinculada by remember { mutableStateOf(false) }
    var uid by remember { mutableStateOf("") }

    DisposableEffect(sid) {
        val db = FirebaseDatabase.getInstance()
        val ref = db.getReference("sessions").child(sid)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val linked = snapshot.child("linked").getValue(Boolean::class.java)
                val user = snapshot.child("userId").getValue(String::class.java)
                if (linked == true && user != null && !vinculada) {
                    vinculada = true
                    uid = user
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)

        onDispose { ref.removeEventListener(listener) }
    }

    LaunchedEffect(vinculada) {
        if (vinculada) {
            val db = FirebaseDatabase.getInstance()
            db.getReference("maquinas").child(MACHINE_ID).child("sesion_activa").setValue(sid)
            delay(3000)
            onVinculada(sid, uid)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B5D4B), Color(0xFF063D32))
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

        Box(contentAlignment = Alignment.TopCenter) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .padding(top = 50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 70.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bienvenido",
                        textAlign = TextAlign.Center,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Escanea el QR para comenzar",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2FAE32)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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

        if (vinculada) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val composition by rememberLottieComposition(
                            LottieCompositionSpec.RawRes(R.raw.success)
                        )
                        LottieAnimation(
                            composition = composition,
                            iterations = 1,
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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

@Composable
fun InicioScreen(sessionId: String, onIniciar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF003B34), Color(0xFF00594D), Color(0xFF003B34))
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

        Box(contentAlignment = Alignment.TopCenter) {
            Card(
                modifier = Modifier
                    .width(520.dp)
                    .padding(top = 45.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 75.dp, bottom = 40.dp, start = 40.dp, end = 40.dp),
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
                        onClick = {
                            FirebaseDatabase.getInstance()
                                .getReference("sessions").child(sessionId).child("status")
                                .setValue("activo")
                            onIniciar()
                        },
                        modifier = Modifier.width(220.dp).height(55.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF11B311))
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11B311)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
fun ConteoScreen(sessionId: String, onFinalizar: (botellas: Int) -> Unit) {
    var botellas by remember { mutableStateOf(0) }
    val puntos = botellas * 20

    DisposableEffect(sessionId) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("sessions").child(sessionId).child("botellas").child("count")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.getValue(Int::class.java) ?: 0
                botellas = count
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)

        onDispose { ref.removeEventListener(listener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF003B34), Color(0xFF00594D), Color(0xFF003B34))
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

        Box(contentAlignment = Alignment.TopCenter) {
            Card(
                modifier = Modifier
                    .width(520.dp)
                    .padding(top = 45.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 75.dp, bottom = 35.dp, start = 25.dp, end = 25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Botellas introducidas",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17322E)
                    )

                    Spacer(modifier = Modifier.height(35.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.botella),
                                contentDescription = null,
                                modifier = Modifier.size(90.dp)
                            )
                            Image(
                                painter = painterResource(R.drawable.botella),
                                contentDescription = null,
                                modifier = Modifier.size(90.dp).offset(x = (-40).dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "→",
                                fontSize = 40.sp,
                                color = Color(0xFF17322E)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = botellas.toString(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF17322E)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(90.dp),
                            thickness = 2.dp,
                            color = Color(0xFFE0E0E0)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.estrella),
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Puntos",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = puntos.toString(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF44C225)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(35.dp))

                    Button(
                        onClick = { onFinalizar(botellas) },
                        modifier = Modifier.width(220.dp).height(55.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44C225))
                    ) {
                        Text(
                            text = "FINALIZAR",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2FAE32)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_ecocycle),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DespedidaScreen(
    sessionId: String,
    userId: String,
    totalBotellas: Int,
    onReiniciar: () -> Unit
) {
    val totalPuntos = totalBotellas * 20
    val scope = rememberCoroutineScope()
    var enviado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance()
            .getReference("maquinas").child(MACHINE_ID).child("sesion_activa")
            .removeValue()
        FirebaseDatabase.getInstance()
            .getReference("sessions").child(sessionId).child("status")
            .setValue("completado")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF003B34), Color(0xFF00594D), Color(0xFF003B34))
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

        Box(contentAlignment = Alignment.TopCenter) {
            Card(
                modifier = Modifier
                    .width(520.dp)
                    .padding(top = 45.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 75.dp, bottom = 35.dp, start = 30.dp, end = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¡Gracias por Reciclar!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17322E)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tu contribución ayuda al planeta",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.width(70.dp),
                            color = Color(0xFF8BC34A)
                        )
                        Image(
                            painter = painterResource(R.drawable.hojita),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).padding(horizontal = 2.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.width(70.dp),
                            color = Color(0xFF8BC34A)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF5E8))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(R.drawable.hoja_sola2),
                                contentDescription = null,
                                modifier = Modifier.size(100.dp).align(Alignment.CenterStart),
                                alpha = 0.5f
                            )
                            Image(
                                painter = painterResource(R.drawable.hoja_sola),
                                contentDescription = null,
                                modifier = Modifier.size(100.dp).align(Alignment.CenterEnd),
                                alpha = 0.5f
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PUNTOS TOTALES",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E6D32)
                                )
                                Text(
                                    text = totalPuntos.toString(),
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF17322E)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalBotellas botella(s) recicladas",
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E6D32)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (!enviado) {
                                enviado = true
                                scope.launch {
                                    try {
                                        val req = FinalizarSesionRequest(
                                            maquinaId = MACHINE_ID,
                                            usuarioId = userId,
                                            botellas = totalBotellas
                                        )
                                        ApiClient.apiService.finalizarSesion(req)
                                    } catch (_: Exception) { }
                                }
                            }
                            onReiniciar()
                        },
                        modifier = Modifier.width(180.dp).height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FAE3A))
                    ) {
                        Text(
                            text = "CERRAR",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Card(modifier = Modifier.size(90.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_ecocycle),
                        contentDescription = null,
                        modifier = Modifier.size(150.dp)
                    )
                }
            }
        }
    }
}
