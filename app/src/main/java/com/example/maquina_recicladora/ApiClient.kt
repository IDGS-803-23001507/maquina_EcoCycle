package com.example.maquina_recicladora

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json".toMediaType()

    // Adjunta la API key de máquina a todas las peticiones (si está configurada)
    private fun Request.Builder.withApiKey(): Request.Builder = apply {
        if (EcoCycleConfig.MACHINE_API_KEY.isNotEmpty()) {
            header("X-Api-Key", EcoCycleConfig.MACHINE_API_KEY)
        }
    }

    suspend fun validarBotella(
        sessionId: String,
        machineId: String,
        esBotella: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("sessionId", sessionId)
                    put("machineId", machineId)
                    put("esBotella", esBotella)
                }
                val body = json.toString().toRequestBody(JSON_MEDIA)
                val request = Request.Builder()
                    .url("${EcoCycleConfig.VISOR_URL}/machine-validate")
                    .post(body)
                    .withApiKey()
                    .build()
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun detectarEnVisor(jpegBytes: ByteArray): Boolean? {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "frame.jpg",
                        jpegBytes.toRequestBody("image/jpeg".toMediaType()))
                    .build()
                val request = Request.Builder()
                    .url("${EcoCycleConfig.VISOR_URL}/detect")
                    .post(requestBody)
                    .withApiKey()
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) null
                else {
                    val json = JSONObject(response.body!!.string())
                    json.optBoolean("botella", false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun limpiarSesionMaquina(machineId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${EcoCycleConfig.VISOR_URL}/machine-cleanup/$machineId")
                    .post("{}".toRequestBody(JSON_MEDIA))
                    .withApiKey()
                    .build()
                client.newCall(request).execute().isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun registrarSesion(
        usuarioId: String,
        maquinaId: String,
        botellas: Int
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("usuarioId", usuarioId)
                    put("maquinaId", maquinaId)
                    put("botellas", botellas)
                }
                val body = json.toString().toRequestBody(JSON_MEDIA)
                val request = Request.Builder()
                    .url("${EcoCycleConfig.NET_API_URL}/sesionreciclaje")
                    .post(body)
                    .withApiKey()
                    .build()
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
