// Auto-generado desde ecocycle.env. NO EDITAR MANUALMENTE.
// Regenerar con: python3 scripts/generate_configs.py

package com.example.maquina_recicladora

object EcoCycleConfig {
    const val MACHINE_ID = "machine_001"
    const val MACHINE_API_KEY = "637b6f2c15a1fb2a53d936cb4cdbd34e11d63905e55e9b96"
    const val SERVER_HOST = "104.248.187.43"
    const val SERVER_DOMAIN = "ecocyclemx.tech"
    const val VISOR_PORT = 3000
    const val NET_API_PORT = 5000
    const val VISOR_URL_HTTPS = "https://ecocyclemx.tech/visor"
    const val NET_API_URL_HTTPS = "https://ecocyclemx.tech/api"
    val VISOR_URL: String get() = "https://${SERVER_DOMAIN}/visor"
    val NET_API_URL: String get() = "https://${SERVER_DOMAIN}/api"
}
