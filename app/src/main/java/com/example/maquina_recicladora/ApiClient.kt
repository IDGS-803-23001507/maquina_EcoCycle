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

    suspend fun esperarConfirmacionEsp32(sessionId: String, timeoutMs: Long = 30000): Boolean? {
        return withContext(Dispatchers.IO) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                try {
                    val request = Request.Builder()
                        .url("${EcoCycleConfig.VISOR_URL}/session-status/$sessionId")
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body!!.string())
                        if (json.has("validacion2")) {
                            val v2 = json.getJSONObject("validacion2")
                            return@withContext v2.optBoolean("esBotella", false)
                        }
                    }
                } catch (_: Exception) { }
                Thread.sleep(1000)
            }
            null
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
