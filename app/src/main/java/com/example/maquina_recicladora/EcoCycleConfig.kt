// Auto-generado desde ecocycle.env. NO EDITAR MANUALMENTE.
// Regenerar con: python3 scripts/generate_configs.py

package com.example.maquina_recicladora

object EcoCycleConfig {
    const val MACHINE_ID = "machine_001"
    const val SERVER_HOST = "104.248.187.43"
    const val VISOR_PORT = 3000
    const val NET_API_PORT = 5000
    val VISOR_URL: String get() = "http://${SERVER_HOST}:${VISOR_PORT}"
    val NET_API_URL: String get() = "http://${SERVER_HOST}:${NET_API_PORT}/api"
}
