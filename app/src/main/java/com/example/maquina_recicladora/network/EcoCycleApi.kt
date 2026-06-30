package com.example.maquina_recicladora.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class FinalizarSesionRequest(
    val usuarioId: String,
    val maquinaId: String,
    val botellas: Int
)

data class ApiResponse(
    val suceso: Boolean,
    val message: String?,
    val data: Map<String, Any>?
)

interface EcoCycleApi {

    @POST("api/sesionreciclaje")
    suspend fun finalizarSesion(@Body request: FinalizarSesionRequest): Response<ApiResponse>
}
