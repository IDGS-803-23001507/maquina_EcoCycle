// Auto-generado desde ecocycle.env. NO EDITAR MANUALMENTE.
// Regenerar con: python3 scripts/generate_configs.py

package com.example.maquina_recicladora

object EcoCycleConfig {
    const val MACHINE_ID = "machine_001"
    const val SERVER_HOST = "192.168.100.19"
    const val NET_API_PORT = 5000
    val BASE_URL: String get() = "http://${SERVER_HOST}:${NET_API_PORT}/"
}
